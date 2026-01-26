// src/App.js
import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import RequireAuth from './components/RequireAuth';
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
import Logs from './components/Logs';

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
          <Route path="geos" element={<AdminGeos />} />
          <Route path="shifts" element={<AdminShifts />} />
          <Route path="team-members" element={<AdminTeamMembers />} />
          <Route path="configuration-items" element={<AdminConfigurationItems />} />
          <Route path="geo-shift-mappings" element={<AdminGeoShiftMappings />} />
          <Route path="ci-user-mappings" element={<AdminCiUserMappings />} />
          <Route path="schedules" element={<AdminSchedules />} />
          <Route path="leaves" element={<AdminLeaves />} />
          <Route path="breaks" element={<AdminBreaks />} />
          <Route path="user-access" element={<AdminUserAccess />} />
          <Route path="logs" element={<Logs />} />
          {/* …other nested routes… */}
        </Route>
      </Routes>
    </Router>
  );
}
