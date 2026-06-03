// src/components/HomeLayout.jsx
import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { Outlet, useLocation, useNavigate, Link } from 'react-router-dom';
import Sidebar from './Sidebar';
import { getCurrentUser } from '../services/auth';
import { canManageCurrentTeam, INCOMPLETE_SETUP_ALLOWED_PATHS, isOrgAdmin } from '../services/permissions';
import { fetchSetupStatus } from '../services/setup';
import { fetchWorkspaceTeams, switchWorkspaceTeam } from '../services/workspace';
import { fetchCoverageSummary, fetchLeaveHandoff, fetchServiceNowValidation } from '../services/servicenow';
import { buildApiUrl } from '../services/api';
import './HomeLayout.css';

export default function HomeLayout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [setupStatus, setSetupStatus] = useState(null);
  const [user, setUser] = useState(getCurrentUser());
  const [teams, setTeams] = useState([]);
  const [teamLoading, setTeamLoading] = useState(false);
  const [teamError, setTeamError] = useState('');
  const [summaryAttentionCount, setSummaryAttentionCount] = useState(0);
  const navigate = useNavigate();
  const location = useLocation();

  const toggleSidebar = () => setIsSidebarOpen(prev => !prev);

  useEffect(() => {
    async function loadSetupStatus() {
      setSetupStatus(null);
      if (!canManageCurrentTeam(user)) {
        return;
      }
      try {
        const status = await fetchSetupStatus();
        setSetupStatus(status);
        const shouldAllowIncompleteSetupPath =
          INCOMPLETE_SETUP_ALLOWED_PATHS.has(location.pathname)
          || (!canManageCurrentTeam(user) && location.pathname === '/dashboard');
        if ((status.brandNew || !status.ready) && !shouldAllowIncompleteSetupPath) {
          navigate('/setup', { replace: true });
        }
      } catch (err) {
        console.error('Failed to load setup status:', err);
      }
    }

    loadSetupStatus();
  }, [location.pathname, navigate, user]);

  useEffect(() => {
    async function loadTeams() {
      if (!user) {
        return;
      }
      setTeamLoading(true);
      setTeamError('');
      try {
        const data = await fetchWorkspaceTeams();
        setTeams(data || []);
      } catch (err) {
        setTeamError('Failed to load teams.');
      } finally {
        setTeamLoading(false);
      }
    }

    loadTeams();
  }, [user]);

  useEffect(() => {
    async function loadSummaryAttentionCount() {
      if (!canManageCurrentTeam(user) || !setupStatus?.ready) {
        setSummaryAttentionCount(0);
        return;
      }
      try {
        const token = sessionStorage.getItem('token');
        const [coverage, validation, leaveHandoff, logsResponse] = await Promise.all([
          fetchCoverageSummary(7).catch(() => null),
          fetchServiceNowValidation().catch(() => null),
          fetchLeaveHandoff().catch(() => null),
          token
            ? axios.get(buildApiUrl('/logs/servicenow'), {
                headers: { Authorization: `Bearer ${token}` },
              }).catch(() => null)
            : Promise.resolve(null),
        ]);
        const latestPollLog = (logsResponse?.data || []).find((log) => log.type === 'POLL');
        const latestFailedAssignments = (latestPollLog?.assignmentResults || [])
          .filter((result) => result.status === 'FAILED').length;
        const pollErrorCount = latestPollLog?.status === 'ERROR' ? 1 : 0;
        const attentionCount =
          (coverage?.gapCount || 0)
          + (coverage?.ciRiskCount || 0)
          + (validation?.issues?.length || 0)
          + (leaveHandoff?.activeIncidentCount || 0)
          + latestFailedAssignments
          + pollErrorCount;
        setSummaryAttentionCount(attentionCount);
      } catch (err) {
        setSummaryAttentionCount(0);
      }
    }

    loadSummaryAttentionCount();
    window.addEventListener('incteam:summary-attention-refresh', loadSummaryAttentionCount);
    return () => window.removeEventListener('incteam:summary-attention-refresh', loadSummaryAttentionCount);
  }, [setupStatus?.ready, user]);

  useEffect(() => {
    const syncUser = () => setUser(getCurrentUser());
    window.addEventListener('incteam:user-session-changed', syncUser);
    return () => window.removeEventListener('incteam:user-session-changed', syncUser);
  }, []);

  useEffect(() => {
    const handleAuthExpired = (event) => {
      const message = event.detail?.message || 'Your session expired. Please sign in again.';
      setUser(null);
      navigate('/signin', {
        replace: true,
        state: {
          from: {
            pathname: location.pathname,
            search: location.search,
            hash: location.hash,
          },
          sessionMessage: message,
        },
      });
    };

    window.addEventListener('incteam:auth-expired', handleAuthExpired);
    return () => window.removeEventListener('incteam:auth-expired', handleAuthExpired);
  }, [location.hash, location.pathname, location.search, navigate]);

  const handleLogout = () => {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
    navigate('/signin', { replace: true });
  };

  const handleSwitchTeam = async (event) => {
    const nextTeamId = Number(event.target.value);
    if (!nextTeamId || nextTeamId === user?.workspace?.teamId) {
      return;
    }
    setTeamLoading(true);
    setTeamError('');
    try {
      await switchWorkspaceTeam(nextTeamId);
      const updatedUser = getCurrentUser();
      setUser(updatedUser);
      const refreshedTeams = await fetchWorkspaceTeams();
      setTeams(refreshedTeams || []);
      let targetPath = '/dashboard';
      if (canManageCurrentTeam(updatedUser)) {
        const status = await fetchSetupStatus();
        setSetupStatus(status);
        targetPath = status.brandNew || !status.ready ? '/setup' : '/summary';
      } else {
        setSetupStatus(null);
      }
      if (location.pathname !== targetPath) {
        navigate(targetPath, { replace: true });
      }
    } catch (err) {
      setTeamError('Failed to switch teams.');
    } finally {
      setTeamLoading(false);
    }
  };

  return (
    <div className="home-layout d-flex">
      {/* ─── Left: Sidebar (fixed width 60px or 220px) ─── */}
      <Sidebar
        isOpen={isSidebarOpen}
        toggleSidebar={toggleSidebar}
        currentUser={user}
        setupStatus={setupStatus}
        summaryAttentionCount={summaryAttentionCount}
      />
      {isSidebarOpen && (
        <button
          type="button"
          className="sidebar-backdrop"
          onClick={toggleSidebar}
          aria-label="Close sidebar"
        />
      )}

      {/* ─── Right: Main Content (shifts over by sidebar’s width) ─── */}
      <div
        className={`main-content ${isSidebarOpen ? 'sidebar-open' : 'sidebar-collapsed'}`}
      >
        {/* ───────── Top Bar ────────── */}
        <div className="top-bar d-flex justify-content-between align-items-center">
          <div className="top-bar__brand">
            <div className="top-bar__eyebrow">Incident Command Workspace</div>
            <h5 className="m-0">InciTeam</h5>
          </div>
          <div className="user-info d-flex align-items-center">
            {user && (
              <>
                {user.workspace?.organizationName && user.workspace?.teamName && (
                  <div className="top-bar__workspace me-3">
                    <div className="top-bar__workspace-label">{user.workspace.organizationName}</div>
                    <select
                      className="form-select form-select-sm top-bar__team-select"
                      value={user.workspace?.teamId || ''}
                      onChange={handleSwitchTeam}
                      disabled={teamLoading || teams.length === 0}
                    >
                      {teams.map((team) => (
                        <option key={team.teamId} value={team.teamId}>
                          {team.teamName}
                        </option>
                      ))}
                    </select>
                    {isOrgAdmin(user) && (
                      <Link
                        className="btn btn-outline-primary btn-sm"
                        to="/teams"
                      >
                        New Team
                      </Link>
                    )}
                  </div>
                )}
                <div className="top-bar__user me-3">
                  <strong>{user.username}</strong>
                  <span>{user.role}</span>
                </div>
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
        {teamError && (
          <div className="px-3 pt-2 text-danger small">
            {teamError}
          </div>
        )}

        {/* ───────── Page Content ────────── */}
        <div className="page-body p-3">
          <Outlet context={{ setupStatus, currentUser: user }} />
        </div>

        <footer className="legal-footer legal-footer--app">
          <div className="legal-footer__brand">InciTeam™</div>
          <div>
            <Link to="/privacy">Privacy Policy</Link>
          </div>
          <div>Copyright © 2026 Parth Sarvan. All Rights Reserved.</div>
          <div>U.S. Trademark Application Serial No. 99808275 pending.</div>
          <div>U.S. Copyright Case No. 1-15157770821 pending.</div>
        </footer>
      </div>
    </div>
  );
}
