// src/components/AvailabilityGrid.jsx
import React, { useMemo } from 'react';
import { DateTime } from 'luxon';
import './AvailabilityGrid.css';

export default function AvailabilityGrid({
  records,     // flat array of { fullName, geoName, shiftName, date, … }
  onLeaveSet,  // Set of strings: "geo–shift||YYYY-MM-DD||fullName"
  onBreakSet,  // Set of strings: "geo–shift||YYYY-MM-DD||fullName"
  zone,        // IANA timezone string for formatting only
  viewMode,    // "week" or "day"
  startDate    // "YYYY-MM-DD"
}) {
  const dayCount = viewMode === 'week' ? 7 : 1;

  // 1) Build an array of consecutive ISO dates starting at `startDate`
  const dateArray = useMemo(() => {
    const base = DateTime.fromISO(startDate, { zone }).startOf('day');
    return Array.from({ length: dayCount }).map((_, i) =>
      base.plus({ days: i }).toISODate()
    );
  }, [startDate, zone, dayCount]);

  // 2) Determine all unique "geo–shift" values
  const geoShiftSet = useMemo(() => {
    const s = new Set();
    records.forEach((r) => {
      s.add(`${r.geoName}–${r.shiftName}`);
    });
    return Array.from(s).sort();
  }, [records]);

  // 3) Build a map { "geo–shift": { "YYYY-MM-DD": [ fullName, … ] } }
  //    so we can quickly gather "namesOnShift" for each cell
  const gridMap = useMemo(() => {
    const m = {};
    records.forEach((r) => {
      const key = `${r.geoName}–${r.shiftName}`;
      if (!m[key]) m[key] = {};
      if (!m[key][r.date]) m[key][r.date] = [];
      m[key][r.date].push(r.fullName);
    });
    return m;
  }, [records]);

  return (
    <div className="availability-container">
      <h4 className="mb-3">
        On-Shift Availability ({viewMode === 'week' ? 'Weekly' : 'Daily'})
      </h4>
      <div className="table-responsive">
        <table className="table availability-table text-center">
          <thead>
            <tr>
              <th style={{ minWidth: '140px' }}>Geo – Shift</th>
              {dateArray.map((date) => {
                const label = DateTime.fromISO(date, { zone }).toFormat(
                  viewMode === 'week' ? 'ccc LLL dd' : 'cccc, LLL dd'
                );
                return <th key={date}>{label}</th>;
              })}
            </tr>
          </thead>
          <tbody>
            {geoShiftSet.map((geoShift) => (
              <tr key={geoShift}>
                <td className="geo-shift-cell">
                  <strong>{geoShift}</strong>
                </td>

                {dateArray.map((date) => {
                  // 3a) All names scheduled for this geoShift/date
                  const namesOnShift = gridMap[geoShift]?.[date] || [];

                  return (
                    <td key={date}>
                      <div className="cell-content">
                        {namesOnShift.map((fullName) => {
                          // 3b) Build the exact key to check if they are on leave
                          const leaveKey = `${geoShift}||${date}||${fullName}`;
                          const isOnLeave = onLeaveSet.has(leaveKey);
                          const isOnBreak = onBreakSet.has(leaveKey);
                          const availabilityClass = isOnLeave
                            ? ' on-leave'
                            : isOnBreak
                            ? ' on-break'
                            : ' on-available';

                          return (
                            <div
                              key={fullName}
                              className={`cell-name${availabilityClass}`}
                            >
                              {fullName}
                            </div>
                          );
                        })}
                      </div>
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
