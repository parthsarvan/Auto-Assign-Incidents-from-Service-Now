// src/components/Logs.jsx
import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { Link, useOutletContext } from 'react-router-dom';
import { getCurrentUser } from '../services/auth';
import { buildApiUrl } from '../services/api';
import { pollServiceNowNow } from '../services/servicenow';
import CurrentRoutingWindow from './CurrentRoutingWindow';
import './Logs.css';

const POLL_REFRESH_BUFFER_MS = 303000;

export default function Logs() {
  const outletContext = useOutletContext() || {};
  const currentUser = outletContext.currentUser || getCurrentUser();
  const { setupStatus } = outletContext;
  const [logs, setLogs] = useState([]);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [resultFilter, setResultFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [pollNowLoading, setPollNowLoading] = useState(false);
  const [pollNowMessage, setPollNowMessage] = useState('');
  const [pollNowError, setPollNowError] = useState('');

  const fetchLogs = async () => {
    setLoading(true);
    setError('');
    try {
      const token = sessionStorage.getItem('token');
      if (!token) {
        setError('No token found; please sign in again.');
        setLoading(false);
        return;
      }
      const response = await axios.get(buildApiUrl('/logs/servicenow'), {
        headers: { Authorization: `Bearer ${token}` },
      });
      setLogs(response.data || []);
      setLastUpdated(new Date());
    } catch (err) {
      console.error('Failed to load ServiceNow logs:', err);
      setError('Failed to load ServiceNow logs. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, []);

  const handlePollNow = async () => {
    setPollNowLoading(true);
    setPollNowMessage('');
    setPollNowError('');
    try {
      const response = await pollServiceNowNow();
      setPollNowMessage(response?.message || 'Poll completed.');
      await fetchLogs();
    } catch (err) {
      console.error('Failed to trigger ServiceNow poll:', err);
      setPollNowError('Failed to trigger poll now. Please try again.');
    } finally {
      setPollNowLoading(false);
    }
  };

  useEffect(() => {
    if (!logs.length) {
      return undefined;
    }

    const latestPollTimestamp = logs
      .filter((log) => log.type === 'POLL' && log.timestamp)
      .map((log) => Date.parse(log.timestamp))
      .filter((value) => !Number.isNaN(value))
      .sort((left, right) => right - left)[0];

    if (!latestPollTimestamp) {
      return undefined;
    }

    const nextRefreshDelay = Math.max(
      latestPollTimestamp + POLL_REFRESH_BUFFER_MS - Date.now(),
      3000
    );
    const timeoutId = setTimeout(() => {
      fetchLogs();
    }, nextRefreshDelay);

    return () => clearTimeout(timeoutId);
  }, [logs]);

  const logMatchesFilters = (log) => {
    if (statusFilter !== 'ALL' && log.status !== statusFilter) {
      return false;
    }

    if (resultFilter !== 'ALL') {
      const results = log.assignmentResults || [];
      const hasMatchingResult = results.some((result) => result.status === resultFilter);
      if (!hasMatchingResult) {
        return false;
      }
    }

    const query = searchTerm.trim().toLowerCase();
    if (!query) {
      return true;
    }

    const searchableValues = [
      log.type,
      log.status,
      log.message,
      log.assignmentConfirmation,
      ...(log.incidents || []).flatMap((incident) => [
        incident.number,
        incident.configurationItem,
        incident.assignmentGroup,
        incident.caller,
        incident.shortDescription,
      ]),
      ...(log.assignmentResults || []).flatMap((result) => [
        result.incidentNumber,
        result.assigneeName,
        result.status,
        result.message,
      ]),
      ...(log.assignmentSelections || []).flatMap((selection) => [
        selection.incidentNumber,
        selection.assigneeName,
        selection.assigneeEmail,
        selection.geo,
        selection.shift,
      ]),
    ]
      .filter(Boolean)
      .map((value) => String(value).toLowerCase());

    return searchableValues.some((value) => value.includes(query));
  };

  const filteredLogs = logs.filter(logMatchesFilters);
  const okCount = logs.filter((log) => log.status === 'OK').length;
  const errorCount = logs.filter((log) => log.status === 'ERROR').length;
  const successResultCount = logs.flatMap((log) => log.assignmentResults || []).filter((result) => result.status === 'SUCCESS').length;
  const failedResultCount = logs.flatMap((log) => log.assignmentResults || []).filter((result) => result.status === 'FAILED').length;
  const skippedResultCount = logs.flatMap((log) => log.assignmentResults || []).filter((result) => result.status === 'SKIPPED').length;
  const teamName = currentUser?.workspace?.teamName || 'Current Team';
  const organizationName = currentUser?.workspace?.organizationName || 'Your Organization';

  const classifyMessage = (message = '') => {
    const normalized = message.toLowerCase();
    if (!normalized) {
      return '';
    }
    if (normalized.includes('assignment is disabled')) {
      return 'Assignment disabled';
    }
    if (normalized.includes('no ci-user mapping')) {
      return 'Missing CI-user mapping';
    }
    if (normalized.includes('ci not configured for this team')) {
      return 'CI not in this team';
    }
    if (normalized.includes('no eligible mapped team member')) {
      return 'No eligible scheduled user';
    }
    if (normalized.includes('p0/p1c')) {
      return 'Assignee busy on critical work';
    }
    if (normalized.includes('missing a configuration item')) {
      return 'Missing CI on incident';
    }
    if (normalized.includes('could not initialize proxy') || normalized.includes('no session')) {
      return 'Backend session error';
    }
    if (normalized.includes('assignment failed')) {
      return 'ServiceNow assignment failure';
    }
    return 'Info';
  };

  const actionForResult = (result) => {
    const message = (result?.message || '').toLowerCase();
    if (message.includes('no ci-user mapping')) {
      return { to: '/ci-user-mappings', label: 'Fix CI Mapping' };
    }
    if (message.includes('ci not configured for this team')) {
      return { to: '/configuration-items', label: 'Review Team CIs' };
    }
    if (message.includes('no eligible mapped team member')) {
      return { to: '/schedules', label: 'Fix Schedules' };
    }
    if (message.includes('p0/p1c')) {
      return { to: '/summary', label: 'Review Summary' };
    }
    if (message.includes('missing a configuration item')) {
      return { to: '/configuration-items', label: 'Fix CI Setup' };
    }
    if (message.includes('missing servicenow assignee sys_id')) {
      return { to: '/team-members', label: 'Fix Team Member' };
    }
    if (message.includes('assignment failed')) {
      return { to: '/assignment-diagnostics', label: 'Open Diagnostics' };
    }
    if (message.includes('assignment is disabled')) {
      return { to: '/summary', label: 'Review Summary' };
    }
    return { to: '/assignment-diagnostics', label: 'Investigate' };
  };

  const sortIncidentsByCreatedOn = (incidents = []) =>
    [...incidents].sort((left, right) => {
      const leftTime = left?.createdOn ? Date.parse(left.createdOn) : Number.POSITIVE_INFINITY;
      const rightTime = right?.createdOn ? Date.parse(right.createdOn) : Number.POSITIVE_INFINITY;
      if (leftTime !== rightTime) {
        return leftTime - rightTime;
      }
      return String(left?.number || '').localeCompare(String(right?.number || ''));
    });

  return (
    <div className="logs-page">
      <div className="logs-hero mb-4">
        <div className="logs-hero__eyebrow">Operational Timeline</div>
        <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
          <div>
            <h2 className="mb-1">ServiceNow Logs for {teamName}</h2>
            <div className="text-muted small">
              Poll history and assignment outcomes for {teamName} in {organizationName}.
            </div>
          </div>
          <div className="logs-hero__actions d-flex gap-2 flex-wrap">
            <button className="btn btn-primary" onClick={handlePollNow} disabled={pollNowLoading || loading}>
              {pollNowLoading ? 'Polling...' : 'Poll Now'}
            </button>
            <button className="btn btn-outline-primary" onClick={fetchLogs} disabled={loading || pollNowLoading}>
              {loading ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>
        </div>
      </div>

      <CurrentRoutingWindow />

      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h5 className="mb-1">Recent Activity</h5>
          <small className="text-muted">
            {lastUpdated ? `Last updated: ${lastUpdated.toLocaleString()}` : 'Not updated yet'}
          </small>
        </div>
      </div>

      <div className="row g-3 mb-4">
        <div className="col-md-3">
          <div className="card border-primary logs-kpi">
            <div className="card-body py-3">
              <div className="summary-card__label">Log Entries</div>
              <div className="summary-kpi__value">{logs.length}</div>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-success logs-kpi">
            <div className="card-body py-3">
              <div className="summary-card__label">Healthy Polls</div>
              <div className="summary-kpi__value text-success">{okCount}</div>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-danger logs-kpi">
            <div className="card-body py-3">
              <div className="summary-card__label">Error Polls</div>
              <div className="summary-kpi__value text-danger">{errorCount}</div>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-secondary logs-kpi">
            <div className="card-body py-3">
              <div className="summary-card__label">Assignment Results</div>
              <div className="small">
                <span className="text-success me-2">Success: {successResultCount}</span>
                <span className="text-danger me-2">Failed: {failedResultCount}</span>
                <span className="text-warning">Skipped: {skippedResultCount}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="card mb-3">
        <div className="card-body">
          <div className="row g-3 align-items-end">
            <div className="col-md-4">
              <label className="form-label">Search</label>
              <input
                type="text"
                className="form-control"
                placeholder="Incident, CI, caller, message..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <div className="col-md-3">
              <label className="form-label">Log Status</label>
              <select
                className="form-select"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="ALL">All</option>
                <option value="OK">OK</option>
                <option value="ERROR">ERROR</option>
              </select>
            </div>
            <div className="col-md-3">
              <label className="form-label">Assignment Result</label>
              <select
                className="form-select"
                value={resultFilter}
                onChange={(e) => setResultFilter(e.target.value)}
              >
                <option value="ALL">All</option>
                <option value="SUCCESS">Success</option>
                <option value="FAILED">Failed</option>
                <option value="SKIPPED">Skipped</option>
              </select>
            </div>
            <div className="col-md-2 text-md-end">
              <div className="text-muted small logs-page__results">
                Showing {filteredLogs.length} of {logs.length}
              </div>
            </div>
          </div>
        </div>
      </div>

      {pollNowMessage && <div className="alert alert-success">{pollNowMessage}</div>}
      {pollNowError && <div className="alert alert-danger">{pollNowError}</div>}
      {error && <div className="alert alert-danger">{error}</div>}

      {!error && logs.length === 0 && (
        <div className="alert alert-info d-flex justify-content-between align-items-center gap-3 flex-wrap">
          <div>
            {setupStatus && !setupStatus.ready
              ? `${teamName} does not have enough setup completed for meaningful polling history yet. Finish the core setup steps first.`
              : `No logs are available for ${teamName} yet. This usually means the poller has not picked up any activity for the current team yet.`}
          </div>
          <Link className="btn btn-sm btn-outline-primary" to={setupStatus && !setupStatus.ready ? '/setup' : '/summary'}>
            {setupStatus && !setupStatus.ready ? 'Continue Setup' : 'Open Summary'}
          </Link>
        </div>
      )}

      {!error && logs.length > 0 && filteredLogs.length === 0 && (
        <div className="alert alert-warning">No logs match the current filters.</div>
      )}

      <div className="logs-list">
        {filteredLogs.map((log, index) => (
          <div className="card mb-3" key={`${log.timestamp}-${index}`}>
            <div className="card-body">
              <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
                <div>
                  <h5 className="card-title mb-1">{log.type}</h5>
                  <div className="text-muted">{new Date(log.timestamp).toLocaleString()}</div>
                </div>
                <div className="text-end">
                  <span
                    className={`badge ${
                      log.status === 'OK' ? 'bg-success' : 'bg-danger'
                    }`}
                  >
                    {log.status}
                  </span>
                  <div className="mt-2">
                    <strong>{log.incidentCount}</strong> incidents
                  </div>
                </div>
              </div>

              {log.message && <p className="mt-3 mb-2">{log.message}</p>}
              {log.message && (
                <div className="mb-3">
                  <span className="badge text-bg-light border">{classifyMessage(log.message)}</span>
                </div>
              )}

              {log.assignmentSelections && log.assignmentSelections.length > 0 && (
                <div className="selection-box mt-3">
                  <div className="selection-box__title">Assignment Selections</div>
                  <div className="selection-box__list">
                    {log.assignmentSelections.map((selection, selectionIndex) => (
                      <div
                        className="selection-box__item"
                        key={`${selection.incidentNumber}-${selectionIndex}`}
                      >
                        <div>
                          <strong>Incident:</strong> {selection.incidentNumber || '-'}
                        </div>
                        <div>
                          <strong>Team Member:</strong> {selection.assigneeName || '-'}
                          {selection.assigneeEmail ? ` (${selection.assigneeEmail})` : ''}
                        </div>
                        <div className="selection-box__meta">
                          <span>
                            <strong>Geo:</strong> {selection.geo || '-'}
                          </span>
                          <span>
                            <strong>Shift:</strong> {selection.shift || '-'}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {log.assignmentConfirmation && (
                <div className="selection-box mt-3">
                  <div className="selection-box__title">Confirmation</div>
                  <div className="selection-box__item">
                    {log.assignmentConfirmation}
                  </div>
                  {log.assignmentResults && log.assignmentResults.length > 0 && (
                    <div className="selection-box__list">
                      {log.assignmentResults.map((result, resultIndex) => (
                        <div
                          className="selection-box__item"
                          key={`${result.incidentNumber}-${resultIndex}`}
                        >
                          <div>
                            <strong>Incident:</strong> {result.incidentNumber || '-'}
                          </div>
                          <div>
                            <strong>Team Member:</strong> {result.assigneeName || '-'}
                          </div>
                          <div className="selection-box__meta">
                            <span>
                              <strong>Status:</strong>{' '}
                              <span className={`badge ${result.status === 'SUCCESS' ? 'bg-success' : result.status === 'FAILED' ? 'bg-danger' : 'bg-warning text-dark'}`}>
                                {result.status || '-'}
                              </span>
                            </span>
                            <span>
                              <strong>Message:</strong> {result.message || '-'}
                            </span>
                          </div>
                          {result.status !== 'SUCCESS' && (
                            <div className="mt-2">
                              <Link className="btn btn-sm btn-outline-primary" to={actionForResult(result).to}>
                                {actionForResult(result).label}
                              </Link>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {log.incidents && log.incidents.length > 0 && (
                <div className="table-responsive">
                  <table className="table table-sm table-striped align-middle">
                    <thead>
                      <tr>
                        <th>Incident</th>
                        <th>Created (Local)</th>
                        <th>Configuration Item</th>
                        <th>Assignment Group</th>
                        <th>Priority</th>
                        <th>Caller</th>
                        <th>Description</th>
                      </tr>
                    </thead>
                    <tbody>
                      {sortIncidentsByCreatedOn(log.incidents).map((incident) => (
                        <tr key={`${incident.number}-${incident.createdOn}`}>
                          <td>{incident.number}</td>
                          <td>
                            {incident.createdOn
                              ? new Date(incident.createdOn).toLocaleString()
                              : '-'}
                          </td>
                          <td>{incident.configurationItem || '-'}</td>
                          <td>{incident.assignmentGroup || '-'}</td>
                          <td>{incident.priority || '-'}</td>
                          <td>{incident.caller || '-'}</td>
                          <td>{incident.shortDescription}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
