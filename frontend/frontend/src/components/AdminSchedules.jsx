import React, { useEffect, useState } from 'react';
import {
  createSchedule,
  deleteSchedule,
  fetchGeos,
  fetchSchedules,
  fetchShifts,
  fetchTeamMembers,
} from '../services/admin';

export default function AdminSchedules() {
  const [schedules, setSchedules] = useState([]);
  const [members, setMembers] = useState([]);
  const [geos, setGeos] = useState([]);
  const [shifts, setShifts] = useState([]);
  const [teamMemberId, setTeamMemberId] = useState('');
  const [geoId, setGeoId] = useState('');
  const [shiftId, setShiftId] = useState('');
  const [date, setDate] = useState('');
  const [error, setError] = useState('');

  const loadData = async () => {
    try {
      const [scheduleData, memberData, geoData, shiftData] = await Promise.all([
        fetchSchedules(),
        fetchTeamMembers(),
        fetchGeos(),
        fetchShifts(),
      ]);
      setSchedules(scheduleData);
      setMembers(memberData);
      setGeos(geoData);
      setShifts(shiftData);
    } catch (err) {
      setError('Failed to load schedules.');
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await createSchedule({ teamMemberId, geoId, shiftId, date });
      setTeamMemberId('');
      setGeoId('');
      setShiftId('');
      setDate('');
      loadData();
    } catch (err) {
      setError('Failed to create schedule.');
    }
  };

  const handleDelete = async (id) => {
    await deleteSchedule(id);
    loadData();
  };

  return (
    <div className="container">
      <h4 className="mb-3">Manage Shift Schedules</h4>
      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card p-3 mb-4">
        <form className="row g-3" onSubmit={handleSubmit}>
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
          <div className="col-md-3">
            <label className="form-label">Date</label>
            <input
              type="date"
              className="form-control"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              required
            />
          </div>
          <div className="col-12">
            <button type="submit" className="btn btn-primary">Add Schedule</button>
          </div>
        </form>
      </div>

      <div className="table-responsive">
        <table className="table table-bordered">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Team Member</th>
              <th>Geo</th>
              <th>Shift</th>
              <th>Date</th>
              <th style={{ width: '120px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {schedules.map((schedule) => (
              <tr key={schedule.tms_id}>
                <td>{schedule.tms_id}</td>
                <td>{schedule.teamMember?.f_name} {schedule.teamMember?.l_name}</td>
                <td>{schedule.geo?.name}</td>
                <td>{schedule.shift?.name}</td>
                <td>{schedule.date}</td>
                <td>
                  <button
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => handleDelete(schedule.tms_id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {schedules.length === 0 && (
              <tr>
                <td colSpan="6" className="text-center">No schedules yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
