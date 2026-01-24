import React, { useEffect, useState } from 'react';
import { createShift, deleteShift, fetchShifts, updateShift } from '../services/admin';
import { getCurrentUser } from '../services/auth';

export default function AdminShifts() {
  const [shifts, setShifts] = useState([]);
  const [name, setName] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const isAdmin = getCurrentUser()?.role === 'Admin';

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
    <div className="container">
      <h4 className="mb-3">Manage Shifts</h4>
      {error && <div className="alert alert-danger">{error}</div>}

      {isAdmin ? (
        <div className="card p-3 mb-4">
          <form className="row g-3" onSubmit={handleSubmit}>
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

      <div className="table-responsive">
        <table className="table table-bordered">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Name</th>
              {isAdmin && <th style={{ width: '180px' }}>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {shifts.map((shift) => (
              <tr key={shift.s_id}>
                <td>{shift.s_id}</td>
                <td>{shift.name}</td>
                {isAdmin && (
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
                <td colSpan={isAdmin ? 3 : 2} className="text-center">No shifts yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
