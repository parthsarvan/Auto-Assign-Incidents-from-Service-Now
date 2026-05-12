import React from 'react';
import { Link } from 'react-router-dom';

export default function AccessDeniedPage({
  title = 'Access denied',
  message = 'You do not have permission to open this page.',
  backTo = '/summary',
  backLabel = 'Go to Summary',
}) {
  return (
    <div className="container py-4">
      <div className="card border-warning shadow-sm">
        <div className="card-body p-4">
          <div className="badge text-bg-warning mb-3">Permissions</div>
          <h3 className="mb-2">{title}</h3>
          <p className="text-muted mb-4">{message}</p>
          <div className="d-flex gap-2 flex-wrap">
            <Link className="btn btn-primary" to={backTo}>
              {backLabel}
            </Link>
            <Link className="btn btn-outline-secondary" to="/dashboard">
              Open Roster
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
