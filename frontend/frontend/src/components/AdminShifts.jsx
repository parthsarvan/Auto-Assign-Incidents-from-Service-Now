import React, { useEffect, useState } from 'react';
import {
  createShift,
  deleteShift,
  fetchGeos,
  fetchGeoShiftMappings,
  fetchShifts,
  updateShift,
} from '../services/admin';
import { getCurrentUser } from '../services/auth';
import { canManageCurrentTeam } from '../services/permissions';
import { getTimeZoneOptions } from '../services/timezones';
import { updateCurrentTeamTimezone } from '../services/workspace';
import SetupAssistBanner from './SetupAssistBanner';
import './AdminCrud.css';

export default function AdminShifts() {
  const [geos, setGeos] = useState([]);
  const [geoShiftMappings, setGeoShiftMappings] = useState([]);
  const [shifts, setShifts] = useState([]);
  const [name, setName] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [selectedGeoIds, setSelectedGeoIds] = useState([]);
  const [teamTimezone, setTeamTimezone] = useState(getCurrentUser()?.workspace?.teamTimezone || '');
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const [timezoneSaving, setTimezoneSaving] = useState(false);
  const canManageTeam = canManageCurrentTeam(getCurrentUser());
  const timezoneOptions = getTimeZoneOptions();

  const loadShiftData = async () => {
    try {
      const [shiftData, geoData, geoShiftData] = await Promise.all([
        fetchShifts(),
        fetchGeos(),
        fetchGeoShiftMappings(),
      ]);
      setShifts(shiftData);
      setGeos(geoData);
      setGeoShiftMappings(geoShiftData);
    } catch (err) {
      setError('Failed to load shifts.');
    }
  };

  useEffect(() => {
    loadShiftData();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editingId) {
        if (!window.confirm('Update this shift?')) {
          return;
        }
        await updateShift(editingId, { name, startTime, endTime, geoIds: selectedGeoIds.map(Number) });
        setEditingId(null);
        setName('');
        setStartTime('');
        setEndTime('');
        setSelectedGeoIds([]);
        loadShiftData();
        return;
      }
      if (!window.confirm('Add this shift?')) {
        return;
      }
      await createShift({ name, startTime, endTime, geoIds: selectedGeoIds.map(Number) });
      setName('');
      setStartTime('');
      setEndTime('');
      setSelectedGeoIds([]);
      loadShiftData();
    } catch (err) {
      setError(typeof err?.response?.data === 'string' ? err.response.data : 'Failed to save shift.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this shift?')) {
      return;
    }
    await deleteShift(id);
    loadShiftData();
  };

  const handleEdit = (shift) => {
    setEditingId(shift.s_id);
    setName(shift.name);
    setStartTime(shift.startTime || '');
    setEndTime(shift.endTime || '');
    setSelectedGeoIds(
      geoShiftMappings
        .filter((mapping) => mapping.shift?.s_id === shift.s_id && mapping.geo?.g_id)
        .map((mapping) => String(mapping.geo.g_id))
    );
  };

  const handleCancel = () => {
    setEditingId(null);
    setName('');
    setStartTime('');
    setEndTime('');
    setSelectedGeoIds([]);
  };

  const handleTimezoneSave = async (e) => {
    e.preventDefault();
    setError('');
    setTimezoneSaving(true);
    try {
      await updateCurrentTeamTimezone(teamTimezone);
    } catch (err) {
      setError(typeof err?.response?.data === 'string' ? err.response.data : 'Failed to save team timezone.');
    } finally {
      setTimezoneSaving(false);
    }
  };

  const getShiftGeoNames = (shiftId) => {
    const names = geoShiftMappings
      .filter((mapping) => mapping.shift?.s_id === shiftId)
      .map((mapping) => mapping.geo?.name)
      .filter(Boolean);
    return names.length > 0 ? names.join(', ') : '—';
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
        <>
        <div className="card p-3 mb-4 admin-crud-card">
          <form className="row g-3 admin-crud-form-grid" onSubmit={handleTimezoneSave}>
            <div className="col-md-8">
              <label className="form-label">Team Timezone</label>
              <select
                className="form-select"
                value={teamTimezone}
                onChange={(e) => setTeamTimezone(e.target.value)}
                required
              >
                <option value="">Select a timezone</option>
                {timezoneOptions.map((timezone) => (
                  <option key={timezone} value={timezone}>
                    {timezone}
                  </option>
                ))}
              </select>
              <div className="form-text">All shifts for this team will use this timezone.</div>
            </div>
            <div className="col-12 d-flex gap-2">
              <button type="submit" className="btn btn-outline-primary" disabled={timezoneSaving}>
                {timezoneSaving ? 'Saving Timezone...' : 'Save Team Timezone'}
              </button>
            </div>
          </form>
        </div>

        <div className="card p-3 mb-4 admin-crud-card">
          <form className="row g-3 admin-crud-form-grid" onSubmit={handleSubmit}>
            <div className="col-md-4">
              <label className="form-label">Shift Name</label>
              <input
                type="text"
                className="form-control"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Start Time</label>
              <input
                type="time"
                className="form-control"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                required
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">End Time</label>
              <input
                type="time"
                className="form-control"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                required
              />
            </div>
            <div className="col-12">
              <label className="form-label">Applies To Geos</label>
              {geos.length > 0 ? (
                <div className="d-flex flex-wrap gap-3">
                  {geos.map((geo) => {
                    const checked = selectedGeoIds.includes(String(geo.g_id));
                    return (
                      <div className="form-check" key={geo.g_id}>
                        <input
                          className="form-check-input"
                          type="checkbox"
                          id={`admin-shift-geo-${geo.g_id}`}
                          checked={checked}
                          onChange={(e) => {
                            const value = String(geo.g_id);
                            setSelectedGeoIds((current) =>
                              e.target.checked
                                ? [...current, value]
                                : current.filter((geoId) => geoId !== value)
                            );
                          }}
                        />
                        <label className="form-check-label" htmlFor={`admin-shift-geo-${geo.g_id}`}>
                          {geo.name}
                        </label>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="form-text text-danger">Add geos first, then create shifts.</div>
              )}
              <div className="form-text">
                Choose every geo that should use this shift. InciTeam will maintain the geo-shift mappings for you.
              </div>
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
        </>
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
              <th>Start</th>
              <th>End</th>
              <th>Geos</th>
              <th>Timezone</th>
              {canManageTeam && <th style={{ width: '180px' }}>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {shifts.map((shift) => (
              <tr key={shift.s_id}>
                <td>{shift.s_id}</td>
                <td>{shift.name}</td>
                <td>{shift.startTime || '—'}</td>
                <td>{shift.endTime || '—'}</td>
                <td>{getShiftGeoNames(shift.s_id)}</td>
                <td>{teamTimezone || getCurrentUser()?.workspace?.teamTimezone || '—'}</td>
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
                <td colSpan={canManageTeam ? 7 : 6} className="text-center">No shifts yet.</td>
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
