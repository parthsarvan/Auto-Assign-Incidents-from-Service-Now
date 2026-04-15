import React, { useEffect, useState } from 'react';
import { DateTime } from 'luxon';
import {
  createLeave,
  deleteLeave,
  fetchLeaves,
  fetchTeamMembers,
  updateLeave,
} from '../services/admin';
import { getCurrentUser } from '../services/auth';
import { canManageCurrentTeam } from '../services/permissions';
import './AdminCrud.css';

export default function AdminLeaves() {
  const [leaves, setLeaves] = useState([]);
  const [members, setMembers] = useState([]);
  const [teamMemberId, setTeamMemberId] = useState('');
  const [startTs, setStartTs] = useState('');
  const [endTs, setEndTs] = useState('');
  const [reason, setReason] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const canManageTeam = canManageCurrentTeam(getCurrentUser());

  const formatUtcToLocal = (isoStr) =>
    DateTime.fromISO(isoStr, { zone: 'utc' })
      .setZone(DateTime.local().zoneName)
      .toFormat('ccc, LLL dd, yyyy hh:mm a');

  const toUtcISOString = (value) => {
    if (!value) return '';
    return new Date(value).toISOString();
  };

  const formatUtcForInput = (isoStr) =>
    DateTime.fromISO(isoStr, { zone: 'utc' })
      .setZone(DateTime.local().zoneName)
      .toFormat("yyyy-MM-dd'T'HH:mm");

  const formatDuration = (start, end) => {
    if (!start || !end) return '-';
    const startUtc = DateTime.fromISO(start, { zone: 'utc' });
    const endUtc = DateTime.fromISO(end, { zone: 'utc' });
    if (!startUtc.isValid || !endUtc.isValid) return '-';
    const minutes = Math.max(0, Math.round(endUtc.diff(startUtc, 'minutes').minutes));
    const totalHours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;
    if (minutes < 60) {
      return `${minutes} min`;
    }
    if (totalHours < 24) {
      return `${totalHours}h ${remainingMinutes}m`;
    }
    const totalDays = Math.floor(totalHours / 24);
    const remainingHours = totalHours % 24;
    return `${totalDays}d ${remainingHours}h`;
  };

  const loadData = async () => {
    try {
      const [leaveData, memberData] = await Promise.all([
        fetchLeaves(),
        fetchTeamMembers(),
      ]);
      setLeaves(leaveData);
      setMembers(memberData);
    } catch (err) {
      setError('Failed to load leaves.');
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editingId) {
        if (!window.confirm('Update this leave entry?')) {
          return;
        }
        await updateLeave(editingId, {
          teamMemberId,
          startTs: toUtcISOString(startTs),
          endTs: toUtcISOString(endTs),
          reason,
        });
        setEditingId(null);
        setTeamMemberId('');
        setStartTs('');
        setEndTs('');
        setReason('');
        loadData();
        return;
      }
      if (!window.confirm('Add this leave entry?')) {
        return;
      }
      await createLeave({
        teamMemberId,
        startTs: toUtcISOString(startTs),
        endTs: toUtcISOString(endTs),
        reason,
      });
      setTeamMemberId('');
      setStartTs('');
      setEndTs('');
      setReason('');
      loadData();
    } catch (err) {
      setError('Failed to create leave.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this leave entry?')) {
      return;
    }
    await deleteLeave(id);
    loadData();
  };

  const handleEdit = (leave) => {
    setEditingId(leave.leave_id);
    setTeamMemberId(leave.teamMember?.tm_id || '');
    setStartTs(formatUtcForInput(leave.startTs));
    setEndTs(formatUtcForInput(leave.endTs));
    setReason(leave.reason || '');
  };

  const handleCancel = () => {
    setEditingId(null);
    setTeamMemberId('');
    setStartTs('');
    setEndTs('');
    setReason('');
  };

  return (
    <div className="container admin-crud-page">
      <div className="admin-crud-hero mb-4">
        <div className="admin-crud-hero__eyebrow">Availability Controls</div>
        <h2 className="mb-1">Manage Leaves</h2>
        <div className="text-muted">Record planned leave so assignment logic can avoid unavailable team members.</div>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}

      {canManageTeam ? (
        <div className="card p-3 mb-4 admin-crud-card">
          <form className="row g-3 admin-crud-form-grid" onSubmit={handleSubmit}>
          <div className="col-md-3">
            <label className="form-label">Team Member</label>
            <select
              className="form-select"
              value={teamMemberId}
              onChange={(e) => setTeamMemberId(e.target.value)}
              required
            >
              <option value="">Select Member</option>
              {members.map((member) => (
                <option key={member.tm_id} value={member.tm_id}>
                  {member.f_name} {member.l_name}
                </option>
              ))}
            </select>
          </div>
          <div className="col-md-3">
            <label className="form-label">Start (Local)</label>
            <input
              type="datetime-local"
              className="form-control"
              value={startTs}
              onChange={(e) => setStartTs(e.target.value)}
              required
            />
          </div>
          <div className="col-md-3">
            <label className="form-label">End (Local)</label>
            <input
              type="datetime-local"
              className="form-control"
              value={endTs}
              onChange={(e) => setEndTs(e.target.value)}
              required
            />
          </div>
          <div className="col-md-3">
            <label className="form-label">Reason</label>
            <input
              type="text"
              className="form-control"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          </div>
            <div className="col-12 d-flex gap-2">
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update Leave' : 'Add Leave'}
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
              <th>Team Member</th>
              <th>Start (Local)</th>
              <th>End (Local)</th>
              <th>Duration</th>
              <th>Reason</th>
              {canManageTeam && <th style={{ width: '180px' }}>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {leaves.map((leave) => (
              <tr key={leave.leave_id}>
                <td>{leave.leave_id}</td>
                <td>
                  {leave.teamMember?.f_name} {leave.teamMember?.l_name}
                </td>
                <td>{formatUtcToLocal(leave.startTs)}</td>
                <td>{formatUtcToLocal(leave.endTs)}</td>
                <td>{formatDuration(leave.startTs, leave.endTs)}</td>
                <td>{leave.reason || '-'}</td>
                {canManageTeam && (
                  <td className="d-flex gap-2">
                    <button
                      className="btn btn-outline-primary btn-sm"
                      onClick={() => handleEdit(leave)}
                    >
                      Update
                    </button>
                    <button
                      className="btn btn-outline-danger btn-sm"
                      onClick={() => handleDelete(leave.leave_id)}
                    >
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {leaves.length === 0 && (
              <tr>
                <td colSpan={canManageTeam ? 7 : 6} className="text-center">No leaves yet.</td>
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
