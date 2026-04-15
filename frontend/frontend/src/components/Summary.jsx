import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { Link, useOutletContext } from 'react-router-dom';
import { getCurrentUser } from '../services/auth';
import { canManageCurrentTeam } from '../services/permissions';
import { fetchCoverageSummary, fetchServiceNowHealth, fetchServiceNowValidation } from '../services/servicenow';
import { buildApiUrl } from '../services/api';
import './Summary.css';

export default function Summary() {
  const outletContext = useOutletContext() || {};
  const currentUser = outletContext.currentUser || getCurrentUser();
  const { setupStatus } = outletContext;
  const [health, setHealth] = useState(null);
  const [healthLoading, setHealthLoading] = useState(false);
  const [healthError, setHealthError] = useState('');
  const [validation, setValidation] = useState(null);
  const [validationLoading, setValidationLoading] = useState(false);
  const [validationError, setValidationError] = useState('');
  const [recentLogs, setRecentLogs] = useState([]);
  const [logsLoading, setLogsLoading] = useState(false);
  const [logsError, setLogsError] = useState('');
  const [coverage, setCoverage] = useState(null);
  const [coverageLoading, setCoverageLoading] = useState(false);
  const [coverageError, setCoverageError] = useState('');
  const canManageTeam = canManageCurrentTeam(currentUser);

  useEffect(() => {
    async function loadHealth() {
      if (!canManageTeam) {
        return;
      }
      setHealthLoading(true);
      setHealthError('');
      try {
        const data = await fetchServiceNowHealth();
        setHealth(data);
      } catch (err) {
        setHealthError('Failed to run ServiceNow health check.');
      } finally {
        setHealthLoading(false);
      }
    }

    loadHealth();
  }, [canManageTeam]);

  useEffect(() => {
    async function loadValidation() {
      if (!canManageTeam) {
        return;
      }
      setValidationLoading(true);
      setValidationError('');
      try {
        const data = await fetchServiceNowValidation();
        setValidation(data);
      } catch (err) {
        setValidationError('Failed to validate stored ServiceNow sys IDs.');
      } finally {
        setValidationLoading(false);
      }
    }

    loadValidation();
  }, [canManageTeam]);

  useEffect(() => {
    async function loadRecentLogs() {
      if (!canManageTeam) {
        return;
      }
      setLogsLoading(true);
      setLogsError('');
      try {
        const token = sessionStorage.getItem('token');
        if (!token) {
          setLogsError('No token found; please sign in again.');
          return;
        }
        const response = await axios.get(buildApiUrl('/logs/servicenow'), {
          headers: { Authorization: `Bearer ${token}` },
        });
        setRecentLogs(response.data || []);
      } catch (err) {
        setLogsError('Failed to load recent ServiceNow activity.');
      } finally {
        setLogsLoading(false);
      }
    }

    loadRecentLogs();
  }, [canManageTeam]);

  useEffect(() => {
    async function loadCoverage() {
      if (!canManageTeam) {
        return;
      }
      setCoverageLoading(true);
      setCoverageError('');
      try {
        const data = await fetchCoverageSummary(7);
        setCoverage(data);
      } catch (err) {
        setCoverageError('Failed to load coverage summary.');
      } finally {
        setCoverageLoading(false);
      }
    }

    loadCoverage();
  }, [canManageTeam]);

  if (!canManageTeam) {
    return (
      <div className="alert alert-info">
        Summary is available for team managers and admins.
      </div>
    );
  }

  const latestPollLog = recentLogs.find((log) => log.type === 'POLL');
  const latestAssignmentResults = latestPollLog?.assignmentResults || [];
  const latestSuccessCount = latestAssignmentResults.filter((result) => result.status === 'SUCCESS').length;
  const latestFailedCount = latestAssignmentResults.filter((result) => result.status === 'FAILED').length;
  const latestSkippedCount = latestAssignmentResults.filter((result) => result.status === 'SKIPPED').length;
  const latestTopIssue = validation?.issues?.[0];
  const teamName = currentUser?.workspace?.teamName || 'Current Team';
  const organizationName = currentUser?.workspace?.organizationName || 'Your Organization';
  const coverageActionForIssue = (issue) => {
    if (!issue) {
      return { to: '/schedules', label: 'Review Coverage' };
    }
    if (issue.type === 'GEO_SHIFT_GAP') {
      return { to: '/schedules', label: 'Open Schedules' };
    }
    if (issue.type === 'CI_SCHEDULE_RISK') {
      return { to: '/ci-user-mappings', label: 'Review CI Mapping' };
    }
    return { to: '/setup', label: 'Review Setup' };
  };

  return (
    <div className="summary-page px-3 py-3">
      <div className="summary-hero mb-4">
        <div className="summary-hero__eyebrow">Operations Center</div>
        <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
          <div>
            <h2 className="mb-1">Summary for {teamName}</h2>
            <div className="text-muted">
              Operational health, setup readiness, and ServiceNow diagnostics for {teamName} in {organizationName}.
            </div>
          </div>
          <div className="summary-hero__actions d-flex gap-2 flex-wrap">
            <Link className="btn btn-outline-primary" to="/logs">
              View Logs
            </Link>
            <Link className="btn btn-primary" to="/assignment-diagnostics">
              Open Diagnostics
            </Link>
          </div>
        </div>
      </div>

      <div className="summary-metrics row g-3 mb-4">
        <div className="col-lg-4">
          <div className={`card h-100 summary-kpi ${latestPollLog?.status === 'ERROR' ? 'summary-kpi--danger' : 'summary-kpi--success'}`}>
            <div className="card-body">
              <div className="summary-kpi__label">Latest Poll</div>
              <div className="summary-kpi__value">
                {latestPollLog?.status === 'ERROR' ? 'Needs attention' : latestPollLog ? 'Healthy' : 'Waiting'}
              </div>
              <div className="text-muted">
                {latestPollLog?.message || 'No poll activity recorded yet for this team.'}
              </div>
            </div>
          </div>
        </div>
        <div className="col-lg-4">
          <div className={`card h-100 summary-kpi ${latestFailedCount > 0 ? 'summary-kpi--danger' : latestSuccessCount > 0 ? 'summary-kpi--success' : 'summary-kpi--warning'}`}>
            <div className="card-body">
              <div className="summary-kpi__label">Assignment Outcome</div>
              <div className="summary-kpi__value">
                {latestSuccessCount} / {latestFailedCount} / {latestSkippedCount}
              </div>
              <div className="text-muted">Success / Failed / Skipped from the latest poll cycle.</div>
            </div>
          </div>
        </div>
        <div className="col-lg-4">
          <div className={`card h-100 summary-kpi ${validation?.valid ? 'summary-kpi--success' : 'summary-kpi--warning'}`}>
            <div className="card-body">
              <div className="summary-kpi__label">Validation Risk</div>
              <div className="summary-kpi__value">
                {validation?.valid ? 'Clear' : `${validation?.issues?.length || 0} Issues`}
              </div>
              <div className="text-muted">
                {validation?.valid ? 'No known ServiceNow sys ID mismatches.' : 'Stored CI or user IDs need review.'}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="row g-3 mb-4">
        <div className="col-lg-4">
          <div className={`card h-100 summary-card ${latestPollLog?.status === 'ERROR' ? 'border-danger' : 'border-success'}`}>
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-start gap-3">
                <div>
                  <div className="summary-card__label">Latest Poll</div>
                  <h5 className="mb-1">Current Poll Status</h5>
                  {logsError && <div className="text-danger small">{logsError}</div>}
                  {!logsError && latestPollLog && (
                    <>
                      <div className={latestPollLog.status === 'ERROR' ? 'text-danger' : 'text-success'}>
                        {latestPollLog.status === 'ERROR' ? 'Poll failed' : 'Poll completed'}
                      </div>
                      <div className="text-muted small">
                        {latestPollLog.timestamp ? new Date(latestPollLog.timestamp).toLocaleString() : '-'}
                      </div>
                      <div className="text-muted small mt-2">
                        {latestPollLog.message || 'No message available.'}
                      </div>
                    </>
                  )}
                  {!logsError && !latestPollLog && !logsLoading && (
                    <div className="text-muted small">No poll activity recorded yet.</div>
                  )}
                </div>
                <Link className="btn btn-sm btn-outline-primary" to="/logs">
                  Open Logs
                </Link>
              </div>
            </div>
          </div>
        </div>
        <div className="col-lg-4">
          <div className={`card h-100 summary-card ${latestFailedCount > 0 ? 'border-danger' : latestSuccessCount > 0 ? 'border-success' : 'border-warning'}`}>
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-start gap-3">
                <div>
                  <div className="summary-card__label">Assignment Outcome</div>
                  <h5 className="mb-1">Latest Decision</h5>
                  {latestPollLog ? (
                    <>
                      <div className="text-muted small">
                        Success {latestSuccessCount} | Failed {latestFailedCount} | Skipped {latestSkippedCount}
                      </div>
                      <div className="text-muted small mt-2">
                        {latestPollLog.assignmentConfirmation || 'No assignment results recorded yet.'}
                      </div>
                      {latestAssignmentResults[0] && (
                        <div className="small mt-2">
                          <strong>Most recent result:</strong> {latestAssignmentResults[0].incidentNumber || '-'} - {latestAssignmentResults[0].message || '-'}
                        </div>
                      )}
                    </>
                  ) : (
                    <div className="text-muted small">No assignment activity recorded yet.</div>
                  )}
                </div>
                <Link className="btn btn-sm btn-outline-primary" to="/assignment-diagnostics">
                  Run Dry Run
                </Link>
              </div>
            </div>
          </div>
        </div>
        <div className="col-lg-4">
          <div className={`card h-100 summary-card ${validation?.valid ? 'border-success' : 'border-warning'}`}>
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-start gap-3">
                <div>
                  <div className="summary-card__label">Current Validation Risk</div>
                  <h5 className="mb-1">ServiceNow Record Health</h5>
                  {validationError && <div className="text-danger small">{validationError}</div>}
                  {!validationError && validation && (
                    <>
                      <div className={validation.valid ? 'text-success' : 'text-warning'}>
                        {validation.valid ? 'No known sys ID issues' : `${validation.issues?.length || 0} issues need attention`}
                      </div>
                      <div className="text-muted small mt-2">
                        {validation.checkedAt ? `Checked ${new Date(validation.checkedAt).toLocaleString()}` : 'Not checked yet'}
                      </div>
                      {latestTopIssue && (
                        <div className="small mt-2">
                          <strong>Top issue:</strong> {latestTopIssue.localName} ({latestTopIssue.localSysId})
                        </div>
                      )}
                    </>
                  )}
                  {!validationError && !validation && (
                    <div className="text-muted small">Validation has not been run yet.</div>
                  )}
                </div>
                <Link className="btn btn-sm btn-outline-primary" to="/setup">
                  Review Setup
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="row g-4">
        <div className="col-12 col-xxl-7">
          <div className="card summary-detail-card border-primary mb-4">
            <div className="card-body d-flex justify-content-between align-items-start gap-3 flex-wrap">
              <div>
                <div className="summary-card__label">ServiceNow Health</div>
                <h5 className="mb-1">Connection Status</h5>
                {healthError && <div className="text-danger">{healthError}</div>}
                {!healthError && health && (
                  <>
                    <div className={`mb-2 ${health.healthy ? 'text-success' : 'text-danger'}`}>
                      {health.healthy ? 'Connected' : 'Connection issue'}: {health.message}
                    </div>
                    <div className="text-muted small">
                      Instance: {health.instanceUrl}
                    </div>
                    <div className="text-muted small">
                      Last checked: {health.checkedAt ? new Date(health.checkedAt).toLocaleString() : '-'}
                    </div>
                    <div className="text-muted small">
                      Last poll: {health.lastPollAt ? `${new Date(health.lastPollAt).toLocaleString()} (${health.lastPollStatus || 'Unknown'})` : 'No poll yet'}
                    </div>
                  </>
                )}
              </div>
              <div className="d-flex gap-2 flex-wrap">
                <button
                  type="button"
                  className="btn btn-outline-primary"
                  disabled={healthLoading}
                  onClick={async () => {
                    setHealthLoading(true);
                    setHealthError('');
                    try {
                      const data = await fetchServiceNowHealth();
                      setHealth(data);
                    } catch (err) {
                      setHealthError('Failed to run ServiceNow health check.');
                    } finally {
                      setHealthLoading(false);
                    }
                  }}
                >
                  {healthLoading ? 'Checking...' : 'Run Connection Test'}
                </button>
                <Link className="btn btn-primary" to="/assignment-diagnostics">
                  Open Diagnostics
                </Link>
              </div>
            </div>
          </div>

          <div className={`card summary-detail-card mb-4 ${coverage?.gapCount || coverage?.ciRiskCount ? 'border-warning' : 'border-success'}`}>
            <div className="card-body d-flex justify-content-between align-items-start gap-3 flex-wrap">
              <div>
                <div className="summary-card__label">Schedule Coverage Outlook</div>
                <h5 className="mb-1">Next 7 Days</h5>
                {coverageError && <div className="text-danger">{coverageError}</div>}
                {!coverageError && coverage && (
                  <>
                    <div className={`mb-2 ${(coverage.gapCount || coverage.ciRiskCount) ? 'text-warning' : 'text-success'}`}>
                      {(coverage.gapCount || coverage.ciRiskCount)
                        ? `${coverage.gapCount} geo/shift gaps and ${coverage.ciRiskCount} CI schedule risks found in the next 7 days.`
                        : 'No coverage gaps detected in the next 7 days.'}
                    </div>
                    <div className="text-muted small">
                      Window: {coverage.startDate} to {coverage.endDate}
                    </div>
                    <div className="text-muted small">
                      Covered geo/shift days: {coverage.coveredGeoShiftDays} of {coverage.totalGeoShiftDays}
                    </div>
                    <div className="text-muted small">
                      Last checked: {coverage.checkedAt ? new Date(coverage.checkedAt).toLocaleString() : '-'}
                    </div>
                    {coverage.issues?.length > 0 && (
                      <div className="summary-issue-list mt-3">
                        {coverage.issues.slice(0, 5).map((issue, index) => (
                          <div
                            className="summary-issue-item"
                            key={`${issue.type}-${issue.date || issue.configurationItem}-${index}`}
                          >
                            <div className="small text-muted">
                              {issue.message}
                            </div>
                            <Link
                              className="btn btn-sm btn-outline-secondary"
                              to={coverageActionForIssue(issue).to}
                            >
                              {coverageActionForIssue(issue).label}
                            </Link>
                          </div>
                        ))}
                        {coverage.issues.length > 5 && (
                          <div className="small text-muted">+{coverage.issues.length - 5} more issues</div>
                        )}
                      </div>
                    )}
                  </>
                )}
              </div>
              <div className="d-flex gap-2 flex-wrap">
                <button
                  type="button"
                  className="btn btn-outline-primary"
                  disabled={coverageLoading}
                  onClick={async () => {
                    setCoverageLoading(true);
                    setCoverageError('');
                    try {
                      const data = await fetchCoverageSummary(7);
                      setCoverage(data);
                    } catch (err) {
                      setCoverageError('Failed to load coverage summary.');
                    } finally {
                      setCoverageLoading(false);
                    }
                  }}
                >
                  {coverageLoading ? 'Checking...' : 'Refresh Coverage'}
                </button>
                <Link className="btn btn-primary" to={coverageActionForIssue(coverage?.issues?.[0]).to}>
                  {coverageActionForIssue(coverage?.issues?.[0]).label}
                </Link>
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-xxl-5">
          <div className={`card summary-detail-card mb-4 ${validation?.valid ? 'border-success' : 'border-warning'}`}>
            <div className="card-body d-flex justify-content-between align-items-start gap-3 flex-wrap">
              <div>
                <div className="summary-card__label">ServiceNow Record Validation</div>
                <h5 className="mb-1">Stored Sys ID Audit</h5>
                {validationError && <div className="text-danger">{validationError}</div>}
                {!validationError && validation && (
                  <>
                    <div className={`mb-2 ${validation.valid ? 'text-success' : 'text-warning'}`}>
                      {validation.message}
                    </div>
                    <div className="text-muted small">
                      Configuration Items: {validation.validConfigurationItemCount} of {validation.configurationItemCount} valid
                    </div>
                    <div className="text-muted small">
                      Team Members: {validation.validTeamMemberCount} of {validation.teamMemberCount} valid
                    </div>
                    <div className="text-muted small">
                      Last checked: {validation.checkedAt ? new Date(validation.checkedAt).toLocaleString() : '-'}
                    </div>
                    {!validation.valid && validation.issues?.length > 0 && (
                      <div className="summary-issue-list mt-3">
                        {validation.issues.slice(0, 5).map((issue, index) => (
                          <div className="summary-issue-item summary-issue-item--stacked" key={`${issue.type}-${issue.localSysId}-${index}`}>
                            <div className="small text-muted">
                              {issue.type === 'CONFIGURATION_ITEM' ? 'CI' : 'User'} {issue.localName} ({issue.localSysId}): {issue.message}
                            </div>
                          </div>
                        ))}
                        {validation.issues.length > 5 && (
                          <div className="small text-muted">+{validation.issues.length - 5} more issues</div>
                        )}
                      </div>
                    )}
                  </>
                )}
              </div>
              <button
                type="button"
                className="btn btn-outline-primary"
                disabled={validationLoading}
                onClick={async () => {
                  setValidationLoading(true);
                  setValidationError('');
                  try {
                    const data = await fetchServiceNowValidation();
                    setValidation(data);
                  } catch (err) {
                    setValidationError('Failed to validate stored ServiceNow sys IDs.');
                  } finally {
                    setValidationLoading(false);
                  }
                }}
              >
                {validationLoading ? 'Validating...' : 'Validate Records'}
              </button>
            </div>
          </div>

          {setupStatus?.ready && (
            <div className="card border-success summary-ready-card mb-4">
              <div className="card-body d-flex justify-content-between align-items-center gap-3 flex-wrap">
                <div>
                  <div className="summary-card__label">Workspace Status</div>
                  <h5 className="mb-1">Workspace Ready</h5>
                  <div className="text-muted">
                    {teamName} is configured and ready for daily use.
                  </div>
                </div>
                <Link className="btn btn-outline-success" to="/setup">
                  Review {teamName} Setup
                </Link>
              </div>
            </div>
          )}

          {setupStatus && !setupStatus.ready && !setupStatus.brandNew && (
            <div className="alert alert-warning d-flex justify-content-between align-items-center mb-4 flex-wrap gap-3">
              <div>
                {teamName} setup is incomplete. {setupStatus.completedSteps} of {setupStatus.totalSteps} core steps are done.
              </div>
              <Link className="btn btn-sm btn-primary" to="/setup">
                Continue {teamName} Setup
              </Link>
            </div>
          )}

          {setupStatus?.ready && setupStatus.steps?.some((step) => step.key === 'schedules' && !step.complete) && (
            <div className="alert alert-info d-flex justify-content-between align-items-center mb-4 flex-wrap gap-3">
              <div>
                {teamName} setup is complete. Schedules are optional, but adding them will improve incident assignment.
              </div>
              <Link className="btn btn-sm btn-outline-primary" to="/schedules">
                Add Schedules
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
