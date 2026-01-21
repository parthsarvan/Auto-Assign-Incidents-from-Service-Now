// src/components/HomeLayout.jsx
import React, { useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import Sidebar from './Sidebar';
import { getCurrentUser } from '../services/auth';
import './HomeLayout.css';

export default function HomeLayout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const navigate = useNavigate();
  const user = getCurrentUser(); // e.g. { username, role }

  const toggleSidebar = () => setIsSidebarOpen(prev => !prev);

  const handleLogout = () => {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
    navigate('/signin', { replace: true });
  };

  return (
    <div className="home-layout d-flex">
      {/* ─── Left: Sidebar (fixed width 60px or 220px) ─── */}
      <Sidebar isOpen={isSidebarOpen} toggleSidebar={toggleSidebar} />

      {/* ─── Right: Main Content (shifts over by sidebar’s width) ─── */}
      <div
        className={`main-content ${isSidebarOpen ? 'sidebar-open' : 'sidebar-collapsed'}`}
      >
        {/* ───────── Top Bar ────────── */}
        <div className="top-bar d-flex justify-content-between align-items-center">
          <h5 className="m-0">Incident+Team Dashboard</h5>
          <div className="user-info d-flex align-items-center">
            {user && (
              <>
                <span className="me-3">
                  <strong>{user.username}</strong> / {user.role}
                </span>
                <button
                  className="btn btn-outline-danger btn-sm"
                  onClick={handleLogout}
                >
                  Logout
                </button>
              </>
            )}
          </div>
        </div>

        {/* ───────── Page Content ────────── */}
        <div className="page-body p-3">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
