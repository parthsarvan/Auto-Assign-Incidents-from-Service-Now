// src/components/Logs.jsx
import React, { useEffect, useState } from 'react';
import axios from 'axios';
import './Logs.css';

export default function Logs() {
  const [logs, setLogs] = useState([]);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

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
      const response = await axios.get('http://localhost:8080/api/logs/servicenow', {
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
    const intervalId = setInterval(fetchLogs, 300000);
    return () => clearInterval(intervalId);
  }, []);

  return (
    <div className="logs-page">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h4 className="mb-1">ServiceNow Logs</h4>
          <small className="text-muted">
            {lastUpdated ? `Last updated: ${lastUpdated.toLocaleString()}` : 'Not updated yet'}
          </small>
        </div>
        <button className="btn btn-outline-primary" onClick={fetchLogs} disabled={loading}>
          {loading ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {!error && logs.length === 0 && (
        <div className="alert alert-info">No logs available yet.</div>
      )}

      <div className="logs-list">
        {logs.map((log, index) => (
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
                              <strong>Status:</strong> {result.status || '-'}
                            </span>
                            <span>
                              <strong>Message:</strong> {result.message || '-'}
                            </span>
                          </div>
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
                        <th>Priority</th>
                        <th>Caller</th>
                        <th>Description</th>
                      </tr>
                    </thead>
                    <tbody>
                      {log.incidents.map((incident) => (
                        <tr key={`${incident.number}-${incident.createdOn}`}>
                          <td>{incident.number}</td>
                          <td>
                            {incident.createdOn
                              ? new Date(incident.createdOn).toLocaleString()
                              : '-'}
                          </td>
                          <td>{incident.configurationItem || '-'}</td>
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
