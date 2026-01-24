import React, { useEffect, useState } from 'react';
import { createTeamMember, deleteTeamMember, fetchTeamMembers } from '../services/admin';

export default function AdminTeamMembers() {
  const [teamMembers, setTeamMembers] = useState([]);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [error, setError] = useState('');

  const loadTeamMembers = async () => {
    try {
      const data = await fetchTeamMembers();
      setTeamMembers(data);
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
      await createTeamMember({ f_name: firstName, l_name: lastName });
      setFirstName('');
      setLastName('');
      loadTeamMembers();
    } catch (err) {
      setError('Failed to create team member.');
    }
  };

  const handleDelete = async (id) => {
    await deleteTeamMember(id);
    loadTeamMembers();
  };

  return (
    <div className="container">
      <h4 className="mb-3">Manage Team Members</h4>
      {error && <div className="alert alert-danger">{error}</div>}

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
          <div className="col-12">
            <button type="submit" className="btn btn-primary">Add Team Member</button>
          </div>
        </form>
      </div>

      <div className="table-responsive">
        <table className="table table-bordered">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>First Name</th>
              <th>Last Name</th>
              <th style={{ width: '120px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {teamMembers.map((member) => (
              <tr key={member.tm_id}>
                <td>{member.tm_id}</td>
                <td>{member.f_name}</td>
                <td>{member.l_name}</td>
                <td>
                  <button
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => handleDelete(member.tm_id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {teamMembers.length === 0 && (
              <tr>
                <td colSpan="4" className="text-center">No team members yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
