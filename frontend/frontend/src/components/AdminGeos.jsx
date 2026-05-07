import React, { useEffect, useState } from 'react';
import { createGeo, deleteGeo, fetchGeos, updateGeo } from '../services/admin';
import { getCurrentUser } from '../services/auth';
import { canManageCurrentTeam } from '../services/permissions';
import SetupAssistBanner from './SetupAssistBanner';
import './AdminCrud.css';

export default function AdminGeos() {
  const [geos, setGeos] = useState([]);
  const [name, setName] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const canManageTeam = canManageCurrentTeam(getCurrentUser());

  const loadGeos = async () => {
    try {
      const data = await fetchGeos();
      setGeos(data);
    } catch (err) {
      setError('Failed to load geos.');
    }
  };

  useEffect(() => {
    loadGeos();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editingId) {
        if (!window.confirm('Update this geo?')) {
          return;
        }
        await updateGeo(editingId, { name });
        setEditingId(null);
        setName('');
        loadGeos();
        return;
      }
      if (!window.confirm('Add this geo?')) {
        return;
      }
      await createGeo({ name });
      setName('');
      loadGeos();
    } catch (err) {
      setError(typeof err?.response?.data === 'string' ? err.response.data : 'Failed to save geo.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this geo?')) {
      return;
    }
    await deleteGeo(id);
    loadGeos();
  };

  const handleEdit = (geo) => {
    setEditingId(geo.g_id);
    setName(geo.name);
  };

  const handleCancel = () => {
    setEditingId(null);
    setName('');
  };

  return (
    <div className="container admin-crud-page">
      <SetupAssistBanner
        title="Setup Step: Geos"
        helperText="Start by adding the geographic regions your team supports."
      />
      <div className="admin-crud-hero mb-4">
        <div className="admin-crud-hero__eyebrow">Team Geography</div>
        <h2 className="mb-1">Manage Geos</h2>
        <div className="text-muted">Define the supported geographic regions for the current team.</div>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}

      {canManageTeam ? (
        <div className="card p-3 mb-4 admin-crud-card">
          <form className="row g-3 admin-crud-form-grid" onSubmit={handleSubmit}>
            <div className="col-md-6">
              <label className="form-label">Geo Name</label>
              <input
                type="text"
                className="form-control"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
            <div className="col-12 d-flex gap-2">
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update Geo' : 'Add Geo'}
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
              <th>Name</th>
              {canManageTeam && <th style={{ width: '180px' }}>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {geos.map((geo) => (
              <tr key={geo.g_id}>
                <td>{geo.g_id}</td>
                <td>{geo.name}</td>
                {canManageTeam && (
                  <td className="d-flex gap-2">
                    <button
                      className="btn btn-outline-primary btn-sm"
                      onClick={() => handleEdit(geo)}
                    >
                      Update
                    </button>
                    <button
                      className="btn btn-outline-danger btn-sm"
                      onClick={() => handleDelete(geo.g_id)}
                    >
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {geos.length === 0 && (
              <tr>
                <td colSpan={canManageTeam ? 3 : 2} className="text-center">No geos yet.</td>
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
