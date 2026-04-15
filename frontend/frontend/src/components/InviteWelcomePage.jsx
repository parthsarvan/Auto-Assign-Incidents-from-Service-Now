import React from 'react';
import { Link, Navigate, useLocation, useOutletContext } from 'react-router-dom';
import { getCurrentUser } from '../services/auth';
import { canManageCurrentTeam, getDefaultRouteForUser } from '../services/permissions';

export default function InviteWelcomePage() {
  const location = useLocation();
  const outletContext = useOutletContext() || {};
  const currentUser = outletContext.currentUser || getCurrentUser();
  const joinedViaInvite = Boolean(location.state?.joinedViaInvite);

  if (!joinedViaInvite || !currentUser?.workspace) {
    return <Navigate to={getDefaultRouteForUser(currentUser)} replace />;
  }

  const organizationName = currentUser.workspace.organizationName || 'your organization';
  const teamName = currentUser.workspace.teamName || 'your team';
  const canManageTeam = canManageCurrentTeam(currentUser);
  const primaryDestination = canManageTeam ? '/summary' : '/';
  const primaryLabel = canManageTeam ? 'Open Summary' : 'Open Availability';

  return (
    <div className="container py-4">
      <div className="card border-success shadow-sm">
        <div className="card-body p-4">
          <div className="badge bg-success-subtle text-success border mb-3">Welcome to InciTeam</div>
          <h2 className="mb-2">You joined {teamName}</h2>
          <p className="text-muted mb-4">
            Your account is now connected to {organizationName}, and your active team workspace is set to {teamName}.
          </p>

          <div className="row g-3 mb-4">
            <div className="col-md-6">
              <div className="border rounded p-3 h-100">
                <div className="fw-semibold mb-1">Organization</div>
                <div className="text-muted">{organizationName}</div>
              </div>
            </div>
            <div className="col-md-6">
              <div className="border rounded p-3 h-100">
                <div className="fw-semibold mb-1">Active Team</div>
                <div className="text-muted">{teamName}</div>
              </div>
            </div>
          </div>

          <div className="d-flex gap-2 flex-wrap">
            <Link className="btn btn-primary" to={primaryDestination}>
              {primaryLabel}
            </Link>
            {canManageTeam && (
              <Link className="btn btn-outline-primary" to="/">
                View Availability
              </Link>
            )}
            {canManageTeam && (
              <Link className="btn btn-outline-secondary" to="/setup">
                Review Team Setup
              </Link>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
