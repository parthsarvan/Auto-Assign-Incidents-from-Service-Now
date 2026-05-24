import React, { useState } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { deleteCurrentAccount } from '../services/account';
import { getCurrentUser } from '../services/auth';
import './AccountSettingsPage.css';

function describeRequestError(err, fallbackMessage) {
  return typeof err?.response?.data === 'string' && err.response.data
    ? err.response.data
    : fallbackMessage;
}

export default function AccountSettingsPage() {
  const navigate = useNavigate();
  const outletContext = useOutletContext() || {};
  const currentUser = outletContext.currentUser || getCurrentUser();
  const [confirmation, setConfirmation] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');
  const canSubmit = confirmation.trim().toUpperCase() === 'DELETE';

  const handleDeleteAccount = async () => {
    setError('');
    if (!canSubmit) {
      setError('Type DELETE to confirm account deletion.');
      return;
    }
    if (!window.confirm('Permanently delete your InciTeam account? This cannot be undone.')) {
      return;
    }
    setDeleting(true);
    try {
      await deleteCurrentAccount();
      navigate('/signin', { replace: true });
    } catch (err) {
      setError(describeRequestError(err, 'Failed to delete account.'));
      setDeleting(false);
    }
  };

  return (
    <div className="container account-settings-page">
      <div className="admin-page-hero mb-4">
        <div className="admin-page-hero__eyebrow">Account</div>
        <h2 className="mb-1">Account Settings</h2>
        <div className="text-muted">Review your account details and manage account deletion.</div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="row g-3">
        <div className="col-lg-5">
          <div className="card admin-page-card h-100">
            <div className="card-body">
              <div className="summary-card__label">Signed In As</div>
              <h3 className="account-settings-page__name">{currentUser?.username || 'Current user'}</h3>
              <dl className="account-settings-page__details">
                <div>
                  <dt>Work Email</dt>
                  <dd>{currentUser?.workEmail || 'Not available'}</dd>
                </div>
                <div>
                  <dt>Organization Role</dt>
                  <dd>{currentUser?.role || 'User'}</dd>
                </div>
                <div>
                  <dt>Current Team</dt>
                  <dd>{currentUser?.workspace?.teamName || 'None'}</dd>
                </div>
                <div>
                  <dt>Team Role</dt>
                  <dd>{currentUser?.workspace?.teamRole || 'Member'}</dd>
                </div>
              </dl>
            </div>
          </div>
        </div>

        <div className="col-lg-7">
          <div className="card admin-page-card account-settings-page__danger">
            <div className="card-body">
              <div className="summary-card__label">Danger Zone</div>
              <h3>Delete Account</h3>
              <p>
                This removes your InciTeam login, team access, push notification tokens,
                and roster records linked to your work email, including routing mappings,
                schedules, leaves, and breaks.
              </p>
              <p>
                If your account is the last organization Admin or last TEAM_ADMIN for a team,
                assign another admin first so the workspace remains manageable.
              </p>
              <label className="form-label" htmlFor="delete-account-confirmation">
                Type DELETE to confirm
              </label>
              <input
                id="delete-account-confirmation"
                type="text"
                className="form-control"
                value={confirmation}
                onChange={(event) => setConfirmation(event.target.value)}
                disabled={deleting}
              />
              <button
                type="button"
                className="btn btn-danger mt-3"
                onClick={handleDeleteAccount}
                disabled={!canSubmit || deleting}
              >
                {deleting ? 'Deleting Account...' : 'Delete My Account'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
