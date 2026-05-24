import React, { useEffect, useState } from 'react';
import {
  createConfigurationItem,
  deleteConfigurationItem,
  fetchConfigurationItems,
  updateConfigurationItem,
} from '../services/admin';
import { getCurrentUser } from '../services/auth';
import { canManageCurrentTeam } from '../services/permissions';
import { searchServiceNowConfigurationItems } from '../services/servicenow';
import SetupAssistBanner from './SetupAssistBanner';
import './AdminCrud.css';

export default function AdminConfigurationItems() {
  const [items, setItems] = useState([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [serviceNowSysId, setServiceNowSysId] = useState('');
  const [serviceNowSearch, setServiceNowSearch] = useState('');
  const [serviceNowResults, setServiceNowResults] = useState([]);
  const [serviceNowLookupLoading, setServiceNowLookupLoading] = useState(false);
  const [serviceNowLookupComplete, setServiceNowLookupComplete] = useState(false);
  const [serviceNowLookupError, setServiceNowLookupError] = useState('');
  const [selectedServiceNowLabel, setSelectedServiceNowLabel] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const canManageTeam = canManageCurrentTeam(getCurrentUser());

  const resetForm = () => {
    setEditingId(null);
    setName('');
    setDescription('');
    setServiceNowSysId('');
    setServiceNowSearch('');
    setServiceNowResults([]);
    setServiceNowLookupComplete(false);
    setServiceNowLookupError('');
    setSelectedServiceNowLabel('');
  };

  const loadItems = async () => {
    try {
      const data = await fetchConfigurationItems();
      setItems(data);
    } catch (err) {
      setError('Failed to load configuration items.');
    }
  };

  useEffect(() => {
    loadItems();
  }, []);

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
        const results = await searchServiceNowConfigurationItems(serviceNowSearch.trim());
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

  const handleServiceNowCiSelect = (result) => {
    const label = result.displayName || '';
    setServiceNowSysId(result.sysId || '');
    setSelectedServiceNowLabel(label);
    setServiceNowSearch(label);
    setName(label);
    setDescription([result.detail, result.secondaryDetail].filter(Boolean).join(' / '));
    setServiceNowResults([]);
    setServiceNowLookupComplete(false);
    setServiceNowLookupError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (!serviceNowSysId.trim()) {
        setError('Select the matching ServiceNow CI record.');
        return;
      }

      if (editingId) {
        if (!window.confirm('Update this configuration item?')) {
          return;
        }
        await updateConfigurationItem(editingId, { name, description, serviceNowSysId });
        resetForm();
        loadItems();
        return;
      }

      if (!window.confirm('Add this configuration item?')) {
        return;
      }
      await createConfigurationItem({ name, description, serviceNowSysId });
      resetForm();
      loadItems();
    } catch (err) {
      setError(typeof err?.response?.data === 'string' ? err.response.data : 'Failed to save configuration item.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this configuration item?')) {
      return;
    }
    await deleteConfigurationItem(id);
    loadItems();
  };

  const handleEdit = (item) => {
    setEditingId(item.ci_id);
    setName(item.name || '');
    setDescription(item.description || '');
    setServiceNowSysId(item.serviceNowSysId || '');
    setServiceNowSearch(item.name || '');
    setSelectedServiceNowLabel(item.name || '');
    setServiceNowResults([]);
    setServiceNowLookupComplete(false);
    setServiceNowLookupError('');
  };

  return (
    <div className="container admin-crud-page">
      <SetupAssistBanner
        title="Setup Step: Configuration Items"
        helperText="Search ServiceNow and link each supported CI without manual ID lookup."
      />
      <div className="admin-crud-hero mb-4">
        <div className="admin-crud-hero__eyebrow">Configuration Inventory</div>
        <h2 className="mb-1">Manage Configuration Items</h2>
        <div className="text-muted">Track the systems this team supports and connect them to ServiceNow records.</div>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}

      {canManageTeam ? (
        <div className="card p-3 mb-4 admin-crud-card">
          <form className="row g-3 admin-crud-form-grid" onSubmit={handleSubmit}>
            <div className="col-12">
              <label className="form-label">Find ServiceNow CI</label>
              <input
                type="search"
                className="form-control"
                value={serviceNowSearch}
                onChange={(e) => {
                  setServiceNowSearch(e.target.value);
                  setSelectedServiceNowLabel('');
                  setServiceNowSysId('');
                  setServiceNowLookupComplete(false);
                  setServiceNowLookupError('');
                }}
                placeholder="Search by CI name, asset tag, or serial number"
              />
              <div className="form-text">
                Select the matching ServiceNow record. InciTeam keeps the ServiceNow link behind the scenes.
              </div>
              {serviceNowLookupLoading && <div className="lookup-status">Searching ServiceNow...</div>}
              {serviceNowLookupError && <div className="lookup-status lookup-status--error">{serviceNowLookupError}</div>}
              {!serviceNowLookupLoading
                && !serviceNowLookupError
                && serviceNowLookupComplete
                && !selectedServiceNowLabel
                && serviceNowResults.length === 0 && (
                  <div className="lookup-status lookup-status--warning">
                    No matching ServiceNow CI found. Try a fuller CI name from ServiceNow.
                  </div>
                )}
              {serviceNowResults.length > 0 && (
                <div className="lookup-results">
                  {serviceNowResults.map((result) => (
                    <button
                      key={result.sysId}
                      type="button"
                      className="lookup-result"
                      onClick={() => handleServiceNowCiSelect(result)}
                    >
                      <strong>{result.displayName}</strong>
                      <span>
                        {[result.detail, result.secondaryDetail].filter(Boolean).join(' / ') || 'Configuration item'}
                      </span>
                    </button>
                  ))}
                </div>
              )}
              {selectedServiceNowLabel && (
                <div className="linked-record-badge">Linked ServiceNow CI: {selectedServiceNowLabel}</div>
              )}
            </div>
            <div className="col-md-5">
              <label className="form-label">Name</label>
              <input
                type="text"
                className="form-control"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
            <div className="col-md-7">
              <label className="form-label">Description</label>
              <input
                type="text"
                className="form-control"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
            <div className="col-12 d-flex gap-2">
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update CI' : 'Add CI'}
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
                  <th>Name</th>
                  <th>Description</th>
                  <th>ServiceNow Link</th>
                  {canManageTeam && <th style={{ width: '180px' }}>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.ci_id}>
                    <td>{item.ci_id}</td>
                    <td>{item.name}</td>
                    <td>{item.description || '-'}</td>
                    <td>{item.serviceNowSysId ? 'Linked' : 'Needs link'}</td>
                    {canManageTeam && (
                      <td className="d-flex gap-2">
                        <button
                          className="btn btn-outline-primary btn-sm"
                          onClick={() => handleEdit(item)}
                        >
                          Update
                        </button>
                        <button
                          className="btn btn-outline-danger btn-sm"
                          onClick={() => handleDelete(item.ci_id)}
                        >
                          Delete
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
                {items.length === 0 && (
                  <tr>
                    <td colSpan={canManageTeam ? 5 : 4} className="text-center">
                      No configuration items yet.
                    </td>
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
