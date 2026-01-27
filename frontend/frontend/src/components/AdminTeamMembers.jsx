import React, { useEffect, useState } from 'react';
import {
  createTeamMember,
  deleteTeamMember,
  fetchGeos,
  fetchTeamMembers,
  updateTeamMember,
} from '../services/admin';
import { getCurrentUser } from '../services/auth';

export default function AdminTeamMembers() {
  const [teamMembers, setTeamMembers] = useState([]);
  const [geos, setGeos] = useState([]);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [sysId, setSysId] = useState('');
  const [geoId, setGeoId] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const isAdmin = getCurrentUser()?.role === 'Admin';

  const loadTeamMembers = async () => {
    try {
      const [data, geoData] = await Promise.all([
        fetchTeamMembers(),
        fetchGeos(),
      ]);
      setTeamMembers(data);
      setGeos(geoData);
    } catch (err) {
      setError('Failed to load team members.');
    }
  };

  useEffect(() => {
    loadTeamMembers();
  }, []);

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
        await updateTeamMember(editingId, {
          f_name: firstName,
          l_name: lastName,
          email,
          sys_id: sysId,
          geoId,
        });
        setEditingId(null);
        setFirstName('');
        setLastName('');
        setEmail('');
        setSysId('');
        setGeoId('');
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
      await createTeamMember({
        f_name: firstName,
        l_name: lastName,
        email,
        sys_id: sysId,
        geoId,
      });
      setFirstName('');
      setLastName('');
      setEmail('');
      setSysId('');
      setGeoId('');
      loadTeamMembers();
    } catch (err) {
      setError('Failed to create team member.');
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
    setSysId(member.sys_id || '');
    setGeoId(member.geo?.g_id || '');
  };

  const handleCancel = () => {
    setEditingId(null);
    setFirstName('');
    setLastName('');
    setEmail('');
    setSysId('');
    setGeoId('');
  };

  return (
    <div className="container">
      <h4 className="mb-3">Manage Team Members</h4>
      {error && <div className="alert alert-danger">{error}</div>}

      {isAdmin ? (
        <div className="card p-3 mb-4">
          <form className="row g-3" onSubmit={handleSubmit}>
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
            <label className="form-label">ServiceNow User Sys ID</label>
            <input
              type="text"
              className="form-control"
              value={sysId}
              onChange={(e) => setSysId(e.target.value)}
              placeholder="Optional"
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

      <div className="table-responsive">
        <table className="table table-bordered">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>First Name</th>
              <th>Last Name</th>
              <th>Email</th>
              <th>Geo</th>
              {isAdmin && <th style={{ width: '180px' }}>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {teamMembers.map((member) => (
              <tr key={member.tm_id}>
                <td>{member.tm_id}</td>
                <td>{member.f_name}</td>
                <td>{member.l_name}</td>
                <td>{member.email || '-'}</td>
                <td>{member.geo?.name || '-'}</td>
                {isAdmin && (
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
                <td colSpan={isAdmin ? 6 : 5} className="text-center">No team members yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
