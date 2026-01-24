import React, { useEffect, useState } from 'react';
import { DateTime } from 'luxon';
import { createBreak, deleteBreak, fetchBreaks, fetchTeamMembers } from '../services/admin';

export default function AdminBreaks() {
  const [breaks, setBreaks] = useState([]);
  const [members, setMembers] = useState([]);
  const [teamMemberId, setTeamMemberId] = useState('');
  const [startTs, setStartTs] = useState('');
  const [endTs, setEndTs] = useState('');
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');

  const formatUtcToLocal = (isoStr) =>
    DateTime.fromISO(isoStr, { zone: 'utc' })
      .setZone(DateTime.local().zoneName)
      .toFormat('ccc, LLL dd, yyyy hh:mm a');

  const toUtcISOString = (value) => {
    if (!value) return '';
    return new Date(value).toISOString();
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

  return (
    <div className="container">
      <h4 className="mb-3">Manage Breaks</h4>
      {error && <div className="alert alert-danger">{error}</div>}

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
          <div className="col-12">
            <button type="submit" className="btn btn-primary">Add Break</button>
          </div>
        </form>
      </div>

      <div className="table-responsive">
        <table className="table table-bordered">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Team Member</th>
              <th>Start (Local)</th>
              <th>End (Local)</th>
              <th>Reason</th>
              <th style={{ width: '120px' }}>Actions</th>
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
                <td>{entry.reason || '-'}</td>
                <td>
                  <button
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => handleDelete(entry.break_id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {breaks.length === 0 && (
              <tr>
                <td colSpan="6" className="text-center">No breaks yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
