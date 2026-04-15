import React from 'react';
import { Link, useLocation } from 'react-router-dom';

export default function SetupAssistBanner({ title, helperText }) {
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const inSetupFlow = params.get('setup') === '1';

  if (!inSetupFlow) {
    return null;
  }

  return (
    <div className="alert alert-primary d-flex justify-content-between align-items-center gap-3 flex-wrap shadow-sm">
      <div>
        <strong>{title}</strong>
        {helperText ? <div className="small mt-1">{helperText}</div> : null}
      </div>
      <Link className="btn btn-sm btn-outline-primary" to="/setup">
        Back to Setup
      </Link>
    </div>
  );
}
