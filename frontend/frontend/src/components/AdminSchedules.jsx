import React, { useEffect, useState } from 'react';
import { DateTime } from 'luxon';
import {
  createSchedule,
  deleteSchedule,
  fetchGeoShiftMappings,
  fetchSchedules,
  fetchShifts,
  fetchTeamMembers,
  updateSchedule,
} from '../services/admin';
import { getCurrentUser } from '../services/auth';
import { canManageCurrentTeam } from '../services/permissions';
import CurrentRoutingWindow from './CurrentRoutingWindow';
import SetupAssistBanner from './SetupAssistBanner';
import './AdminCrud.css';

export default function AdminSchedules() {
  const [schedules, setSchedules] = useState([]);
  const [members, setMembers] = useState([]);
  const [shifts, setShifts] = useState([]);
  const [geoShiftMappings, setGeoShiftMappings] = useState([]);
  const [teamMemberId, setTeamMemberId] = useState('');
  const [geoId, setGeoId] = useState('');
  const [shiftId, setShiftId] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const canManageTeam = canManageCurrentTeam(getCurrentUser());
  const selectedMember = members.find((member) => String(member.tm_id) === String(teamMemberId));
  const selectedGeoName = selectedMember?.geo?.name || '';
  const allowedShiftIdsForGeo = geoShiftMappings
    .filter((mapping) => String(mapping.geo?.g_id) === String(geoId))
    .map((mapping) => String(mapping.shift?.s_id));
  const uniqueAllowedShiftIds = Array.from(new Set(allowedShiftIdsForGeo));
  const allowedShifts = shifts.filter((shift) =>
    uniqueAllowedShiftIds.includes(String(shift.s_id))
  );
  const hasSingleMappedShift = uniqueAllowedShiftIds.length === 1;
  const hasMultipleMappedShifts = uniqueAllowedShiftIds.length > 1;
  const autoSelectedShiftName = allowedShifts[0]?.name || '';

  const loadData = async () => {
    try {
      const [scheduleData, memberData, shiftData, geoShiftData] = await Promise.all([
        fetchSchedules(),
        fetchTeamMembers(),
        fetchShifts(),
        fetchGeoShiftMappings(),
      ]);
      setSchedules(scheduleData);
      setMembers(memberData);
      setShifts(shiftData);
      setGeoShiftMappings(geoShiftData);
    } catch (err) {
      setError('Failed to load schedules.');
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    const member = members.find((m) => String(m.tm_id) === String(teamMemberId));
    const memberGeoId = member?.geo?.g_id ? String(member.geo.g_id) : '';
    if (geoId !== memberGeoId) {
      setGeoId(memberGeoId);
    }

    if (!memberGeoId) {
      if (shiftId !== '') {
        setShiftId('');
      }
      return;
    }

    const allowedShiftIds = geoShiftMappings
      .filter((mapping) => String(mapping.geo?.g_id) === String(memberGeoId))
      .map((mapping) => String(mapping.shift?.s_id));

    const uniqueShiftIds = Array.from(new Set(allowedShiftIds));
    if (uniqueShiftIds.length === 1) {
      if (shiftId !== uniqueShiftIds[0]) {
        setShiftId(uniqueShiftIds[0]);
      }
    } else if (uniqueShiftIds.length > 1) {
      if (!uniqueShiftIds.includes(String(shiftId))) {
        setShiftId('');
      }
    } else if (shiftId !== '') {
      setShiftId('');
    }
  }, [teamMemberId, members, geoShiftMappings, shiftId, geoId]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editingId) {
        if (!window.confirm('Update this schedule?')) {
          return;
        }
        if (!geoId) {
          setError('Selected team member does not have a geo assigned.');
          return;
        }
        if (uniqueAllowedShiftIds.length === 0) {
          setError('This geo does not have any mapped shifts yet. Add a geo-shift mapping first.');
          return;
        }
        if (!shiftId) {
          setError('Please select a shift before saving.');
          return;
        }
        await updateSchedule(editingId, { teamMemberId, geoId, shiftId, startDate, endDate });
        setEditingId(null);
        setTeamMemberId('');
        setGeoId('');
        setShiftId('');
        setStartDate('');
        setEndDate('');
        loadData();
        return;
      }
      if (!window.confirm('Add this schedule?')) {
        return;
      }
      if (!geoId) {
        setError('Selected team member does not have a geo assigned.');
        return;
      }
      if (uniqueAllowedShiftIds.length === 0) {
        setError('This geo does not have any mapped shifts yet. Add a geo-shift mapping first.');
        return;
      }
      if (!shiftId) {
        setError('Please select a shift before saving.');
        return;
      }
      await createSchedule({ teamMemberId, geoId, shiftId, startDate, endDate });
      setTeamMemberId('');
      setGeoId('');
      setShiftId('');
      setStartDate('');
      setEndDate('');
      loadData();
    } catch (err) {
      setError('Failed to create schedule.');
    }
  };

  const formatDurationDays = (start, end) => {
    if (!start || !end) return '-';
    const startDt = DateTime.fromISO(start);
    const endDt = DateTime.fromISO(end);
    const diffDays = Math.floor(endDt.diff(startDt, 'days').days) + 1;
    return diffDays > 0 ? `${diffDays} day${diffDays > 1 ? 's' : ''}` : '-';
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this schedule?')) {
      return;
    }
    await deleteSchedule(id);
    loadData();
  };

  const handleEdit = (schedule) => {
    setEditingId(schedule.tms_id);
    setTeamMemberId(schedule.teamMember?.tm_id || '');
    setGeoId(schedule.geo?.g_id || '');
    setShiftId(schedule.shift?.s_id || '');
    setStartDate(schedule.startDate || '');
    setEndDate(schedule.endDate || '');
  };

  const handleCancel = () => {
    setEditingId(null);
    setTeamMemberId('');
    setGeoId('');
    setShiftId('');
    setStartDate('');
    setEndDate('');
  };

  return (
    <div className="container admin-crud-page">
      <SetupAssistBanner
        title="Setup Step: Schedules"
        helperText="Create the initial shift schedule so InciTeam can pick the right assignee."
      />
      <div className="admin-crud-hero mb-4">
        <div className="admin-crud-hero__eyebrow">Rotation Planning</div>
        <h2 className="mb-1">Manage Shift Schedules</h2>
        <div className="text-muted">Define who covers each mapped shift window for the current team.</div>
      </div>
      <CurrentRoutingWindow />
      {error && <div className="alert alert-danger">{error}</div>}

      {canManageTeam ? (
        <div className="card p-3 mb-4 admin-crud-card">
          <form className="row g-3 admin-crud-form-grid" onSubmit={handleSubmit}>
          <div className="col-md-3">
            <label className="form-label">Team Member</label>
            <select
              className="form-select"
              value={teamMemberId}
              onChange={(e) => setTeamMemberId(e.target.value)}
              required
            >
              <option value="">Select Member</option>
              {members.map((member) => (
                <option key={member.tm_id} value={member.tm_id}>
                  {member.f_name} {member.l_name}
                </option>
              ))}
            </select>
          </div>
          <div className="col-md-3">
            <label className="form-label">Geo</label>
            <input
              type="text"
              className="form-control"
              value={selectedGeoName}
              readOnly
            />
          </div>
          <div className="col-md-3">
            <label className="form-label">{hasMultipleMappedShifts ? 'Shift' : 'Mapped Shift'}</label>
            {!teamMemberId ? (
              <input
                type="text"
                className="form-control"
                value=""
                placeholder="Select a team member first"
                readOnly
              />
            ) : uniqueAllowedShiftIds.length === 0 ? (
              <>
                <input
                  type="text"
                  className="form-control"
                  value=""
                  placeholder="No mapped shift available"
                  readOnly
                />
                <div className="form-text text-danger">
                  Add a geo-shift mapping for {selectedGeoName || 'this geo'} before creating a schedule.
                </div>
              </>
            ) : hasSingleMappedShift ? (
              <>
                <input
                  type="text"
                  className="form-control"
                  value={autoSelectedShiftName}
                  readOnly
                />
                <div className="form-text">
                  This geo supports one shift, so InciTeam selected it automatically.
                </div>
              </>
            ) : (
              <>
                <select
                  className="form-select"
                  value={shiftId}
                  onChange={(e) => setShiftId(e.target.value)}
                  required
                >
                  <option value="">Select Shift</option>
                  {allowedShifts.map((shift) => (
                    <option key={shift.s_id} value={shift.s_id}>{shift.name}</option>
                  ))}
                </select>
                <div className="form-text">
                  Multiple shifts are mapped to {selectedGeoName || 'this geo'}, so choose the one to schedule.
                </div>
              </>
            )}
          </div>
          <div className="col-md-3">
            <label className="form-label">Start Date</label>
            <input
              type="date"
              className="form-control"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              required
            />
          </div>
          <div className="col-md-3">
            <label className="form-label">End Date</label>
            <input
              type="date"
              className="form-control"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              required
            />
          </div>
            <div className="col-12 d-flex gap-2">
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update Schedule' : 'Add Schedule'}
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
              <th>Team Member</th>
              <th>Geo</th>
              <th>Shift</th>
              <th>Start Date</th>
              <th>End Date</th>
              <th>Duration</th>
              {canManageTeam && <th style={{ width: '180px' }}>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {schedules.map((schedule) => (
              <tr key={schedule.tms_id}>
                <td>{schedule.tms_id}</td>
                <td>{schedule.teamMember?.f_name} {schedule.teamMember?.l_name}</td>
                <td>{schedule.geo?.name}</td>
                <td>{schedule.shift?.name}</td>
                <td>{schedule.startDate}</td>
                <td>{schedule.endDate}</td>
                <td>{formatDurationDays(schedule.startDate, schedule.endDate)}</td>
                {canManageTeam && (
                  <td className="d-flex gap-2">
                    <button
                      className="btn btn-outline-primary btn-sm"
                      onClick={() => handleEdit(schedule)}
                    >
                      Update
                    </button>
                    <button
                      className="btn btn-outline-danger btn-sm"
                      onClick={() => handleDelete(schedule.tms_id)}
                    >
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {schedules.length === 0 && (
              <tr>
                <td colSpan={canManageTeam ? 8 : 7} className="text-center">No schedules yet.</td>
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
