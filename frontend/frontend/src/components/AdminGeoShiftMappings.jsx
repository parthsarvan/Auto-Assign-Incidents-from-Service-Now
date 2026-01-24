import React, { useEffect, useState } from 'react';
import {
  createGeoShiftMapping,
  deleteGeoShiftMapping,
  fetchGeoShiftMappings,
  fetchGeos,
  fetchShifts,
  updateGeoShiftMapping,
} from '../services/admin';
import { getCurrentUser } from '../services/auth';

export default function AdminGeoShiftMappings() {
  const [mappings, setMappings] = useState([]);
  const [geos, setGeos] = useState([]);
  const [shifts, setShifts] = useState([]);
  const [geoId, setGeoId] = useState('');
  const [shiftId, setShiftId] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const isAdmin = getCurrentUser()?.role === 'Admin';

  const loadData = async () => {
    try {
      const [geoData, shiftData, mappingData] = await Promise.all([
        fetchGeos(),
        fetchShifts(),
        fetchGeoShiftMappings(),
      ]);
      setGeos(geoData);
      setShifts(shiftData);
      setMappings(mappingData);
    } catch (err) {
      setError('Failed to load geo/shift mappings.');
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editingId) {
        if (!window.confirm('Update this geo/shift mapping?')) {
          return;
        }
        await updateGeoShiftMapping(editingId, { geoId, shiftId });
        setEditingId(null);
        setGeoId('');
        setShiftId('');
        loadData();
        return;
      }
      if (!window.confirm('Add this geo/shift mapping?')) {
        return;
      }
      await createGeoShiftMapping({ geoId, shiftId });
      setGeoId('');
      setShiftId('');
      loadData();
    } catch (err) {
      setError('Failed to create mapping.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this geo/shift mapping?')) {
      return;
    }
    await deleteGeoShiftMapping(id);
    loadData();
  };

  const handleEdit = (mapping) => {
    setEditingId(mapping.gsm_id);
    setGeoId(mapping.geo?.g_id || '');
    setShiftId(mapping.shift?.s_id || '');
  };

  const handleCancel = () => {
    setEditingId(null);
    setGeoId('');
    setShiftId('');
  };

  return (
    <div className="container">
      <h4 className="mb-3">Manage Geo-Shift Mappings</h4>
      {error && <div className="alert alert-danger">{error}</div>}

      {isAdmin ? (
        <div className="card p-3 mb-4">
          <form className="row g-3" onSubmit={handleSubmit}>
            <div className="col-md-4">
              <label className="form-label">Geo</label>
              <select
                className="form-select"
                value={geoId}
                onChange={(e) => setGeoId(e.target.value)}
                required
              >
                <option value="">Select Geo</option>
                {geos.map((geo) => (
                  <option key={geo.g_id} value={geo.g_id}>{geo.name}</option>
                ))}
              </select>
            </div>
            <div className="col-md-4">
              <label className="form-label">Shift</label>
              <select
                className="form-select"
                value={shiftId}
                onChange={(e) => setShiftId(e.target.value)}
                required
              >
                <option value="">Select Shift</option>
                {shifts.map((shift) => (
                  <option key={shift.s_id} value={shift.s_id}>{shift.name}</option>
                ))}
              </select>
            </div>
            <div className="col-12 d-flex gap-2">
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update Mapping' : 'Add Mapping'}
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
              <th>Geo</th>
              <th>Shift</th>
              {isAdmin && <th style={{ width: '180px' }}>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {mappings.map((mapping) => (
              <tr key={mapping.gsm_id}>
                <td>{mapping.gsm_id}</td>
                <td>{mapping.geo?.name}</td>
                <td>{mapping.shift?.name}</td>
                {isAdmin && (
                  <td className="d-flex gap-2">
                    <button
                      className="btn btn-outline-primary btn-sm"
                      onClick={() => handleEdit(mapping)}
                    >
                      Update
                    </button>
                    <button
                      className="btn btn-outline-danger btn-sm"
                      onClick={() => handleDelete(mapping.gsm_id)}
                    >
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {mappings.length === 0 && (
              <tr>
                <td colSpan={isAdmin ? 4 : 3} className="text-center">No mappings yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
