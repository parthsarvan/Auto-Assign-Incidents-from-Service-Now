import React, { useEffect, useState } from 'react';
import { createLeave, deleteLeave, fetchLeaves, fetchTeamMembers } from '../services/admin';

export default function AdminLeaves() {
  const [leaves, setLeaves] = useState([]);
  const [members, setMembers] = useState([]);
  const [teamMemberId, setTeamMemberId] = useState('');
  const [startTs, setStartTs] = useState('');
  const [endTs, setEndTs] = useState('');
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');

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
      await createLeave({ teamMemberId, startTs, endTs, reason });
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
    await deleteLeave(id);
    loadData();
  };

  return (
    <div className="container">
      <h4 className="mb-3">Manage Leaves</h4>
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
            <label className="form-label">Start (UTC)</label>
            <input
              type="datetime-local"
              className="form-control"
              value={startTs}
              onChange={(e) => setStartTs(e.target.value)}
              required
            />
          </div>
          <div className="col-md-3">
            <label className="form-label">End (UTC)</label>
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
            <button type="submit" className="btn btn-primary">Add Leave</button>
          </div>
        </form>
      </div>

      <div className="table-responsive">
        <table className="table table-bordered">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Team Member</th>
              <th>Start (UTC)</th>
              <th>End (UTC)</th>
              <th>Reason</th>
              <th style={{ width: '120px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {leaves.map((leave) => (
              <tr key={leave.leave_id}>
                <td>{leave.leave_id}</td>
                <td>
                  {leave.teamMember?.f_name} {leave.teamMember?.l_name}
                </td>
                <td>{leave.startTs}</td>
                <td>{leave.endTs}</td>
                <td>{leave.reason || '-'}</td>
                <td>
                  <button
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => handleDelete(leave.leave_id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {leaves.length === 0 && (
              <tr>
                <td colSpan="6" className="text-center">No leaves yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
