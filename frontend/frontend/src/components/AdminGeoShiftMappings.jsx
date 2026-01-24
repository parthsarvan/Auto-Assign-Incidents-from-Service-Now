import React, { useEffect, useState } from 'react';
import {
  createGeoShiftMapping,
  deleteGeoShiftMapping,
  fetchGeoShiftMappings,
  fetchGeos,
  fetchShifts,
} from '../services/admin';

export default function AdminGeoShiftMappings() {
  const [mappings, setMappings] = useState([]);
  const [geos, setGeos] = useState([]);
  const [shifts, setShifts] = useState([]);
  const [geoId, setGeoId] = useState('');
  const [shiftId, setShiftId] = useState('');
  const [error, setError] = useState('');

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

  return (
    <div className="container">
      <h4 className="mb-3">Manage Geo-Shift Mappings</h4>
      {error && <div className="alert alert-danger">{error}</div>}

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
              <th>Geo</th>
              <th>Shift</th>
              <th style={{ width: '120px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {mappings.map((mapping) => (
              <tr key={mapping.gsm_id}>
                <td>{mapping.gsm_id}</td>
                <td>{mapping.geo?.name}</td>
                <td>{mapping.shift?.name}</td>
                <td>
                  <button
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => handleDelete(mapping.gsm_id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {mappings.length === 0 && (
              <tr>
                <td colSpan="4" className="text-center">No mappings yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
