// src/components/RequireAuth.jsx
import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { getCurrentUser } from '../services/auth';

export default function RequireAuth({ children }) {
  const user = getCurrentUser();
  const location = useLocation();

  if (!user) {
    // Save where we were trying to go, so we can redirect back after login (optional)
    return <Navigate to="/signin" state={{ from: location }} replace />;
  }

  // Otherwise, render the protected content
  return children;
}
