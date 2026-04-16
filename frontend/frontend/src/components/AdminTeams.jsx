import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createWorkspaceTeam, fetchWorkspaceTeams, regenerateWorkspaceInvite, switchWorkspaceTeam } from '../services/workspace';
import { getCurrentUser } from '../services/auth';
import './AdminTeams.css';

export default function AdminTeams() {
  const navigate = useNavigate();
  const [teams, setTeams] = useState([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [copyMode, setCopyMode] = useState('scratch');
  const [copyFromTeamId, setCopyFromTeamId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [inviteMessage, setInviteMessage] = useState('');
  const [inviteLoadingTeamId, setInviteLoadingTeamId] = useState(null);
  const currentUser = getCurrentUser();
  const isAdmin = currentUser?.role === 'Admin';
  const organizationName = currentUser?.workspace?.organizationName || 'Your Organization';

  const loadTeams = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await fetchWorkspaceTeams();
      setTeams(data || []);
    } catch (err) {
      setError('Failed to load teams.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTeams();
  }, []);

  const handleCreate = async (event) => {
    event.preventDefault();
    if (!isAdmin) {
      return;
    }
    setError('');
    setInviteMessage('');
    try {
      await createWorkspaceTeam(
        name,
        description,
        copyMode === 'copy' && copyFromTeamId ? Number(copyFromTeamId) : null
      );
      setName('');
      setDescription('');
      setCopyMode('scratch');
      setCopyFromTeamId('');
      await loadTeams();
    } catch (err) {
      setError(typeof err?.response?.data === 'string' ? err.response.data : 'Failed to create team.');
    }
  };

  const handleSwitch = async (teamId) => {
    setError('');
    try {
      await switchWorkspaceTeam(teamId);
      await loadTeams();
      navigate('/summary', { replace: true });
    } catch (err) {
      setError(typeof err?.response?.data === 'string' ? err.response.data : 'Failed to switch team.');
    }
  };

  const handleCopyInviteCode = async (team) => {
    try {
      const code = team.joinCode || '';
      if (!code) {
        throw new Error('Missing invite code');
      }

      if (navigator.clipboard?.writeText && window.isSecureContext) {
        await navigator.clipboard.writeText(code);
      } else {
        const helperInput = document.createElement('textarea');
        helperInput.value = code;
        helperInput.setAttribute('readonly', '');
        helperInput.style.position = 'fixed';
        helperInput.style.opacity = '0';
        document.body.appendChild(helperInput);
        helperInput.focus();
        helperInput.select();
        const copied = document.execCommand('copy');
        document.body.removeChild(helperInput);
        if (!copied) {
          throw new Error('Copy command failed');
        }
      }
      setInviteMessage(`Copied invite code for ${team.teamName}.`);
      window.setTimeout(() => setInviteMessage(''), 2500);
    } catch (err) {
      setError('Failed to copy invite code.');
    }
  };

  const handleRegenerateInviteCode = async (team) => {
    if (!window.confirm(`Regenerate the invite code for ${team.teamName}? Existing shared codes will stop working.`)) {
      return;
    }
    setError('');
    setInviteLoadingTeamId(team.teamId);
    try {
      const updatedTeam = await regenerateWorkspaceInvite(team.teamId);
      setTeams((previous) => previous.map((item) => (
        item.teamId === updatedTeam.teamId ? updatedTeam : item
      )));
      setInviteMessage(`Generated a new invite code for ${updatedTeam.teamName}.`);
      window.setTimeout(() => setInviteMessage(''), 2500);
    } catch (err) {
      setError(typeof err?.response?.data === 'string' ? err.response.data : 'Failed to regenerate invite code.');
    } finally {
      setInviteLoadingTeamId(null);
    }
  };

  return (
    <div className="container admin-teams-page">
      <div className="admin-page-hero mb-4">
        <div className="admin-page-hero__eyebrow">Organization Structure</div>
        <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
          <div>
            <h2 className="mb-1">Manage Teams</h2>
            <div className="text-muted">
              Teams in {organizationName}. Switch the active team to change which workspace the rest of InciTeam is showing.
            </div>
          </div>
        </div>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}
      {inviteMessage && <div className="alert alert-success">{inviteMessage}</div>}

      {isAdmin ? (
        <>
          <div className="card border-primary p-3 mb-4 admin-page-card">
            <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
              <div>
                <h5 className="mb-1">Invite Your Team</h5>
                <div className="text-muted">
                  Share a team invite code so new users can sign up directly into the right organization and team.
                </div>
              </div>
              <div className="text-muted small">
                Invite codes are team-specific and can be reused by multiple teammates.
              </div>
            </div>
          </div>

          <div className="card p-3 mb-4 admin-page-card">
          <form className="row g-3" onSubmit={handleCreate}>
            <div className="col-md-4">
              <label className="form-label">Team Name</label>
              <input
                type="text"
                className="form-control"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
            <div className="col-md-6">
              <label className="form-label">Description</label>
              <input
                type="text"
                className="form-control"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Optional"
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Setup Mode</label>
              <select
                className="form-select"
                value={copyMode}
                onChange={(e) => setCopyMode(e.target.value)}
              >
                <option value="scratch">Start from scratch</option>
                <option value="copy">Copy setup from another team</option>
              </select>
            </div>
            {copyMode === 'copy' && (
              <div className="col-md-4">
                <label className="form-label">Copy From</label>
                <select
                  className="form-select"
                  value={copyFromTeamId}
                  onChange={(e) => setCopyFromTeamId(e.target.value)}
                  required
                >
                  <option value="">Select source team...</option>
                  {teams.map((team) => (
                    <option key={team.teamId} value={team.teamId}>
                      {team.teamName}
                    </option>
                  ))}
                </select>
                <div className="form-text">
                  Copies geos, shifts, CIs, team members, and mappings. Schedules, leaves, breaks, and logs stay empty.
                </div>
              </div>
            )}
            <div className="col-md-2 d-flex align-items-end">
              <button type="submit" className="btn btn-primary w-100">
                Create Team
              </button>
            </div>
          </form>
          </div>
        </>
      ) : (
        <div className="alert alert-info">Read-only access. Contact an admin to create teams.</div>
      )}

      <div className="card admin-page-card">
        <div className="card-body">
          <div className="table-responsive">
        <table className="table table-bordered admin-page-table">
          <thead className="table-light">
            <tr>
              <th>Team</th>
              <th>Description</th>
              <th>Invite Code</th>
              <th>Status</th>
              <th style={{ width: '280px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {teams.map((team) => (
              <tr key={team.teamId}>
                <td>{team.teamName}</td>
                <td>{team.description || '-'}</td>
                <td>
                  <div className="d-flex align-items-center gap-2 flex-wrap">
                    <code>{team.joinCode || '-'}</code>
                    <button
                      type="button"
                      className="btn btn-outline-secondary btn-sm"
                      onClick={() => handleCopyInviteCode(team)}
                      disabled={!team.joinCode}
                    >
                      Copy
                    </button>
                  </div>
                </td>
                <td>
                  {team.current ? (
                    <span className="badge bg-success">Current Team</span>
                  ) : (
                    <span className="badge bg-secondary">Available</span>
                  )}
                </td>
                <td>
                  <div className="d-flex gap-2 flex-wrap">
                    <button
                      className="btn btn-outline-primary btn-sm"
                      disabled={team.current || loading}
                      onClick={() => handleSwitch(team.teamId)}
                    >
                      Switch
                    </button>
                    {isAdmin && (
                      <button
                        type="button"
                        className="btn btn-outline-warning btn-sm"
                        disabled={inviteLoadingTeamId === team.teamId}
                        onClick={() => handleRegenerateInviteCode(team)}
                      >
                        {inviteLoadingTeamId === team.teamId ? 'Regenerating...' : 'Regenerate Code'}
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
            {teams.length === 0 && (
              <tr>
                <td colSpan={5} className="text-center">No teams found.</td>
              </tr>
            )}
          </tbody>
        </table>
          </div>
        </div>
      </div>
    </div>
  );
}
