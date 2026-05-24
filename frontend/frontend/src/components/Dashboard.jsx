// src/components/Dashboard.jsx
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { DateTime } from 'luxon';
import axios from 'axios';
import { Link, useOutletContext } from 'react-router-dom';

import AvailabilityGrid from './AvailabilityGrid';
import LeaveList from './LeaveList';
import BreakList from './BreakList';
import CurrentRoutingWindow from './CurrentRoutingWindow';
import { canManageCurrentTeam } from '../services/permissions';
import { getCurrentUser } from '../services/auth';
import { buildApiUrl } from '../services/api';
import './Dashboard.css';

const DASHBOARD_REFRESH_MS = 60000;

function getMemberKeys(record) {
  const keys = [];
  if (record?.tmId !== null && record?.tmId !== undefined) {
    keys.push(`id:${record.tmId}`);
  }
  if (record?.fullName) {
    keys.push(`name:${record.fullName}`);
  }
  return keys;
}

function getPrimaryMemberKey(record) {
  return getMemberKeys(record)[0] || '';
}

function makeAvailabilityStatusKey(record) {
  return `${record.geoName}–${record.shiftName}||${record.date}||${getPrimaryMemberKey(record)}`;
}

function recordOverlapsRosterDate(record, date, zone) {
  const start = DateTime.fromISO(record.startTs, { zone: 'utc' }).setZone(zone);
  const end = DateTime.fromISO(record.endTs, { zone: 'utc' }).setZone(zone);
  const dayStart = DateTime.fromISO(date, { zone }).startOf('day');
  const dayEnd = dayStart.endOf('day');

  if (!start.isValid || !end.isValid || !dayStart.isValid) {
    return false;
  }

  return start <= dayEnd && end >= dayStart;
}

function buildImpactSet(availabilityRecords, impactRecords, zone) {
  const recordsByMember = new Map();

  impactRecords.forEach((record) => {
    getMemberKeys(record).forEach((memberKey) => {
      if (!recordsByMember.has(memberKey)) {
        recordsByMember.set(memberKey, []);
      }
      recordsByMember.get(memberKey).push(record);
    });
  });

  const impactSet = new Set();
  availabilityRecords.forEach((availabilityRecord) => {
    const relatedImpactRecords = getMemberKeys(availabilityRecord)
      .flatMap((memberKey) => recordsByMember.get(memberKey) || []);
    const impacted = relatedImpactRecords.some((impactRecord) =>
      recordOverlapsRosterDate(impactRecord, availabilityRecord.date, zone)
    );

    if (impacted) {
      impactSet.add(makeAvailabilityStatusKey(availabilityRecord));
    }
  });

  return impactSet;
}

