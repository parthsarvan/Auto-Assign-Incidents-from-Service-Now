import React, { useEffect, useState } from 'react';
import { fetchCurrentRoutingWindow } from '../services/routing';
import './CurrentRoutingWindow.css';

function formatTimeRange(windowItem) {
  if (!windowItem?.startTime || !windowItem?.endTime) {
    return '';
  }
  return `${windowItem.startTime} - ${windowItem.endTime}`;
}

export default function CurrentRoutingWindow({ compact = false }) {
  const [routingWindow, setRoutingWindow] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let mounted = true;

    async function loadRoutingWindow() {
      setLoading(true);
      setError('');
      try {
        const data = await fetchCurrentRoutingWindow();
        if (mounted) {
          setRoutingWindow(data);
        }
      } catch (err) {
        if (mounted) {
          setError('Current geo and shift could not be loaded.');
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    }

    loadRoutingWindow();
    const intervalId = setInterval(loadRoutingWindow, 60000);

    return () => {
      mounted = false;
      clearInterval(intervalId);
    };
  }, []);

  const activeWindows = routingWindow?.activeWindows || [];
  const hasActiveWindow = routingWindow?.hasActiveWindow;
  const statusClass = hasActiveWindow ? 'current-routing-window--active' : 'current-routing-window--attention';

  return (
    <section className={`current-routing-window ${statusClass} ${compact ? 'current-routing-window--compact' : ''}`}>
      <div>
        <div className="current-routing-window__eyebrow">Current Routing Window</div>
        <h3 className="current-routing-window__title">
          {loading
            ? 'Resolving active geo and shift...'
            : hasActiveWindow
              ? activeWindows.map((item) => `${item.geo} / ${item.shift}`).join(', ')
              : 'No active geo / shift right now'}
        </h3>
        <div className="current-routing-window__meta">
          {routingWindow?.teamLocalDateTime && routingWindow?.timezone
            ? `Team time: ${routingWindow.teamLocalDateTime} (${routingWindow.timezone})`
            : 'Team time is based on the timezone configured during setup.'}
        </div>
        {!loading && (
          <div className="current-routing-window__message">
            {error || routingWindow?.message || 'This matches the routing clock used by polling and assignment.'}
          </div>
        )}
      </div>

      {activeWindows.length > 0 && (
        <div className="current-routing-window__chips">
          {activeWindows.map((item) => (
            <span className="current-routing-window__chip" key={`${item.geo}-${item.shift}`}>
              <strong>{item.geo}</strong>
              <span>{item.shift}</span>
              <small>{formatTimeRange(item)}</small>
            </span>
          ))}
        </div>
      )}
    </section>
  );
}
