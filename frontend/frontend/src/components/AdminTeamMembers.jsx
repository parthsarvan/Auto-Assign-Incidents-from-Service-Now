import React, { useCallback, useEffect, useState } from 'react';
import {
  createTeamMember,
  deleteTeamMember,
  fetchGeos,
  fetchJoinedTeamUsers,
  fetchTeamMembers,
  updateTeamMember,
} from '../services/admin';
import { getCurrentUser } from '../services/auth';
import { canManageCurrentTeam } from '../services/permissions';
import { searchServiceNowUsers } from '../services/servicenow';
import SetupAssistBanner from './SetupAssistBanner';
import './AdminCrud.css';

export default function AdminTeamMembers() {
  const [teamMembers, setTeamMembers] = useState([]);
  const [geos, setGeos] = useState([]);
  const [joinedUsers, setJoinedUsers] = useState([]);
  const [selectedUserId, setSelectedUserId] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [sysId, setSysId] = useState('');
  const [serviceNowSearch, setServiceNowSearch] = useState('');
  const [serviceNowResults, setServiceNowResults] = useState([]);
  const [serviceNowLookupLoading, setServiceNowLookupLoading] = useState(false);
  const [serviceNowLookupComplete, setServiceNowLookupComplete] = useState(false);
  const [serviceNowLookupError, setServiceNowLookupError] = useState('');
  const [selectedServiceNowLabel, setSelectedServiceNowLabel] = useState('');
  const [geoId, setGeoId] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const canManageTeam = canManageCurrentTeam(getCurrentUser());

  const resetForm = () => {
    setEditingId(null);
    setSelectedUserId('');
    setFirstName('');
    setLastName('');
    setEmail('');
    setPhone('');
    setSysId('');
    setServiceNowSearch('');
    setServiceNowResults([]);
    setServiceNowLookupComplete(false);
    setServiceNowLookupError('');
    setSelectedServiceNowLabel('');
    setGeoId('');
  };

  const loadTeamMembers = useCallback(async () => {
    try {
      const [data, geoData, userData] = await Promise.all([
        fetchTeamMembers(),
        fetchGeos(),
        canManageTeam ? fetchJoinedTeamUsers() : Promise.resolve([]),
      ]);
      setTeamMembers(data);
      setGeos(geoData);
      const eligibleUsers = (userData || []).map((user) => ({
        ...user,
        displayName: [user.firstName, user.lastName].filter(Boolean).join(' ').trim() || user.username,
        alreadyMapped: Boolean(user.workEmail) && (data || []).some((member) =>
          (member.email || '').trim().toLowerCase() === user.workEmail.trim().toLowerCase()
        ),
      }));
      setJoinedUsers(eligibleUsers);
      if (selectedUserId) {
        const stillAvailable = eligibleUsers.some((user) => String(user.id) === String(selectedUserId));
        if (!stillAvailable) {
          setSelectedUserId('');
        }
      }
    } catch (err) {
      setError('Failed to load team members.');
    }
  }, [canManageTeam, selectedUserId]);

  useEffect(() => {
    loadTeamMembers();
  }, [loadTeamMembers]);

  useEffect(() => {
    if (!canManageTeam || serviceNowSearch.trim().length < 2) {
      setServiceNowResults([]);
      setServiceNowLookupLoading(false);
      setServiceNowLookupComplete(false);
      setServiceNowLookupError('');
      return undefined;
    }

    let active = true;
    setServiceNowLookupLoading(true);
    setServiceNowLookupError('');
    const timer = window.setTimeout(async () => {
      try {
        const results = await searchServiceNowUsers(serviceNowSearch.trim());
        if (active) {
          setServiceNowResults(results || []);
          setServiceNowLookupComplete(true);
        }
      } catch (err) {
        if (active) {
          setServiceNowResults([]);
          setServiceNowLookupComplete(true);
          setServiceNowLookupError(typeof err?.response?.data === 'string'
            ? err.response.data
            : 'ServiceNow lookup failed. Check the connection and permissions.');
        }
      } finally {
        if (active) {
          setServiceNowLookupLoading(false);
        }
      }
    }, 300);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [canManageTeam, serviceNowSearch]);

  useEffect(() => {
    if (sysId || !email || serviceNowResults.length !== 1) {
      return;
    }
    const onlyResult = serviceNowResults[0];
    if ((onlyResult.email || '').trim().toLowerCase() === email.trim().toLowerCase()) {
      handleServiceNowUserSelect(onlyResult, { preserveSearch: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [email, serviceNowResults, sysId]);

  const handleServiceNowUserSelect = (result, options = {}) => {
    const label = result.displayName || result.email || '';
    setSysId(result.sysId || '');
    setSelectedServiceNowLabel(label);
    if (!options.preserveSearch) {
      setServiceNowSearch(result.email || label);
    }
    if (result.email) {
      setEmail(result.email);
    }
    const parts = (result.displayName || '').trim().split(/\s+/);
    if (!firstName && parts[0]) {
      setFirstName(parts[0]);
    }
    if (!lastName && parts.length > 1) {
      setLastName(parts.slice(1).join(' '));
    }
    setServiceNowResults([]);
    setServiceNowLookupComplete(false);
    setServiceNowLookupError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    try {
      if (!geoId) {
        setError('Please select a geo.');
        return;
      }
      if (!sysId.trim()) {
        setError('Select the matching ServiceNow user.');
        return;
      }

      const payload = {
        f_name: firstName,
        l_name: lastName,
        email,
        phone,
        sys_id: sysId,
        geoId,
      };

      if (editingId) {
        if (!window.confirm('Update this team member?')) {
          return;
        }
        await updateTeamMember(editingId, payload);
        resetForm();
        loadTeamMembers();
        return;
      }

      if (!window.confirm('Add this team member?')) {
        return;
      }
      await createTeamMember(payload);
      resetForm();
      loadTeamMembers();
    } catch (err) {
      setError(typeof err?.response?.data === 'string' ? err.response.data : 'Failed to save team member.');
    }
  };

  const handleDelete = async (member) => {
    setError('');
    setMessage('');
    const memberName = `${member.f_name || ''} ${member.l_name || ''}`.trim() || member.email || 'this team member';
    if (!window.confirm(`Delete ${memberName}? This removes roster routing, schedules, leaves, and breaks. If a linked InciTeam account exists, that account will also be deleted.`)) {
      return;
    }
    try {
      const response = await deleteTeamMember(member.tm_id);
      setMessage(response?.message || 'Team member deleted.');
      loadTeamMembers();
    } catch (err) {
      setError(typeof err?.response?.data === 'string' ? err.response.data : 'Failed to delete team member.');
    }
  };

  const handleEdit = (member) => {
    setEditingId(member.tm_id);
    setSelectedUserId('');
    setFirstName(member.f_name || '');
    setLastName(member.l_name || '');
    setEmail(member.email || '');
    setPhone(member.phone || '');
    setSysId(member.sys_id || '');
    setServiceNowSearch(member.email || `${member.f_name || ''} ${member.l_name || ''}`.trim());
    setSelectedServiceNowLabel(`${member.f_name || ''} ${member.l_name || ''}`.trim());
    setServiceNowLookupComplete(false);
    setServiceNowLookupError('');
    setGeoId(member.geo?.g_id || '');
  };

  const handleLinkedUserChange = (userId) => {
    setSelectedUserId(userId);
    if (!userId) {
      return;
    }
    const selectedUser = joinedUsers.find((user) => String(user.id) === String(userId));
    if (!selectedUser) {
      return;
    }
    setFirstName(selectedUser.firstName || '');
    setLastName(selectedUser.lastName || '');
    setEmail(selectedUser.workEmail || '');
    setSysId('');
    setSelectedServiceNowLabel('');
    setServiceNowSearch(selectedUser.workEmail || selectedUser.displayName || '');
    setServiceNowLookupComplete(false);
    setServiceNowLookupError('');
  };

  return (
    <div className="container admin-crud-page">
      <SetupAssistBanner
        title="Setup Step: Team Members"
        helperText="Add team members and link them to ServiceNow users without manual ID lookup."
      />
      <div className="admin-crud-hero mb-4">
        <div className="admin-crud-hero__eyebrow">People Directory</div>
        <h2 className="mb-1">Manage Team Members</h2>
        <div className="text-muted">Maintain the team roster, geo ownership, and ServiceNow user identities.</div>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}

      {canManageTeam ? (
        <div className="card p-3 mb-4 admin-crud-card">
          <form className="row g-3 admin-crud-form-grid" onSubmit={handleSubmit}>
            <div className="col-12">
              <label className="form-label">Joined InciTeam User</label>
              <select
                className="form-select"
                value={selectedUserId}
                onChange={(e) => handleLinkedUserChange(e.target.value)}
                disabled={Boolean(editingId)}
              >
                <option value="">Select an existing joined user (optional)</option>
                {joinedUsers.map((user) => (
                  <option key={user.id} value={user.id} disabled={user.alreadyMapped}>
                    {user.displayName}{user.workEmail ? ` (${user.workEmail})` : ''}
                  </option>
                ))}
              </select>
              <div className="form-text">
                Pick a joined user to prefill details. InciTeam will search ServiceNow by that email.
              </div>
            </div>
            <div className="col-md-5">
              <label className="form-label">First Name</label>
              <input
                type="text"
                className="form-control"
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                required
              />
            </div>
            <div className="col-md-5">
              <label className="form-label">Last Name</label>
              <input
                type="text"
                className="form-control"
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                required
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Email</label>
              <input
                type="email"
                className="form-control"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  setSysId('');
                  setSelectedServiceNowLabel('');
                  setServiceNowSearch(e.target.value);
                }}
                required
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Phone</label>
              <input
                type="tel"
                className="form-control"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="Optional"
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Geo</label>
              <select
                className="form-select"
                value={geoId}
                onChange={(e) => setGeoId(e.target.value)}
                required
              >
                <option value="">Select Geo</option>
                {geos.map((geo) => (
                  <option key={geo.g_id} value={geo.g_id}>
                    {geo.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12">
              <label className="form-label">Find ServiceNow User</label>
              <input
                type="search"
                className="form-control"
                value={serviceNowSearch}
                onChange={(e) => {
	                  setServiceNowSearch(e.target.value);
	                  setSysId('');
	                  setSelectedServiceNowLabel('');
	                  setServiceNowLookupComplete(false);
	                  setServiceNowLookupError('');
	                }}
                placeholder="Search by email, name, or ServiceNow username"
              />
              <div className="form-text">
                Select the matching ServiceNow user. InciTeam keeps the ServiceNow link behind the scenes.
	              </div>
	              {serviceNowLookupLoading && <div className="lookup-status">Searching ServiceNow...</div>}
	              {serviceNowLookupError && <div className="lookup-status lookup-status--error">{serviceNowLookupError}</div>}
	              {!serviceNowLookupLoading
	                && !serviceNowLookupError
	                && serviceNowLookupComplete
	                && !selectedServiceNowLabel
	                && serviceNowResults.length === 0 && (
	                  <div className="lookup-status lookup-status--warning">
	                    No matching ServiceNow user found. Try the user's email or ServiceNow username.
	                  </div>
	                )}
	              {serviceNowResults.length > 0 && (
                <div className="lookup-results">
                  {serviceNowResults.map((result) => (
                    <button
                      key={result.sysId}
                      type="button"
                      className="lookup-result"
                      onClick={() => handleServiceNowUserSelect(result)}
                    >
                      <strong>{result.displayName}</strong>
                      <span>{[result.email, result.userName].filter(Boolean).join(' / ') || 'ServiceNow user'}</span>
                    </button>
                  ))}
                </div>
              )}
              {selectedServiceNowLabel && (
                <div className="linked-record-badge">Linked ServiceNow user: {selectedServiceNowLabel}</div>
              )}
            </div>
            <div className="col-12 d-flex gap-2">
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update Team Member' : 'Add Team Member'}
              </button>
              {editingId && (
                <button type="button" className="btn btn-outline-secondary" onClick={resetForm}>
                  Cancel
                </button>
              )}
            </div>
          </form>
        </div>
      ) : (
        <div className="alert alert-info">Read-only access. Contact an admin to make changes.</div>
      )}

      <div className="card admin-crud-card">
        <div className="card-body">
          <div className="table-responsive">
            <table className="table table-bordered admin-crud-table">
              <thead className="table-light">
                <tr>
                  <th>ID</th>
                  <th>First Name</th>
                  <th>Last Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Geo</th>
                  <th>ServiceNow Link</th>
                  {canManageTeam && <th style={{ width: '180px' }}>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {teamMembers.map((member) => (
                  <tr key={member.tm_id}>
                    <td>{member.tm_id}</td>
                    <td>{member.f_name}</td>
                    <td>{member.l_name}</td>
                    <td>{member.email || '-'}</td>
                    <td>{member.phone || '-'}</td>
                    <td>{member.geo?.name || '-'}</td>
                    <td>{member.sys_id ? 'Linked' : 'Needs link'}</td>
                    {canManageTeam && (
                      <td className="d-flex gap-2">
                        <button
                          className="btn btn-outline-primary btn-sm"
                          onClick={() => handleEdit(member)}
                        >
                          Update
                        </button>
                        <button
                          className="btn btn-outline-danger btn-sm"
                          onClick={() => handleDelete(member)}
                        >
                          Delete
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
                {teamMembers.length === 0 && (
                  <tr>
                    <td colSpan={canManageTeam ? 8 : 7} className="text-center">No team members yet.</td>
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
