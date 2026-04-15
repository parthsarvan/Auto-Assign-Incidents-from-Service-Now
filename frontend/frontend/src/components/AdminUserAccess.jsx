import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { assignUserToTeam, fetchUsers, removeUserFromTeam, updateUserRole, updateUserTeamRole } from '../services/admin';
import { getCurrentUser, setCurrentUser } from '../services/auth';
import { resolveLandingRoute } from '../services/permissions';
import { fetchWorkspaceTeams } from '../services/workspace';
import { useNavigate } from 'react-router-dom';
import './AdminUserAccess.css';

export default function AdminUserAccess() {
  const navigate = useNavigate();
  const currentUser = getCurrentUser();
  const isAdmin = currentUser?.role === 'Admin';
  const organizationName = currentUser?.workspace?.organizationName || 'Your Organization';
  const [users, setUsers] = useState([]);
  const [teams, setTeams] = useState([]);
  const [pendingRoles, setPendingRoles] = useState({});
  const [pendingTeamSelections, setPendingTeamSelections] = useState({});
  const [pendingTeamRoles, setPendingTeamRoles] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const syncCurrentUserSession = useCallback((userData) => {
    const sessionUser = getCurrentUser();
    if (!sessionUser) {
      return;
    }
    const currentSummary = userData.find((user) => user.id === sessionUser.u_id);
    if (!currentSummary) {
      return;
    }
    const currentMembership = (currentSummary.teamMemberships || []).find((membership) => membership.current);
    setCurrentUser({
      ...sessionUser,
      role: currentSummary.role,
      workspace: {
        ...sessionUser.workspace,
        teamId: currentSummary.currentTeamId ?? sessionUser.workspace?.teamId ?? null,
        teamName: currentSummary.currentTeamName ?? sessionUser.workspace?.teamName ?? null,
        teamRole: currentMembership?.role || null,
      },
    });
  }, []);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [userData, teamData] = await Promise.all([fetchUsers(), fetchWorkspaceTeams()]);
      setUsers(userData || []);
      setTeams(teamData || []);
      const initialRoles = {};
      const initialTeamSelections = {};
      const initialTeamRoles = {};
      (userData || []).forEach((user) => {
        initialRoles[user.id] = user.role;
        initialTeamSelections[user.id] = '';
        (user.teamMemberships || []).forEach((membership) => {
          initialTeamRoles[`${user.id}-${membership.teamId}`] = membership.role;
        });
      });
      setPendingRoles(initialRoles);
      setPendingTeamSelections(initialTeamSelections);
      setPendingTeamRoles(initialTeamRoles);
      syncCurrentUserSession(userData || []);
    } catch (err) {
      setError('Failed to load users or teams.');
    } finally {
      setLoading(false);
    }
  }, [syncCurrentUserSession]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const teamOptionsByUser = useMemo(() => {
    const byUser = {};
    users.forEach((user) => {
      const assignedIds = new Set((user.teamMemberships || []).map((membership) => membership.teamId));
      byUser[user.id] = teams.filter((team) => !assignedIds.has(team.teamId));
    });
    return byUser;
  }, [teams, users]);

  const handleRoleChange = (id, value) => {
    setPendingRoles((prev) => ({ ...prev, [id]: value }));
  };

  const handleTeamSelectionChange = (id, value) => {
    setPendingTeamSelections((prev) => ({ ...prev, [id]: value }));
  };

  const handleTeamRoleChange = (userId, teamId, value) => {
    setPendingTeamRoles((prev) => ({ ...prev, [`${userId}-${teamId}`]: value }));
  };

  const handleUpdateRole = async (id) => {
    setError('');
    const role = pendingRoles[id];
    if (!role) {
      setError('Please select a role before saving.');
      return;
    }
    if (!window.confirm('Update this user role?')) {
      return;
    }
    try {
      await updateUserRole(id, role);
      await loadData();
      if (id === getCurrentUser()?.u_id) {
        navigate(resolveLandingRoute(getCurrentUser(), '/user-access'), { replace: true });
      }
    } catch (err) {
      setError('Failed to update user role.');
    }
  };

  const handleAssignTeam = async (id) => {
    setError('');
    const teamId = pendingTeamSelections[id];
    if (!teamId) {
      setError('Select a team before assigning access.');
      return;
    }
    try {
      await assignUserToTeam(id, Number(teamId));
      setPendingTeamSelections((prev) => ({ ...prev, [id]: '' }));
      await loadData();
    } catch (err) {
      setError(typeof err?.response?.data === 'string' ? err.response.data : 'Failed to assign team access.');
    }
  };

  const handleRemoveTeam = async (id, teamId, teamName) => {
    setError('');
    if (!window.confirm(`Remove access to ${teamName}?`)) {
      return;
    }
    try {
      await removeUserFromTeam(id, teamId);
      await loadData();
      if (id === getCurrentUser()?.u_id) {
        navigate(resolveLandingRoute(getCurrentUser(), '/user-access'), { replace: true });
      }
    } catch (err) {
      setError(typeof err?.response?.data === 'string' ? err.response.data : 'Failed to remove team access.');
    }
  };

  const handleUpdateTeamRole = async (userId, teamId) => {
    setError('');
    const role = pendingTeamRoles[`${userId}-${teamId}`];
    if (!role) {
      setError('Please select a team role before saving.');
      return;
    }
    try {
      await updateUserTeamRole(userId, teamId, role);
      await loadData();
      if (userId === getCurrentUser()?.u_id) {
        navigate(resolveLandingRoute(getCurrentUser(), '/user-access'), { replace: true });
      }
    } catch (err) {
      setError(typeof err?.response?.data === 'string' ? err.response.data : 'Failed to update team role.');
    }
  };

  return (
    <div className="container admin-user-access-page">
      <div className="admin-page-hero mb-4">
        <div className="admin-page-hero__eyebrow">Permissions</div>
        <h2 className="mb-1">Manage User Access</h2>
        <div className="text-muted">
          Manage organization roles and team access for users in {organizationName}.
        </div>
      </div>
      <div className="row g-3 mb-4">
        <div className="col-md-4">
          <div className="card admin-page-card h-100">
            <div className="card-body">
              <div className="summary-card__label">Users</div>
              <div className="summary-kpi__value">{users.length}</div>
              <div className="text-muted">Organization users currently visible in access management.</div>
            </div>
          </div>
        </div>
        <div className="col-md-4">
          <div className="card admin-page-card h-100">
            <div className="card-body">
              <div className="summary-card__label">Teams</div>
              <div className="summary-kpi__value">{teams.length}</div>
              <div className="text-muted">Teams available for assignment inside {organizationName}.</div>
            </div>
          </div>
        </div>
        <div className="col-md-4">
          <div className="card admin-page-card h-100">
            <div className="card-body">
              <div className="summary-card__label">Mode</div>
              <div className="summary-kpi__value">{isAdmin ? 'Admin' : 'Read-only'}</div>
              <div className="text-muted">Only org admins can change global roles and team memberships.</div>
            </div>
          </div>
        </div>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}
      {!isAdmin && (
        <div className="alert alert-info">Read-only access. Contact an admin to make changes.</div>
      )}

      <div className="card admin-page-card">
        <div className="card-body">
          <div className="table-responsive">
        <table className="table table-bordered align-middle admin-page-table">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Username</th>
              <th>Org Role</th>
              <th>Team Access</th>
              <th>Assign Team</th>
              {isAdmin && <th style={{ width: '220px' }}>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>{user.id}</td>
                <td>
                  <div className="fw-semibold">{user.username}</div>
                  <div className="text-muted small">
                    Current team: {user.currentTeamName || 'None'}
                  </div>
                </td>
                <td>
                  <select
                    className="form-select"
                    value={pendingRoles[user.id] || user.role}
                    onChange={(e) => handleRoleChange(user.id, e.target.value)}
                    disabled={!isAdmin}
                  >
                    <option value="User">User</option>
                    <option value="Admin">Admin</option>
                  </select>
                </td>
                <td>
                  {(user.teamMemberships || []).length > 0 ? (
                    <div className="d-flex flex-wrap gap-2">
                      {user.teamMemberships.map((membership) => (
                        <div key={`${user.id}-${membership.teamId}`} className="border rounded px-2 py-1 small">
                          <div className="fw-semibold">
                            {membership.teamName}
                            {membership.current ? ' (Current)' : ''}
                          </div>
                          {isAdmin ? (
                            <>
                              <select
                                className="form-select form-select-sm my-2"
                                value={pendingTeamRoles[`${user.id}-${membership.teamId}`] || membership.role}
                                onChange={(e) => handleTeamRoleChange(user.id, membership.teamId, e.target.value)}
                              >
                                <option value="TEAM_ADMIN">TEAM_ADMIN</option>
                                <option value="MANAGER">MANAGER</option>
                                <option value="MEMBER">MEMBER</option>
                              </select>
                              <div className="d-flex gap-2 flex-wrap">
                                <button
                                  type="button"
                                  className="btn btn-outline-primary btn-sm"
                                  onClick={() => handleUpdateTeamRole(user.id, membership.teamId)}
                                >
                                  Save Team Role
                                </button>
                                <button
                                  type="button"
                                  className="btn btn-link btn-sm p-0 text-danger"
                                  onClick={() => handleRemoveTeam(user.id, membership.teamId, membership.teamName)}
                                >
                                  Remove
                                </button>
                              </div>
                            </>
                          ) : (
                            <div className="text-muted">{membership.role}</div>
                          )}
                        </div>
                      ))}
                    </div>
                  ) : (
                    <span className="text-muted small">No team access yet.</span>
                  )}
                </td>
                <td>
                  <select
                    className="form-select"
                    value={pendingTeamSelections[user.id] || ''}
                    onChange={(e) => handleTeamSelectionChange(user.id, e.target.value)}
                    disabled={!isAdmin || (teamOptionsByUser[user.id] || []).length === 0}
                  >
                    <option value="">Select team...</option>
                    {(teamOptionsByUser[user.id] || []).map((team) => (
                      <option key={team.teamId} value={team.teamId}>
                        {team.teamName}
                      </option>
                    ))}
                  </select>
                </td>
                {isAdmin && (
                  <td>
                    <div className="d-flex gap-2 flex-wrap">
                      <button
                        className="btn btn-outline-primary btn-sm"
                        onClick={() => handleUpdateRole(user.id)}
                        disabled={loading}
                      >
                        Update Role
                      </button>
                      <button
                        className="btn btn-outline-success btn-sm"
                        onClick={() => handleAssignTeam(user.id)}
                        disabled={loading || !pendingTeamSelections[user.id]}
                      >
                        Assign Team
                      </button>
                    </div>
                  </td>
                )}
              </tr>
            ))}
            {users.length === 0 && (
              <tr>
                <td colSpan={isAdmin ? 6 : 5} className="text-center">
                  No users found.
                </td>
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
