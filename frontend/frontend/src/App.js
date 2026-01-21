// src/App.js
import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import RequireAuth from './components/RequireAuth';
import HomeLayout from './components/HomeLayout';
import Dashboard from './components/Dashboard';
import SignInPage from './components/SignInPage';
import SignUpPage from './components/SignUpPage';

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
          {/* …other nested routes… */}
        </Route>
      </Routes>
    </Router>
  );
}
