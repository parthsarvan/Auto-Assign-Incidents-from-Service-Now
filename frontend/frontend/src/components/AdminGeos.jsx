import React, { useEffect, useState } from 'react';
import { createGeo, deleteGeo, fetchGeos } from '../services/admin';

export default function AdminGeos() {
  const [geos, setGeos] = useState([]);
  const [name, setName] = useState('');
  const [error, setError] = useState('');

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
      if (!window.confirm('Add this geo?')) {
        return;
      }
      await createGeo({ name });
      setName('');
      loadGeos();
    } catch (err) {
      setError('Failed to create geo.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this geo?')) {
      return;
    }
    await deleteGeo(id);
    loadGeos();
  };

  return (
    <div className="container">
      <h4 className="mb-3">Manage Geos</h4>
      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card p-3 mb-4">
        <form className="row g-3" onSubmit={handleSubmit}>
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
          <div className="col-12">
            <button type="submit" className="btn btn-primary">Add Geo</button>
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
            {geos.map((geo) => (
              <tr key={geo.g_id}>
                <td>{geo.g_id}</td>
                <td>{geo.name}</td>
                <td>
                  <button
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => handleDelete(geo.g_id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {geos.length === 0 && (
              <tr>
                <td colSpan="3" className="text-center">No geos yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