export default function Dashboard() {
  const outletContext = useOutletContext() || {};
  const currentUser = outletContext.currentUser || getCurrentUser();
  const { setupStatus } = outletContext;
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
  const teamName = currentUser?.workspace?.teamName || 'Current Team';
  const organizationName = currentUser?.workspace?.organizationName || 'Your Organization';
  const canManageTeam = canManageCurrentTeam(currentUser);
  const schedulesStep = setupStatus?.steps?.find((step) => step.key === 'schedules');
  const isBrandNewTeam = Boolean(setupStatus?.brandNew);
  const setupIncomplete = Boolean(setupStatus && !setupStatus.ready);
  const schedulesMissing = Boolean(setupStatus?.ready && schedulesStep && !schedulesStep.complete);
  const hasAnyAvailabilityData =
    availabilityRecords.length > 0 || leaveRecords.length > 0 || breakRecords.length > 0;

  // ── 3) Fetch Availability & Leave Whenever startDate or viewMode Changes ──
  const loadAllData = useCallback(async () => {
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
          buildApiUrl('/schedule/next'),
          {
            params: { startDate, days: dayCount },
            headers: { Authorization: `Bearer ${token}` },
          }
        );
        const availabilityFlat = availResp.data || [];
        setAvailabilityRecords(availabilityFlat);

        // 3b) Fetch “on‐leave” records from the backend
        //    (this returns flat objects: { fullName, geoName, shiftName, startTs, endTs, reason })
        const leaveResp = await axios.get(
          buildApiUrl('/leave/next'),
          {
            params: { startDate, days: dayCount },
            headers: { Authorization: `Bearer ${token}` },
          }
        );

        const leaveFlat = (leaveResp.data || []).map((rec) => {
          const utcDate = DateTime.fromISO(rec.startTs, { zone: 'utc' }).toISODate();
          return {
            tmId:      rec.tmId,
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
        setOnLeaveSet(buildImpactSet(availabilityFlat, leaveFlat, zone));

        // 3d) Fetch “on‐break” records from the backend
        const breakResp = await axios.get(
          buildApiUrl('/break/next'),
          {
            params: { startDate, days: dayCount },
            headers: { Authorization: `Bearer ${token}` },
          }
        );

        const breakFlat = (breakResp.data || []).map((rec) => {
          const utcDate = DateTime.fromISO(rec.startTs, { zone: 'utc' }).toISODate();
          return {
            tmId: rec.tmId,
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
        setOnBreakSet(buildImpactSet(availabilityFlat, breakFlat, zone));
      } catch (err) {
        console.error('Error loading data:', err);
        setError('Failed to load availability, leave, or break data. Please try again.');
      } finally {
        setLoading(false);
      }
    }, [dayCount, startDate, zone]);

  useEffect(() => {
    loadAllData();
    const intervalId = setInterval(loadAllData, DASHBOARD_REFRESH_MS);
    return () => clearInterval(intervalId);
  }, [loadAllData]);

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

  const emptyState = useMemo(() => {
    if (loading || error || hasAnyAvailabilityData) {
      return null;
    }

    if (setupIncomplete) {
      return {
        variant: 'warning',
        message: isBrandNewTeam
          ? `${teamName} has not been set up yet, so there is no availability to show.`
          : `${teamName} setup is still incomplete, so the availability view does not have enough team data yet.`,
        ctaTo: canManageTeam ? '/setup' : null,
        ctaLabel: canManageTeam ? 'Continue Setup' : null,
      };
    }

    if (schedulesMissing) {
      return {
        variant: 'info',
        message: `${teamName} setup is complete, but no schedules have been added yet. Add schedules to populate on-shift availability.`,
        ctaTo: canManageTeam ? '/schedules' : null,
        ctaLabel: canManageTeam ? 'Add Schedules' : null,
      };
    }

    return {
      variant: 'secondary',
      message: `No on-shift availability, leave, or break data exists for ${teamName} in this ${viewMode === 'week' ? '7-day' : '1-day'} window.`,
      ctaTo: canManageTeam ? '/summary' : null,
      ctaLabel: canManageTeam ? 'Open Summary' : null,
    };
  }, [
    canManageTeam,
    error,
    hasAnyAvailabilityData,
    isBrandNewTeam,
    loading,
    schedulesMissing,
    setupIncomplete,
    teamName,
    viewMode,
  ]);

  // ── 5) Render ───────────────────────────────────────────────────────────────
  return (
    <div className="dashboard-page px-3 py-3">
      <div className="dashboard-hero mb-4">
        <div>
          <div className="dashboard-hero__eyebrow">Team Availability</div>
          <h2 className="mb-1">{teamName} Coverage Board</h2>
          <div className="text-muted">
            Live on-shift availability, leave, and break visibility for {teamName} in {organizationName}.
          </div>
        </div>
        <div className="dashboard-toolbar">
          <button
            className="btn btn-outline-secondary"
            onClick={handlePrev}
          >
            ‹ Prev {viewMode === 'week' ? '7 Days' : '1 Day'}
          </button>
          <button
            className="btn btn-outline-secondary"
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
      </div>

      <CurrentRoutingWindow />

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

      {!loading && !error && emptyState && (
        <div className={`alert alert-${emptyState.variant} d-flex justify-content-between align-items-center gap-3 flex-wrap mt-4`}>
          <div>{emptyState.message}</div>
          {emptyState.ctaTo && emptyState.ctaLabel && (
            <Link className="btn btn-sm btn-outline-primary" to={emptyState.ctaTo}>
              {emptyState.ctaLabel}
            </Link>
          )}
        </div>
      )}


      {/* ─────────────── Leave List Table ─────────────────────────────────────── */}
      {!loading && !error && leaveRecords.length > 0 && (
        <div className="dashboard-section mt-5">
          <div className="dashboard-section__header">
            <div>
              <div className="dashboard-section__eyebrow">Availability Impact</div>
              <h4>On Leave</h4>
            </div>
          </div>
          <LeaveList data={leaveRecords} zone={zone} />
        </div>
      )}

      {!loading && !error && breakRecords.length > 0 && (
        <div className="dashboard-section mt-5">
          <div className="dashboard-section__header">
            <div>
              <div className="dashboard-section__eyebrow">Availability Impact</div>
              <h4>On Break</h4>
            </div>
          </div>
          <BreakList data={breakRecords} zone={zone} />
        </div>
      )}
    </div>
  );
}
