// src/components/LeaveList.jsx
import React from 'react';
import { DateTime } from 'luxon';

export default function LeaveList({ data, zone }) {
  // Format an incoming ISO (UTC‐based) into local zone
  const fmt = (isoStr) =>
    DateTime.fromISO(isoStr, { zone: 'utc' })
      .setZone(zone)
      .toFormat('ccc, LLL dd, yyyy hh:mm a');

  const fmtDuration = (startIso, endIso) => {
    const start = DateTime.fromISO(startIso, { zone: 'utc' }).setZone(zone);
    const end   = DateTime.fromISO(endIso,   { zone: 'utc' }).setZone(zone);
    const diff  = end.diff(start, ['days', 'hours', 'minutes']);

    let parts = [];
    if (diff.days > 0) parts.push(`${diff.days} day${diff.days > 1 ? 's' : ''}`);
    if (diff.hours > 0) parts.push(`${diff.hours} hr${diff.hours > 1 ? 's' : ''}`);
    if (diff.minutes > 0 && diff.days === 0)
      parts.push(`${diff.minutes} min${diff.minutes > 1 ? 's' : ''}`);
    return parts.join(' ') || '0 min';
  };

  return (
    <div className="table-responsive">
      <table className="table table-bordered">
        <thead className="table-light">
          <tr>
            <th>Team Member</th>
            <th>Geo</th>
            <th>Shift</th>
            <th>Leave Start</th>
            <th>Leave End</th>
            <th>Duration</th>
            <th>Reason</th>
          </tr>
        </thead>
        <tbody>
          {data.map((rec, idx) => (
            <tr key={idx}>
              <td>{rec.fullName}</td>
              <td>{rec.geoName}</td>
              <td>{rec.shiftName}</td>
              <td>{fmt(rec.startTs)}</td>
              <td>{fmt(rec.endTs)}</td>
              <td>{fmtDuration(rec.startTs, rec.endTs)}</td>
              <td>{rec.reason || '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
