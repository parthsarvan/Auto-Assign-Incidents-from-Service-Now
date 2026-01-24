import React, { useEffect, useState } from 'react';
import {
  createCiUserMapping,
  deleteCiUserMapping,
  fetchCiUserMappings,
  fetchConfigurationItems,
  fetchTeamMembers,
} from '../services/admin';

export default function AdminCiUserMappings() {
  const [mappings, setMappings] = useState([]);
  const [items, setItems] = useState([]);
  const [members, setMembers] = useState([]);
  const [configurationItemId, setConfigurationItemId] = useState('');
  const [teamMemberId, setTeamMemberId] = useState('');
  const [sortOrder, setSortOrder] = useState('');
  const [error, setError] = useState('');

  const loadData = async () => {
    try {
      const [itemData, memberData, mappingData] = await Promise.all([
        fetchConfigurationItems(),
        fetchTeamMembers(),
        fetchCiUserMappings(),
      ]);
      setItems(itemData);
      setMembers(memberData);
      setMappings(mappingData);
    } catch (err) {
      setError('Failed to load CI-user mappings.');
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (!window.confirm('Add this CI-user mapping?')) {
        return;
      }
      await createCiUserMapping({
        configurationItemId,
        teamMemberId,
        sortOrder: sortOrder === '' ? null : Number(sortOrder),
      });
      setConfigurationItemId('');
      setTeamMemberId('');
      setSortOrder('');
      loadData();
    } catch (err) {
      setError('Failed to create mapping.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this CI-user mapping?')) {
      return;
    }
    await deleteCiUserMapping(id);
    loadData();
  };

  return (
    <div className="container">
      <h4 className="mb-3">Manage CI-User Mappings</h4>
      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card p-3 mb-4">
        <form className="row g-3" onSubmit={handleSubmit}>
          <div className="col-md-4">
            <label className="form-label">Configuration Item</label>
            <select
              className="form-select"
              value={configurationItemId}
              onChange={(e) => setConfigurationItemId(e.target.value)}
              required
            >
              <option value="">Select CI</option>
              {items.map((item) => (
                <option key={item.ci_id} value={item.ci_id}>{item.name}</option>
              ))}
            </select>
          </div>
          <div className="col-md-4">
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
          <div className="col-md-2">
            <label className="form-label">Sort Order</label>
            <input
              type="number"
              className="form-control"
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value)}
            />
          </div>
          <div className="col-12">
            <button type="submit" className="btn btn-primary">Add Mapping</button>
          </div>
        </form>
      </div>

      <div className="table-responsive">
        <table className="table table-bordered">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>CI</th>
              <th>Team Member</th>
              <th>Sort Order</th>
              <th style={{ width: '120px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {mappings.map((mapping) => (
              <tr key={mapping.mapping_id}>
                <td>{mapping.mapping_id}</td>
                <td>{mapping.configurationItem?.name}</td>
                <td>
                  {mapping.teamMember?.f_name} {mapping.teamMember?.l_name}
                </td>
                <td>{mapping.sortOrder ?? '-'}</td>
                <td>
                  <button
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => handleDelete(mapping.mapping_id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {mappings.length === 0 && (
              <tr>
                <td colSpan="5" className="text-center">No mappings yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
