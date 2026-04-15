import React from 'react';
import { getCurrentUser } from '../services/auth';
import AccessDeniedPage from './AccessDeniedPage';

export default function RequirePermission({
  children,
  allow,
  title,
  message,
  backTo,
  backLabel,
}) {
  const user = getCurrentUser();

  if (!allow(user)) {
    return (
      <AccessDeniedPage
        title={title}
        message={message}
        backTo={backTo}
        backLabel={backLabel}
      />
    );
  }

  return children;
}
