// src/components/Sidebar.jsx
import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { canManageCurrentTeam, canViewCurrentTeam, isOrgAdmin } from '../services/permissions';
import './Sidebar.css';

export default function Sidebar({ isOpen, toggleSidebar, currentUser, setupStatus }) {
  const location = useLocation();
  const orgAdmin = isOrgAdmin(currentUser);
  const teamManager = canManageCurrentTeam(currentUser);
  const teamViewer = canViewCurrentTeam(currentUser);
  const setupLocked = Boolean(teamManager && setupStatus && !setupStatus.ready);
  const handleNavClick = () => {
    if (typeof window !== 'undefined' && window.innerWidth <= 768 && isOpen) {
      toggleSidebar();
    }
  };

  const menuItems = [
    ...(!setupLocked ? [{ label: 'Dashboard', path: '/dashboard' }] : []),
    ...(!setupLocked && teamManager ? [
      { label: 'Summary', path: '/summary' },
      { label: 'Setup', path: '/setup' },
    ] : teamManager ? [
      { label: 'Setup', path: '/setup' },
    ] : []),
    ...(!setupLocked && teamViewer ? [
      { label: 'Schedule', path: '/schedules' },
      { label: 'Geo Shift Mapping', path: '/geo-shift-mappings' },
      { label: 'Team Members', path: '/team-members' },
      { label: 'CI User Mapping', path: '/ci-user-mappings' },
      { label: 'Leaves', path: '/leaves' },
      { label: 'Break', path: '/breaks' },
    ] : []),
    ...(!setupLocked && teamManager ? [
      { label: 'Geo', path: '/geos' },
      { label: 'Shift', path: '/shifts' },
      { label: 'CI', path: '/configuration-items' },
    ] : []),
    ...(orgAdmin ? [
      { label: 'Teams', path: '/teams' },
      { label: 'User Access', path: '/user-access' },
    ] : []),
    ...(!setupLocked && teamViewer ? [
      { label: 'Logs', path: '/logs' },
      { label: 'Diagnostics', path: '/assignment-diagnostics' },
    ] : []),
  ];

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
          <ul className="sidebar__menu">
            {menuItems.map((item) => (
              <li key={item.path} className={location.pathname === item.path ? 'active' : ''}>
                <Link to={item.path} onClick={handleNavClick}>{item.label}</Link>
              </li>
            ))}
          </ul>
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
