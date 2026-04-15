// src/App.js
import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import RequireAuth from './components/RequireAuth';
import RequirePermission from './components/RequirePermission';
import HomeLayout from './components/HomeLayout';
import Dashboard from './components/Dashboard';
import SignInPage from './components/SignInPage';
import SignUpPage from './components/SignUpPage';
import AdminGeos from './components/AdminGeos';
import AdminShifts from './components/AdminShifts';
import AdminTeamMembers from './components/AdminTeamMembers';
import AdminConfigurationItems from './components/AdminConfigurationItems';
import AdminGeoShiftMappings from './components/AdminGeoShiftMappings';
import AdminCiUserMappings from './components/AdminCiUserMappings';
import AdminSchedules from './components/AdminSchedules';
import AdminLeaves from './components/AdminLeaves';
import AdminBreaks from './components/AdminBreaks';
import AdminUserAccess from './components/AdminUserAccess';
import AdminTeams from './components/AdminTeams';
import Logs from './components/Logs';
import SetupPage from './components/SetupPage';
import AssignmentDiagnostics from './components/AssignmentDiagnostics';
import Summary from './components/Summary';
import InviteWelcomePage from './components/InviteWelcomePage';
import { canManageCurrentTeam, isOrgAdmin } from './services/permissions';

function TeamManagerRoute({ children }) {
  return (
    <RequirePermission
      allow={canManageCurrentTeam}
      title="Team access required"
      message="This page is available to TEAM_ADMIN and MANAGER users for the current team."
      backTo="/summary"
      backLabel="Go to Summary"
    >
      {children}
    </RequirePermission>
  );
}

function OrgAdminRoute({ children }) {
  return (
    <RequirePermission
      allow={isOrgAdmin}
      title="Organization admin required"
      message="This page is available only to organization admins."
      backTo="/summary"
      backLabel="Go to Summary"
    >
      {children}
    </RequirePermission>
  );
}

export default function App() {
  return (
    <Router>
      <Routes>
        {/* Public */}
        <Route path="/signin" element={<SignInPage />} />
        <Route path="/signup" element={<SignUpPage />} />

        {/* Protected */}
        <Route
          path="/"
          element={
            <RequireAuth>
              <HomeLayout />
            </RequireAuth>
          }
        >
          <Route index element={<Dashboard />} />
          <Route path="summary" element={<TeamManagerRoute><Summary /></TeamManagerRoute>} />
          <Route path="welcome" element={<InviteWelcomePage />} />
          <Route path="setup" element={<TeamManagerRoute><SetupPage /></TeamManagerRoute>} />
          <Route path="geos" element={<TeamManagerRoute><AdminGeos /></TeamManagerRoute>} />
          <Route path="shifts" element={<TeamManagerRoute><AdminShifts /></TeamManagerRoute>} />
          <Route path="team-members" element={<TeamManagerRoute><AdminTeamMembers /></TeamManagerRoute>} />
          <Route path="configuration-items" element={<TeamManagerRoute><AdminConfigurationItems /></TeamManagerRoute>} />
          <Route path="geo-shift-mappings" element={<TeamManagerRoute><AdminGeoShiftMappings /></TeamManagerRoute>} />
          <Route path="ci-user-mappings" element={<TeamManagerRoute><AdminCiUserMappings /></TeamManagerRoute>} />
          <Route path="schedules" element={<TeamManagerRoute><AdminSchedules /></TeamManagerRoute>} />
          <Route path="leaves" element={<TeamManagerRoute><AdminLeaves /></TeamManagerRoute>} />
          <Route path="breaks" element={<TeamManagerRoute><AdminBreaks /></TeamManagerRoute>} />
          <Route path="user-access" element={<OrgAdminRoute><AdminUserAccess /></OrgAdminRoute>} />
          <Route path="teams" element={<OrgAdminRoute><AdminTeams /></OrgAdminRoute>} />
          <Route path="logs" element={<TeamManagerRoute><Logs /></TeamManagerRoute>} />
          <Route path="assignment-diagnostics" element={<TeamManagerRoute><AssignmentDiagnostics /></TeamManagerRoute>} />
          {/* …other nested routes… */}
        </Route>
      </Routes>
    </Router>
  );
}
