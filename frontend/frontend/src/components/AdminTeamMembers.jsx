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
  const [geoId, setGeoId] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const canManageTeam = canManageCurrentTeam(getCurrentUser());

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

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editingId) {
        if (!window.confirm('Update this team member?')) {
          return;
        }
        if (!geoId) {
          setError('Please select a geo.');
          return;
        }
        if (!sysId.trim()) {
          setError('ServiceNow User Sys ID is required.');
          return;
        }
        await updateTeamMember(editingId, {
          f_name: firstName,
          l_name: lastName,
          email,
          phone,
          sys_id: sysId,
          geoId,
        });
        setEditingId(null);
        setFirstName('');
        setLastName('');
        setEmail('');
        setPhone('');
        setSysId('');
        setGeoId('');
        setSelectedUserId('');
        loadTeamMembers();
        return;
      }
      if (!window.confirm('Add this team member?')) {
        return;
      }
      if (!geoId) {
        setError('Please select a geo.');
        return;
      }
      if (!sysId.trim()) {
        setError('ServiceNow User Sys ID is required.');
        return;
      }
      await createTeamMember({
        f_name: firstName,
        l_name: lastName,
        email,
        phone,
        sys_id: sysId,
        geoId,
      });
      setFirstName('');
      setLastName('');
      setEmail('');
      setPhone('');
      setSysId('');
      setGeoId('');
      setSelectedUserId('');
      loadTeamMembers();
    } catch (err) {
      setError(typeof err?.response?.data === 'string' ? err.response.data : 'Failed to create team member.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this team member?')) {
      return;
    }
    await deleteTeamMember(id);
    loadTeamMembers();
  };

  const handleEdit = (member) => {
    setEditingId(member.tm_id);
    setFirstName(member.f_name || '');
    setLastName(member.l_name || '');
    setEmail(member.email || '');
    setPhone(member.phone || '');
    setSysId(member.sys_id || '');
    setGeoId(member.geo?.g_id || '');
  };

  const handleCancel = () => {
    setEditingId(null);
    setSelectedUserId('');
    setFirstName('');
    setLastName('');
    setEmail('');
    setPhone('');
    setSysId('');
    setGeoId('');
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
  };

  return (
    <div className="container admin-crud-page">
      <SetupAssistBanner
        title="Setup Step: Team Members"
        helperText="Add the people on your team together with their ServiceNow user sys IDs."
      />
      <div className="admin-crud-hero mb-4">
        <div className="admin-crud-hero__eyebrow">People Directory</div>
        <h2 className="mb-1">Manage Team Members</h2>
        <div className="text-muted">Maintain the team roster, geo ownership, and ServiceNow user identities.</div>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}

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
              Pick a joined team user to prefill name and email, or leave this blank and enter everything manually.
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
              onChange={(e) => setEmail(e.target.value)}
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
            <label className="form-label">ServiceNow User Sys ID</label>
            <input
              type="text"
              className="form-control"
              value={sysId}
              onChange={(e) => setSysId(e.target.value)}
              required
            />
          </div>
          <div className="col-md-2">
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
          <div className="col-12 d-flex gap-2">
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update Team Member' : 'Add Team Member'}
              </button>
              {editingId && (
                <button type="button" className="btn btn-outline-secondary" onClick={handleCancel}>
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
                      onClick={() => handleDelete(member.tm_id)}
                    >
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {teamMembers.length === 0 && (
              <tr>
                <td colSpan={canManageTeam ? 7 : 6} className="text-center">No team members yet.</td>
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
