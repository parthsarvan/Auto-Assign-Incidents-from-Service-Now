// src/components/Sidebar.jsx
import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import './Sidebar.css';

export default function Sidebar({ isOpen, toggleSidebar }) {
  const location = useLocation();

  const menuItems = [
    { label: 'Dashboard', path: '/' },
    { label: 'Schedule', path: '/schedules' },
    { label: 'Geo', path: '/geos' },
    { label: 'Shift', path: '/shifts' },
    { label: 'Geo Shift Mapping', path: '/geo-shift-mappings' },
    { label: 'Team Members', path: '/team-members' },
    { label: 'CI', path: '/configuration-items' },
    { label: 'CI User Mapping', path: '/ci-user-mappings' },
    { label: 'Leaves', path: '/leaves' },
    { label: 'Break', path: '/breaks' },
    { label: 'User Access', path: '/user-access' },
    { label: 'Logs', path: '/logs' },
    // add more items as needed…
  ];

  return (
    <div className={`sidebar ${isOpen ? 'sidebar--open' : 'sidebar--collapsed'}`}>
      {isOpen ? (
        <>
          {/* Expanded Header */}
          <div className="sidebar__header">
            <h5>Menu</h5>
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
                <Link to={item.path}>{item.label}</Link>
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
