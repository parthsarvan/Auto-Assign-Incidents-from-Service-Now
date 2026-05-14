import React, { useEffect, useMemo, useState } from 'react';
import { DateTime } from 'luxon';
import {
  createSchedule,
  deleteSchedule,
  fetchGeos,
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
  const [geos, setGeos] = useState([]);
  const [teamMemberIds, setTeamMemberIds] = useState([]);
  const [coveragePreset, setCoveragePreset] = useState('EVERY_DAY');
  const [coverageDays, setCoverageDays] = useState([
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
    'SUNDAY',
  ]);
  const selectedGeo = geos.find((geo) => String(geo.g_id) === String(geoId));
  const selectedGeoName = selectedGeo?.name || '';
  const filteredMembers = useMemo(() => (
    geoId
      ? members.filter((member) => String(member.geo?.g_id) === String(geoId))
      : []
  ), [geoId, members]);
  const uniqueAllowedShiftIds = useMemo(() => {
    const allowedShiftIdsForGeo = geoShiftMappings
      .filter((mapping) => String(mapping.geo?.g_id) === String(geoId))
      .map((mapping) => String(mapping.shift?.s_id));
    return Array.from(new Set(allowedShiftIdsForGeo));
  }, [geoId, geoShiftMappings]);
  const allowedShifts = useMemo(() => (
    shifts.filter((shift) => uniqueAllowedShiftIds.includes(String(shift.s_id)))
  ), [shifts, uniqueAllowedShiftIds]);
  const hasSingleMappedShift = uniqueAllowedShiftIds.length === 1;
  const hasMultipleMappedShifts = uniqueAllowedShiftIds.length > 1;
  const autoSelectedShiftName = allowedShifts[0]?.name || '';

  const loadData = async () => {
    try {
      const [scheduleData, memberData, shiftData, geoShiftData, geoData] = await Promise.all([
        fetchSchedules(),
        fetchTeamMembers(),
        fetchShifts(),
        fetchGeoShiftMappings(),
        fetchGeos(),
      ]);
      setSchedules(scheduleData);
      setMembers(memberData);
      setShifts(shiftData);
      setGeoShiftMappings(geoShiftData);
      setGeos(geoData);
    } catch (err) {
      setError('Failed to load schedules.');
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    if (!geoId) {
      if (shiftId !== '') {
        setShiftId('');
      }
      if (teamMemberIds.length > 0) {
        setTeamMemberIds([]);
      }
      return;
    }

    const filteredMemberIds = new Set(filteredMembers.map((member) => String(member.tm_id)));
    const validSelectedMemberIds = teamMemberIds.filter((id) => filteredMemberIds.has(String(id)));
    if (validSelectedMemberIds.length !== teamMemberIds.length) {
      setTeamMemberIds(validSelectedMemberIds);
    }

    if (uniqueAllowedShiftIds.length === 1) {
      if (shiftId !== uniqueAllowedShiftIds[0]) {
        setShiftId(uniqueAllowedShiftIds[0]);
      }
    } else if (uniqueAllowedShiftIds.length > 1) {
      if (!uniqueAllowedShiftIds.includes(String(shiftId))) {
        setShiftId('');
      }
    } else if (shiftId !== '') {
      setShiftId('');
    }
  }, [filteredMembers, geoId, shiftId, teamMemberIds, uniqueAllowedShiftIds]);

  const resetForm = () => {
    setEditingId(null);
    setTeamMemberId('');
    setTeamMemberIds([]);
    setGeoId('');
    setShiftId('');
    setStartDate('');
    setEndDate('');
    setCoveragePreset('EVERY_DAY');
    setCoverageDays([
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
      'THURSDAY',
      'FRIDAY',
      'SATURDAY',
      'SUNDAY',
    ]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editingId) {
        if (!window.confirm('Update this schedule?')) {
          return;
        }
        if (!teamMemberId) {
          setError('Please select a team member before saving.');
          return;
        }
        if (!geoId) {
          setError('Please select a geo before saving.');
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
        await updateSchedule(editingId, { teamMemberId, geoId, shiftId, startDate, endDate, coverageDays });
        resetForm();
        loadData();
        return;
      }
      if (!window.confirm('Add this schedule?')) {
        return;
      }
      if (teamMemberIds.length === 0) {
        setError('Please select at least one team member before saving.');
        return;
      }
      if (!geoId) {
        setError('Please select a geo before saving.');
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
      if (coverageDays.length === 0) {
        setError('Select at least one coverage day.');
        return;
      }
      await createSchedule({ teamMemberIds, geoId, shiftId, startDate, endDate, coverageDays });
      resetForm();
      loadData();
    } catch (err) {
      setError(typeof err?.response?.data === 'string' ? err.response.data : 'Failed to save schedule.');
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
    setTeamMemberIds(schedule.teamMember?.tm_id ? [String(schedule.teamMember.tm_id)] : []);
    setGeoId(schedule.geo?.g_id || '');
    setShiftId(schedule.shift?.s_id || '');
    setStartDate(schedule.startDate || '');
    setEndDate(schedule.endDate || '');
    const scheduleCoverageDays = parseCoverageDays(schedule.coverageDays);
    setCoverageDays(scheduleCoverageDays);
    setCoveragePreset(resolveCoveragePreset(scheduleCoverageDays));
  };

  const handleCancel = () => {
    resetForm();
  };

  const coveragePresets = {
    EVERY_DAY: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'],
    WEEKDAYS: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
    SATURDAY: ['SATURDAY'],
    SUNDAY: ['SUNDAY'],
    WEEKEND: ['SATURDAY', 'SUNDAY'],
  };
  const dayLabels = [
    ['MONDAY', 'Mon'],
    ['TUESDAY', 'Tue'],
    ['WEDNESDAY', 'Wed'],
    ['THURSDAY', 'Thu'],
    ['FRIDAY', 'Fri'],
    ['SATURDAY', 'Sat'],
    ['SUNDAY', 'Sun'],
  ];

  function parseCoverageDays(value) {
    if (!value || typeof value !== 'string') {
      return coveragePresets.EVERY_DAY;
    }
    return value.split(',').map((day) => day.trim()).filter(Boolean);
  }

  function resolveCoveragePreset(days) {
    const normalized = [...days].sort().join(',');
    const match = Object.entries(coveragePresets).find(([, presetDays]) =>
      [...presetDays].sort().join(',') === normalized
    );
    return match?.[0] || 'CUSTOM';
  }

  function formatCoverageDays(value) {
    const days = parseCoverageDays(value);
    const preset = resolveCoveragePreset(days);
    if (preset === 'EVERY_DAY') return 'Every day';
    if (preset === 'WEEKDAYS') return 'Weekdays';
    if (preset === 'WEEKEND') return 'Weekend';
    if (preset === 'SATURDAY') return 'Saturday';
    if (preset === 'SUNDAY') return 'Sunday';
    return days
      .map((day) => dayLabels.find(([value]) => value === day)?.[1] || day)
      .join(', ');
  }

  const handleCoveragePresetChange = (preset) => {
    setCoveragePreset(preset);
    if (preset !== 'CUSTOM') {
      setCoverageDays(coveragePresets[preset]);
    }
  };

  const toggleCoverageDay = (day) => {
    const nextDays = coverageDays.includes(day)
      ? coverageDays.filter((value) => value !== day)
      : [...coverageDays, day];
    setCoverageDays(nextDays);
    setCoveragePreset(resolveCoveragePreset(nextDays));
  };

  const handleMemberSelectionChange = (event) => {
    const selectedIds = Array.from(event.target.selectedOptions).map((option) => option.value);
    setTeamMemberIds(selectedIds);
    setTeamMemberId(selectedIds[0] || '');
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
          <div className="col-md-3">
            <label className="form-label">{editingId ? 'Team Member' : 'Team Members'}</label>
            {!geoId ? (
              <input
                type="text"
                className="form-control"
                value=""
                placeholder="Select geo first"
                readOnly
              />
            ) : editingId ? (
              <select
                className="form-select"
                value={teamMemberId}
                onChange={(e) => {
                  setTeamMemberId(e.target.value);
                  setTeamMemberIds(e.target.value ? [e.target.value] : []);
                }}
                required
              >
                <option value="">Select Member</option>
                {filteredMembers.map((member) => (
                  <option key={member.tm_id} value={member.tm_id}>
                    {member.f_name} {member.l_name}
                  </option>
                ))}
              </select>
            ) : (
              <>
                <select
                  className="form-select"
                  value={teamMemberIds}
                  onChange={handleMemberSelectionChange}
                  multiple
                  size={Math.min(Math.max(filteredMembers.length, 3), 6)}
                  required
                >
                  {filteredMembers.map((member) => (
                    <option key={member.tm_id} value={member.tm_id}>
                      {member.f_name} {member.l_name}
                    </option>
                  ))}
                </select>
                <div className="form-text">
                  Select one or more {selectedGeoName} team members.
                </div>
              </>
            )}
          </div>
          <div className="col-md-3">
            <label className="form-label">{hasMultipleMappedShifts ? 'Shift' : 'Mapped Shift'}</label>
            {!geoId ? (
              <input
                type="text"
                className="form-control"
                value=""
                placeholder="Select geo first"
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
          <div className="col-md-4">
            <label className="form-label">Repeat</label>
            <select
              className="form-select"
              value={coveragePreset}
              onChange={(e) => handleCoveragePresetChange(e.target.value)}
            >
              <option value="EVERY_DAY">Every day</option>
              <option value="WEEKDAYS">Weekdays (Mon-Fri)</option>
              <option value="WEEKEND">Weekend (Sat-Sun)</option>
              <option value="SATURDAY">Saturday only</option>
              <option value="SUNDAY">Sunday only</option>
              <option value="CUSTOM">Custom days</option>
            </select>
          </div>
          <div className="col-md-8">
            <label className="form-label">Coverage Days</label>
            <div className="admin-crud-day-picker">
              {dayLabels.map(([day, label]) => (
                <label
                  className={`admin-crud-day-pill ${coverageDays.includes(day) ? 'admin-crud-day-pill--selected' : ''}`}
                  key={day}
                >
                  <input
                    type="checkbox"
                    checked={coverageDays.includes(day)}
                    onChange={() => toggleCoverageDay(day)}
                  />
                  {label}
                </label>
              ))}
            </div>
            <div className="form-text">
              Use presets for weekdays/weekends, or pick exact days like Saturday only or Sunday only.
            </div>
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
              <th>Coverage Days</th>
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
                <td>{formatCoverageDays(schedule.coverageDays)}</td>
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
                <td colSpan={canManageTeam ? 9 : 8} className="text-center">No schedules yet.</td>
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
