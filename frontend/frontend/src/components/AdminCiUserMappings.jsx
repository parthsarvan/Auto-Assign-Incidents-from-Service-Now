import React, { useEffect, useState } from 'react';
import {
  fetchCiUserMappings,
  fetchConfigurationItems,
  fetchTeamMembers,
  replaceCiUserMappingsForCi,
} from '../services/admin';
import { getCurrentUser } from '../services/auth';
import { canManageCurrentTeam } from '../services/permissions';
import SetupAssistBanner from './SetupAssistBanner';
import './AdminCrud.css';

export default function AdminCiUserMappings() {
  const [mappings, setMappings] = useState([]);
  const [items, setItems] = useState([]);
  const [members, setMembers] = useState([]);
  const [configurationItemId, setConfigurationItemId] = useState('');
  const [selectedMemberIds, setSelectedMemberIds] = useState([]);
  const [memberToAddId, setMemberToAddId] = useState('');
  const [error, setError] = useState('');
  const canManageTeam = canManageCurrentTeam(getCurrentUser());

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

  const groupedMappings = groupMappingsByCi(mappings);

  const resetForm = () => {
    setConfigurationItemId('');
    setSelectedMemberIds([]);
    setMemberToAddId('');
  };

  const handleAddMember = () => {
    if (!memberToAddId || selectedMemberIds.includes(memberToAddId)) {
      return;
    }
    setSelectedMemberIds((previous) => [...previous, memberToAddId]);
    setMemberToAddId('');
  };

  const handleRemoveMember = (memberId) => {
    setSelectedMemberIds((previous) => previous.filter((id) => id !== memberId));
  };

  const handleMoveMember = (memberId, direction) => {
    setSelectedMemberIds((previous) => {
      const currentIndex = previous.indexOf(memberId);
      const nextIndex = currentIndex + direction;
      if (currentIndex < 0 || nextIndex < 0 || nextIndex >= previous.length) {
        return previous;
      }
      const next = [...previous];
      [next[currentIndex], next[nextIndex]] = [next[nextIndex], next[currentIndex]];
      return next;
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!configurationItemId || selectedMemberIds.length === 0) {
      setError('Select a CI and at least one team member.');
      return;
    }
    try {
      if (!window.confirm('Save this ordered CI owner list?')) {
        return;
      }
      await replaceCiUserMappingsForCi({
        configurationItemId: Number(configurationItemId),
        teamMemberIds: selectedMemberIds.map(Number),
      });
      resetForm();
      await loadData();
    } catch (err) {
      setError('Failed to save CI-user mappings.');
    }
  };

  const handleEditGroup = (group) => {
    setConfigurationItemId(group.configurationItemId ? String(group.configurationItemId) : '');
    setSelectedMemberIds(group.mappings.map((mapping) => String(mapping.teamMember?.tm_id)).filter(Boolean));
    setMemberToAddId('');
  };

  const selectedMembers = selectedMemberIds
    .map((memberId) => members.find((member) => String(member.tm_id) === String(memberId)))
    .filter(Boolean);
  const availableMembers = members.filter((member) => !selectedMemberIds.includes(String(member.tm_id)));

  return (
    <div className="container admin-crud-page">
      <SetupAssistBanner
        title="Setup Step: CI-User Mappings"
        helperText="Map each configuration item to the team members who can take ownership."
      />
      <div className="admin-crud-hero mb-4">
        <div className="admin-crud-hero__eyebrow">Ownership Routing</div>
        <h2 className="mb-1">Manage CI-User Mappings</h2>
        <div className="text-muted">Connect each configuration item to the team members who can own it.</div>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}

      {canManageTeam ? (
        <div className="card p-3 mb-4 admin-crud-card">
          <form className="row g-3 admin-crud-form-grid" onSubmit={handleSubmit}>
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
            <div className="col-md-5">
              <label className="form-label">Add Team Member</label>
              <select
                className="form-select"
                value={memberToAddId}
                onChange={(e) => setMemberToAddId(e.target.value)}
              >
                <option value="">Select Member</option>
                {availableMembers.map((member) => (
                  <option key={member.tm_id} value={member.tm_id}>
                    {member.f_name} {member.l_name}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-3 d-flex align-items-end">
              <button type="button" className="btn btn-outline-primary w-100" onClick={handleAddMember}>
                Add to Order
              </button>
            </div>
            <div className="col-12">
              <label className="form-label">Assignment Order</label>
              {selectedMembers.length === 0 ? (
                <div className="alert alert-info mb-0">
                  Add one or more team members. The list order becomes the round-robin sort order.
                </div>
              ) : (
                <div className="admin-crud-order-list">
                  {selectedMembers.map((member, index) => (
                    <div className="admin-crud-order-item" key={member.tm_id}>
                      <span className="admin-crud-order-rank">{index + 1}</span>
                      <span>{member.f_name} {member.l_name}</span>
                      <div className="ms-auto d-flex gap-2">
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          disabled={index === 0}
                          onClick={() => handleMoveMember(String(member.tm_id), -1)}
                        >
                          Up
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          disabled={index === selectedMembers.length - 1}
                          onClick={() => handleMoveMember(String(member.tm_id), 1)}
                        >
                          Down
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-danger btn-sm"
                          onClick={() => handleRemoveMember(String(member.tm_id))}
                        >
                          Remove
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
            <div className="col-12 d-flex gap-2">
              <button type="submit" className="btn btn-primary">
                Save CI Owner Order
              </button>
              <button type="button" className="btn btn-outline-secondary" onClick={resetForm}>
                Clear
              </button>
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
              <th>CI</th>
              <th>Assignment Order</th>
              {canManageTeam && <th style={{ width: '180px' }}>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {groupedMappings.map((group) => (
              <tr key={group.configurationItemId}>
                <td>{group.configurationItemName}</td>
                <td>
                  <div className="admin-crud-order-chips">
                    {group.mappings.map((mapping, index) => (
                      <span className="badge text-bg-light border" key={mapping.mapping_id}>
                        {index + 1}. {mapping.teamMember?.f_name} {mapping.teamMember?.l_name}
                      </span>
                    ))}
                  </div>
                </td>
                {canManageTeam && (
                  <td className="d-flex gap-2">
                    <button
                      className="btn btn-outline-primary btn-sm"
                      onClick={() => handleEditGroup(group)}
                    >
                      Update
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {groupedMappings.length === 0 && (
              <tr>
                <td colSpan={canManageTeam ? 3 : 2} className="text-center">No mappings yet.</td>
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

function groupMappingsByCi(mappings) {
  const groups = new Map();
  mappings.forEach((mapping) => {
    const configurationItemId = mapping.configurationItem?.ci_id;
    if (!configurationItemId) {
      return;
    }
    if (!groups.has(configurationItemId)) {
      groups.set(configurationItemId, {
        configurationItemId,
        configurationItemName: mapping.configurationItem?.name || '-',
        mappings: [],
      });
    }
    groups.get(configurationItemId).mappings.push(mapping);
  });
  return Array.from(groups.values())
    .map((group) => ({
      ...group,
      mappings: group.mappings.sort((left, right) => {
        const leftOrder = left.sortOrder ?? Number.MAX_SAFE_INTEGER;
        const rightOrder = right.sortOrder ?? Number.MAX_SAFE_INTEGER;
        if (leftOrder !== rightOrder) {
          return leftOrder - rightOrder;
        }
        return String(left.teamMember?.f_name || '').localeCompare(String(right.teamMember?.f_name || ''));
      }),
    }))
    .sort((left, right) => left.configurationItemName.localeCompare(right.configurationItemName));
}
