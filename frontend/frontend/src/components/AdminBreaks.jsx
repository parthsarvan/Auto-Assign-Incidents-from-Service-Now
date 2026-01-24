import React, { useEffect, useState } from 'react';
import { DateTime } from 'luxon';
import {
  createBreak,
  deleteBreak,
  fetchBreaks,
  fetchTeamMembers,
  updateBreak,
} from '../services/admin';
import { getCurrentUser } from '../services/auth';

export default function AdminBreaks() {
  const [breaks, setBreaks] = useState([]);
  const [members, setMembers] = useState([]);
  const [teamMemberId, setTeamMemberId] = useState('');
  const [startTs, setStartTs] = useState('');
  const [endTs, setEndTs] = useState('');
  const [reason, setReason] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const isAdmin = getCurrentUser()?.role === 'Admin';

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
      const [breakData, memberData] = await Promise.all([
        fetchBreaks(),
        fetchTeamMembers(),
      ]);
      setBreaks(breakData);
      setMembers(memberData);
    } catch (err) {
      setError('Failed to load breaks.');
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
        if (!window.confirm('Update this break entry?')) {
          return;
        }
        await updateBreak(editingId, {
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
      if (!window.confirm('Add this break entry?')) {
        return;
      }
      await createBreak({
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
      setError('Failed to create break.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this break entry?')) {
      return;
    }
    await deleteBreak(id);
    loadData();
  };

  const handleEdit = (entry) => {
    setEditingId(entry.break_id);
    setTeamMemberId(entry.teamMember?.tm_id || '');
    setStartTs(formatUtcForInput(entry.startTs));
    setEndTs(formatUtcForInput(entry.endTs));
    setReason(entry.reason || '');
  };

  const handleCancel = () => {
    setEditingId(null);
    setTeamMemberId('');
    setStartTs('');
    setEndTs('');
    setReason('');
  };

  return (
    <div className="container">
      <h4 className="mb-3">Manage Breaks</h4>
      {error && <div className="alert alert-danger">{error}</div>}

      {isAdmin ? (
        <div className="card p-3 mb-4">
          <form className="row g-3" onSubmit={handleSubmit}>
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
                {editingId ? 'Update Break' : 'Add Break'}
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
              <th>Team Member</th>
              <th>Start (Local)</th>
              <th>End (Local)</th>
              <th>Duration</th>
              <th>Reason</th>
              {isAdmin && <th style={{ width: '180px' }}>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {breaks.map((entry) => (
              <tr key={entry.break_id}>
                <td>{entry.break_id}</td>
                <td>
                  {entry.teamMember?.f_name} {entry.teamMember?.l_name}
                </td>
                <td>{formatUtcToLocal(entry.startTs)}</td>
                <td>{formatUtcToLocal(entry.endTs)}</td>
                <td>{formatDuration(entry.startTs, entry.endTs)}</td>
                <td>{entry.reason || '-'}</td>
                {isAdmin && (
                  <td className="d-flex gap-2">
                    <button
                      className="btn btn-outline-primary btn-sm"
                      onClick={() => handleEdit(entry)}
                    >
                      Update
                    </button>
                    <button
                      className="btn btn-outline-danger btn-sm"
                      onClick={() => handleDelete(entry.break_id)}
                    >
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {breaks.length === 0 && (
              <tr>
                <td colSpan={isAdmin ? 7 : 6} className="text-center">No breaks yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
