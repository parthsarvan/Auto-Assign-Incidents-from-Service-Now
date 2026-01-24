import React, { useEffect, useState } from 'react';
import { createShift, deleteShift, fetchShifts } from '../services/admin';

export default function AdminShifts() {
  const [shifts, setShifts] = useState([]);
  const [name, setName] = useState('');
  const [error, setError] = useState('');

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
      await createShift({ name });
      setName('');
      loadShifts();
    } catch (err) {
      setError('Failed to create shift.');
    }
  };

  const handleDelete = async (id) => {
    await deleteShift(id);
    loadShifts();
  };

  return (
    <div className="container">
      <h4 className="mb-3">Manage Shifts</h4>
      {error && <div className="alert alert-danger">{error}</div>}

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
          <div className="col-12">
            <button type="submit" className="btn btn-primary">Add Shift</button>
          </div>
        </form>
      </div>

      <div className="table-responsive">
        <table className="table table-bordered">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th style={{ width: '120px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {shifts.map((shift) => (
              <tr key={shift.s_id}>
                <td>{shift.s_id}</td>
                <td>{shift.name}</td>
                <td>
                  <button
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => handleDelete(shift.s_id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {shifts.length === 0 && (
              <tr>
                <td colSpan="3" className="text-center">No shifts yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
