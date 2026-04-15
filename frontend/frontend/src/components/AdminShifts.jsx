import React, { useEffect, useState } from 'react';
import { createShift, deleteShift, fetchShifts, updateShift } from '../services/admin';
import { getCurrentUser } from '../services/auth';
import { canManageCurrentTeam } from '../services/permissions';
import SetupAssistBanner from './SetupAssistBanner';
import './AdminCrud.css';

export default function AdminShifts() {
  const [shifts, setShifts] = useState([]);
  const [name, setName] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const canManageTeam = canManageCurrentTeam(getCurrentUser());

  const loadShifts = async () => {
    try {
      const data = await fetchShifts();
      setShifts(data);
    } catch (err) {
      setError('Failed to load shifts.');
    }
  };

  useEffect(() => {
    loadShifts();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editingId) {
        if (!window.confirm('Update this shift?')) {
          return;
        }
        await updateShift(editingId, { name });
        setEditingId(null);
        setName('');
        loadShifts();
        return;
      }
      if (!window.confirm('Add this shift?')) {
        return;
      }
      await createShift({ name });
      setName('');
      loadShifts();
    } catch (err) {
      setError('Failed to create shift.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this shift?')) {
      return;
    }
    await deleteShift(id);
    loadShifts();
  };

  const handleEdit = (shift) => {
    setEditingId(shift.s_id);
    setName(shift.name);
  };

  const handleCancel = () => {
    setEditingId(null);
    setName('');
  };

  return (
    <div className="container admin-crud-page">
      <SetupAssistBanner
        title="Setup Step: Shifts"
        helperText="Define the shifts your team uses across supported geos."
      />
      <div className="admin-crud-hero mb-4">
        <div className="admin-crud-hero__eyebrow">Coverage Windows</div>
        <h2 className="mb-1">Manage Shifts</h2>
        <div className="text-muted">Create the shift patterns that define how the team covers incidents.</div>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}

      {canManageTeam ? (
        <div className="card p-3 mb-4 admin-crud-card">
          <form className="row g-3 admin-crud-form-grid" onSubmit={handleSubmit}>
            <div className="col-md-6">
              <label className="form-label">Shift Name</label>
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
                {editingId ? 'Update Shift' : 'Add Shift'}
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
            {shifts.map((shift) => (
              <tr key={shift.s_id}>
                <td>{shift.s_id}</td>
                <td>{shift.name}</td>
                {canManageTeam && (
                  <td className="d-flex gap-2">
                    <button
                      className="btn btn-outline-primary btn-sm"
                      onClick={() => handleEdit(shift)}
                    >
                      Update
                    </button>
                    <button
                      className="btn btn-outline-danger btn-sm"
                      onClick={() => handleDelete(shift.s_id)}
                    >
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {shifts.length === 0 && (
              <tr>
                <td colSpan={canManageTeam ? 3 : 2} className="text-center">No shifts yet.</td>
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
