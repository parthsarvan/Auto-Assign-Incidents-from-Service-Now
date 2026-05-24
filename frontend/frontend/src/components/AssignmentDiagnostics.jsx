import React, { useEffect, useState } from 'react';
import { Link, useOutletContext } from 'react-router-dom';
import { getCurrentUser } from '../services/auth';
import { fetchAssignmentDiagnostics } from '../services/servicenow';
import CurrentRoutingWindow from './CurrentRoutingWindow';
import './AssignmentDiagnostics.css';

export default function AssignmentDiagnostics() {
  const outletContext = useOutletContext() || {};
  const currentUser = outletContext.currentUser || getCurrentUser();
  const { setupStatus } = outletContext;
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const teamName = currentUser?.workspace?.teamName || 'Current Team';
  const organizationName = currentUser?.workspace?.organizationName || 'Your Organization';

  const loadDiagnostics = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetchAssignmentDiagnostics();
      setData(response);
    } catch (err) {
      setError('Failed to run assignment diagnostics.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDiagnostics();
  }, []);

  return (
    <div className="container py-3 diagnostics-page">
      <div className="diagnostics-hero mb-4">
        <div className="diagnostics-hero__eyebrow">Dry Run Analysis</div>
        <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
          <div>
            <h2 className="mb-1">Assignment Diagnostics for {teamName}</h2>
            <div className="text-muted small">
              Dry-run assignment analysis for unassigned incidents affecting {teamName} in {organizationName}.
            </div>
          </div>
          <button className="btn btn-outline-primary" onClick={loadDiagnostics} disabled={loading}>
            {loading ? 'Running...' : 'Run Dry Run'}
          </button>
        </div>
      </div>

      <CurrentRoutingWindow />

      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h5 className="mb-1">Dry Run Results</h5>
          <small className="text-muted">
            {data?.checkedAt ? `Last checked: ${new Date(data.checkedAt).toLocaleString()}` : 'No diagnostics run yet'}
          </small>
        </div>
      </div>

      {!error && !loading && data && (
        <div className="row g-3 mb-4">
          <div className="col-md-4">
            <div className="card diagnostics-kpi border-primary">
              <div className="card-body">
                <div className="summary-card__label">Unassigned Incidents</div>
                <div className="summary-kpi__value">{data.incidentCount}</div>
              </div>
            </div>
          </div>
          <div className="col-md-4">
            <div className="card diagnostics-kpi border-success">
              <div className="card-body">
                <div className="summary-card__label">Assignable Now</div>
                <div className="summary-kpi__value text-success">{data.assignableCount}</div>
              </div>
            </div>
          </div>
          <div className="col-md-4">
            <div className="card diagnostics-kpi border-warning">
              <div className="card-body">
                <div className="summary-card__label">Skipped in Dry Run</div>
                <div className="summary-kpi__value text-warning">{data.skippedCount}</div>
              </div>
            </div>
          </div>
        </div>
      )}

      {error && <div className="alert alert-danger">{error}</div>}

      {loading && (
        <div className="text-center my-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      )}

      {!loading && !error && data?.incidents?.length > 0 && (
        <div className="row g-3">
          {data.incidents.map((incident) => (
            <div className="col-12" key={`${incident.incidentSysId}-${incident.incidentNumber}`}>
              <div className={`card diagnostics-incident-card border-${incident.status === 'ASSIGNABLE' ? 'success' : 'warning'}`}>
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap mb-2">
                    <div>
                      <h5 className="mb-1">{incident.incidentNumber || 'Unknown Incident'}</h5>
                      <div className="text-muted small">
                        CI: {incident.configurationItem || '-'} | Priority: {incident.priority || '-'}
                      </div>
                      <div className="text-muted small">
                        Caller: {incident.caller || '-'} | Created: {incident.createdOn ? new Date(incident.createdOn).toLocaleString() : '-'}
                      </div>
                    </div>
                    <span className={`badge ${incident.status === 'ASSIGNABLE' ? 'bg-success' : 'bg-warning text-dark'}`}>
                      {incident.status}
                    </span>
                  </div>
                  <div className="mb-2">
                    <strong>Reason:</strong> {incident.reason || '-'}
                  </div>
                  {incident.shortDescription && (
                    <div className="mb-2">
                      <strong>Description:</strong> {incident.shortDescription}
                    </div>
                  )}
                  {incident.suggestion && (
                    <div className="border rounded p-3 bg-light diagnostics-suggestion-card">
                      <div><strong>Suggested Assignee:</strong> {incident.suggestion.assigneeName || '-'}</div>
                      <div><strong>Email:</strong> {incident.suggestion.assigneeEmail || '-'}</div>
                      <div><strong>ServiceNow Link:</strong> {incident.suggestion.assigneeSysId ? 'Linked' : 'Not linked'}</div>
                      <div><strong>Geo:</strong> {incident.suggestion.geo || '-'}</div>
                      <div><strong>Shift:</strong> {incident.suggestion.shift || '-'}</div>
                    </div>
                  )}
                  {incident.candidateChecks?.length > 0 && (
                    <div className="mt-3">
                      <div className="fw-semibold mb-2">Mapped Candidate Review</div>
                      <div className="table-responsive">
                        <table className="table table-sm align-middle mb-0">
                          <thead>
                            <tr>
                              <th>Team Member</th>
                              <th>Schedule</th>
                              <th>Match</th>
                              <th>Status</th>
                              <th>Reason</th>
                            </tr>
                          </thead>
                          <tbody>
                            {incident.candidateChecks.map((candidate, index) => (
                              <tr key={`${incident.incidentSysId || incident.incidentNumber}-candidate-${index}`}>
                                <td>
                                  <div className="fw-semibold">{candidate.teamMemberName || '-'}</div>
                                  <div className="text-muted small">{candidate.email || '-'}</div>
                                </td>
                                <td>
                                  <div>{candidate.activeSchedules || '-'}</div>
                                  <div className="text-muted small">Geo: {candidate.memberGeo || '-'}</div>
                                </td>
                                <td>{candidate.matchStatus || '-'}</td>
                                <td>
                                  {candidate.selected ? (
                                    <span className="badge bg-success">Selected</span>
                                  ) : candidate.eligible ? (
                                    <span className="badge bg-primary">Eligible</span>
                                  ) : (
                                    <span className="badge bg-secondary">Filtered Out</span>
                                  )}
                                  {candidate.onLeave && <span className="badge bg-warning text-dark ms-1">On Leave</span>}
                                  {candidate.onBreak && <span className="badge bg-warning text-dark ms-1">On Break</span>}
                                </td>
                                <td>{candidate.reason || '-'}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {!loading && !error && data?.incidents?.length === 0 && (
        <div className="alert alert-success d-flex justify-content-between align-items-center gap-3 flex-wrap">
          <div>
            {setupStatus && !setupStatus.ready
              ? `${teamName} setup is still incomplete, so dry-run diagnostics may not have enough team data yet.`
              : `No unassigned incidents are currently available for ${teamName} diagnostics.`}
          </div>
          <Link className="btn btn-sm btn-outline-primary" to={setupStatus && !setupStatus.ready ? '/setup' : '/summary'}>
            {setupStatus && !setupStatus.ready ? 'Continue Setup' : 'Open Summary'}
          </Link>
        </div>
      )}
    </div>
  );
}
