// src/components/Dashboard.jsx
import React, { useEffect, useState } from 'react';
import { DateTime } from 'luxon';
import axios from 'axios';

import AvailabilityGrid from './AvailabilityGrid';
import LeaveList from './LeaveList';
import BreakList from './BreakList';

export default function Dashboard() {
  // ── 1) Timezone & View State ─────────────────────────────────────────────
  // The user’s browser timezone (e.g. "America/Chicago")
  const zone = DateTime.local().zoneName;

  // “week” or “day”
  const [viewMode, setViewMode] = useState('week');

  // The ISO string for the first date in the window (e.g. "2025-06-03")
  const [startDate, setStartDate] = useState(
    DateTime.now().setZone(zone).toISODate()
  );

  // Number of days to show: 7 if weekly, 1 if daily
  const dayCount = viewMode === 'week' ? 7 : 1;

  // ── 2) Fetched Data & Loading State ───────────────────────────────────────
  // Flat availability: [ { tmId, fullName, geoName, shiftName, date }, … ]
  const [availabilityRecords, setAvailabilityRecords] = useState([]);

  // Flat leaves: [ { fullName, geoName, shiftName, date, startTs, endTs, reason }, … ]
  const [leaveRecords, setLeaveRecords] = useState([]);

  // Flat breaks: [ { fullName, geoName, shiftName, date, startTs, endTs, reason }, … ]
  const [breakRecords, setBreakRecords] = useState([]);

  // A Set of composite keys "geo–shift||YYYY-MM-DD||fullName" for anyone on leave
  const [onLeaveSet, setOnLeaveSet] = useState(new Set());

  // A Set of composite keys "geo–shift||YYYY-MM-DD||fullName" for anyone on break
  const [onBreakSet, setOnBreakSet] = useState(new Set());

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // ── 3) Fetch Availability & Leave Whenever startDate or viewMode Changes ──
  useEffect(() => {
    async function loadAllData() {
      setLoading(true);
      setError('');

      try {
        const token = sessionStorage.getItem('token');
        if (!token) {
          setError('No token found; please sign in again.');
          setLoading(false);
          return;
        }

        // 3a) Fetch “on‐shift availability” from the backend
        //    (this returns flat objects: { tmId, fullName, geoName, shiftName, date })
        const availResp = await axios.get(
          'http://localhost:8080/api/schedule/next',
          {
            params: { startDate, days: dayCount },
            headers: { Authorization: `Bearer ${token}` },
          }
        );
        setAvailabilityRecords(availResp.data);

        // 3b) Fetch “on‐leave” records from the backend
        //    (this returns flat objects: { fullName, geoName, shiftName, startTs, endTs, reason })
        const leaveResp = await axios.get(
          'http://localhost:8080/api/leave/next',
          {
            params: { startDate, days: dayCount },
            headers: { Authorization: `Bearer ${token}` },
          }
        );

        // Convert each leave to include an explicit “date” field based on its UTC startTs
        const leaveFlat = leaveResp.data.map((rec) => {
          // Parse as UTC, then extract just the date portion (YYYY-MM-DD)
          const utcDate = DateTime.fromISO(rec.startTs, { zone: 'utc' }).toISODate();
          return {
            fullName:  rec.fullName,
            geoName:   rec.geoName,
            shiftName: rec.shiftName,
            date:      utcDate,
            startTs:   rec.startTs,
            endTs:     rec.endTs,
            reason:    rec.reason,
          };
        });
        setLeaveRecords(leaveFlat);

        // 3c) Build the Set of keys: "geo–shift||YYYY-MM-DD||fullName"
        const leaveKeySet = new Set();
        leaveFlat.forEach((r) => {
          const key = `${r.geoName}–${r.shiftName}||${r.date}||${r.fullName}`;
          leaveKeySet.add(key);
        });
        setOnLeaveSet(leaveKeySet);

        // 3d) Fetch “on‐break” records from the backend
        const breakResp = await axios.get(
          'http://localhost:8080/api/break/next',
          {
            params: { startDate, days: dayCount },
            headers: { Authorization: `Bearer ${token}` },
          }
        );

        const breakFlat = breakResp.data.map((rec) => {
          const utcDate = DateTime.fromISO(rec.startTs, { zone: 'utc' }).toISODate();
          return {
            fullName: rec.fullName,
            geoName: rec.geoName,
            shiftName: rec.shiftName,
            date: utcDate,
            startTs: rec.startTs,
            endTs: rec.endTs,
            reason: rec.reason,
          };
        });
        setBreakRecords(breakFlat);

        const breakKeySet = new Set();
        breakFlat.forEach((r) => {
          const key = `${r.geoName}–${r.shiftName}||${r.date}||${r.fullName}`;
          breakKeySet.add(key);
        });
        setOnBreakSet(breakKeySet);
      } catch (err) {
        console.error('Error loading data:', err);
        setError('Failed to load availability, leave, or break data. Please try again.');
      } finally {
        setLoading(false);
      }
    }

    loadAllData();
  }, [startDate, viewMode, zone, dayCount]);

  // ── 4) Handlers for Next/Prev & Toggle View Mode ──────────────────────────

  // Move the window backward by `dayCount` days
  const handlePrev = () => {
    const newDate = DateTime.fromISO(startDate, { zone })
      .minus({ days: dayCount })
      .toISODate();
    setStartDate(newDate);
  };

  // Move the window forward by `dayCount` days
  const handleNext = () => {
    const newDate = DateTime.fromISO(startDate, { zone })
      .plus({ days: dayCount })
      .toISODate();
    setStartDate(newDate);
  };

  // Toggle between “week” and “day” view
  const toggleViewMode = () => {
    const nextMode = viewMode === 'week' ? 'day' : 'week';
    setViewMode(nextMode);

    // When switching modes, reset to today’s date in that new mode
    const today = DateTime.now().setZone(zone).toISODate();
    setStartDate(today);
  };

  // ── 5) Render ───────────────────────────────────────────────────────────────
  return (
    <div className="px-3 py-3">

      {/* ─────────────── Control Bar: Prev/Next & Week↔Day Toggle ─────────────── */}
      <div className="d-flex justify-content-end align-items-center mb-4">
        <button
          className="btn btn-outline-secondary me-2"
          onClick={handlePrev}
        >
          ‹ Prev {viewMode === 'week' ? '7 Days' : '1 Day'}
        </button>
        <button
          className="btn btn-outline-secondary me-2"
          onClick={handleNext}
        >
          Next {viewMode === 'week' ? '7 Days' : '1 Day'} ›
        </button>
        <button
          className="btn btn-primary"
          onClick={toggleViewMode}
        >
          {viewMode === 'week'
            ? 'Switch to Daily View'
            : 'Switch to Weekly View'}
        </button>
      </div>


      {/* ─────────────── Loading Spinner / Error Message ──────────────────────── */}
      {loading && (
        <div className="text-center my-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading…</span>
          </div>
        </div>
      )}
      {error && <div className="alert alert-danger">{error}</div>}


      {/* ─────────────── Availability Grid ───────────────────────────────────── */}
      {!loading && !error && (
        <AvailabilityGrid
          records={availabilityRecords}
          onLeaveSet={onLeaveSet}
          onBreakSet={onBreakSet}
          zone={zone}
          viewMode={viewMode}
          startDate={startDate}
        />
      )}


      {/* ─────────────── Leave List Table ─────────────────────────────────────── */}
      {!loading && !error && leaveRecords.length > 0 && (
        <div className="mt-5">
          <h4>On Leave</h4>
          <LeaveList data={leaveRecords} zone={zone} />
        </div>
      )}

      {!loading && !error && breakRecords.length > 0 && (
        <div className="mt-5">
          <h4>On Break</h4>
          <BreakList data={breakRecords} zone={zone} />
        </div>
      )}
    </div>
  );
}
