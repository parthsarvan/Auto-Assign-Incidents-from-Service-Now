// src/components/Sidebar.jsx
import React, { useEffect, useMemo, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { canManageCurrentTeam, canViewCurrentTeam, isOrgAdmin } from '../services/permissions';
import './Sidebar.css';

export default function Sidebar({
  isOpen,
  toggleSidebar,
  currentUser,
  setupStatus,
  summaryAttentionCount = 0,
}) {
  const location = useLocation();
  const orgAdmin = isOrgAdmin(currentUser);
  const teamManager = canManageCurrentTeam(currentUser);
  const teamViewer = canViewCurrentTeam(currentUser);
  const setupLocked = Boolean(teamManager && setupStatus && !setupStatus.ready);
  const [openGroups, setOpenGroups] = useState({});
  const handleNavClick = () => {
    if (typeof window !== 'undefined' && window.innerWidth <= 768 && isOpen) {
      toggleSidebar();
    }
  };

  const navGroups = useMemo(() => {
    const compact = (items) => items.filter(Boolean);
    return [
      {
        key: 'setup',
        label: 'Setup',
        items: compact([
          teamManager && { label: 'Setup', path: '/setup' },
        ]),
      },
      {
        key: 'roster',
        label: 'Roster & Planning',
        items: compact([
          !setupLocked && { label: 'Roster', path: '/dashboard' },
          !setupLocked && teamViewer && { label: 'Schedule', path: '/schedules' },
        ]),
      },
      {
        key: 'people',
        label: 'People',
        items: compact([
          !setupLocked && teamViewer && { label: 'Team Members', path: '/team-members' },
          orgAdmin && { label: 'Teams', path: '/teams' },
        ]),
      },
      {
        key: 'coverage',
        label: 'Coverage Rules',
        items: compact([
          !setupLocked && teamManager && { label: 'Geo', path: '/geos' },
          !setupLocked && teamManager && { label: 'Shift', path: '/shifts' },
          !setupLocked && teamViewer && { label: 'Geo Shift Mapping', path: '/geo-shift-mappings' },
        ]),
      },
      {
        key: 'availability',
        label: 'Availability',
        items: compact([
          !setupLocked && teamViewer && { label: 'Leaves', path: '/leaves' },
          !setupLocked && teamViewer && { label: 'Breaks', path: '/breaks' },
        ]),
      },
      {
        key: 'routing',
        label: 'CI Routing',
        items: compact([
          !setupLocked && teamManager && { label: 'CI', path: '/configuration-items' },
          !setupLocked && teamViewer && { label: 'CI User Mapping', path: '/ci-user-mappings' },
        ]),
      },
      {
        key: 'operations',
        label: 'Operations',
        items: compact([
          !setupLocked && teamManager && { label: 'Summary', path: '/summary', badgeCount: summaryAttentionCount },
          !setupLocked && teamViewer && { label: 'Logs', path: '/logs' },
          !setupLocked && teamViewer && { label: 'Diagnostics', path: '/assignment-diagnostics' },
        ]),
      },
      {
        key: 'access',
        label: 'Access',
        items: compact([
          orgAdmin && { label: 'User Access', path: '/user-access' },
        ]),
      },
    ].filter((group) => group.items.length > 0);
  }, [orgAdmin, setupLocked, summaryAttentionCount, teamManager, teamViewer]);

  const activeGroupKey = useMemo(() => (
    navGroups.find((group) => group.items.some((item) => item.path === location.pathname))?.key || null
  ), [location.pathname, navGroups]);

  useEffect(() => {
    if (!activeGroupKey) {
      return;
    }
    setOpenGroups((previous) => ({
      ...previous,
      [activeGroupKey]: true,
    }));
  }, [activeGroupKey]);

  const toggleGroup = (groupKey) => {
    setOpenGroups((previous) => ({
      ...previous,
      [groupKey]: !(previous[groupKey] ?? groupKey === activeGroupKey),
    }));
  };

  return (
    <div className={`sidebar ${isOpen ? 'sidebar--open' : 'sidebar--collapsed'}`}>
      {isOpen ? (
        <>
          {/* Expanded Header */}
          <div className="sidebar__header">
            <div>
              <div className="sidebar__eyebrow">Operations</div>
              <h5>InciTeam</h5>
            </div>
            <button
              className="sidebar__toggle"
              onClick={toggleSidebar}
              aria-label="Collapse sidebar"
            >
              〈
            </button>
          </div>

          {/* Expanded Menu */}
          <nav className="sidebar__groups" aria-label="Main navigation">
            {navGroups.map((group) => {
              const groupActive = group.key === activeGroupKey;
              const groupOpen = openGroups[group.key] ?? groupActive;
              const groupBadgeCount = group.items.reduce((total, item) => total + (item.badgeCount || 0), 0);
              return (
                <div
                  className={`sidebar__group ${groupActive ? 'sidebar__group--active' : ''}`}
                  key={group.key}
                >
                  <button
                    type="button"
                    className="sidebar__group-toggle"
                    onClick={() => toggleGroup(group.key)}
                    aria-expanded={groupOpen}
                  >
                    <span>{group.label}</span>
                    <span className="sidebar__group-meta">
                      {groupBadgeCount > 0 && (
                        <span className="sidebar__badge" aria-label={`${groupBadgeCount} items need attention`}>
                          {groupBadgeCount > 99 ? '99+' : groupBadgeCount}
                        </span>
                      )}
                      <span className={`sidebar__chevron ${groupOpen ? 'sidebar__chevron--open' : ''}`}>
                        ›
                      </span>
                    </span>
                  </button>

                  {groupOpen && (
                    <ul className="sidebar__submenu">
                      {group.items.map((item) => (
                        <li key={item.path} className={location.pathname === item.path ? 'active' : ''}>
                          <Link to={item.path} onClick={handleNavClick}>
                            <span>{item.label}</span>
                            {item.badgeCount > 0 && (
                              <span
                                className="sidebar__badge"
                                aria-label={`${item.badgeCount} summary items need attention`}
                              >
                                {item.badgeCount > 99 ? '99+' : item.badgeCount}
                              </span>
                            )}
                          </Link>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              );
            })}
          </nav>
        </>
      ) : (
        <>
          {/* Collapsed Header with Hamburger */}
          <div className="sidebar__header-collapsed">
            <button
              className="sidebar__toggle-collapsed"
              onClick={toggleSidebar}
              aria-label="Expand sidebar"
            >
              ☰
            </button>
          </div>
        </>
      )}
    </div>
  );
}
