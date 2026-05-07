import React, { useMemo, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { discoverOrganization, signUp } from '../services/auth';

export default function SignUpPage() {
  const [step, setStep] = useState(1);
  const [organizationName, setOrganizationName] = useState('');
  const [workEmail, setWorkEmail] = useState('');
  const [username, setUsername] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [password, setPassword] = useState('');
  const [inviteCode, setInviteCode] = useState('');
  const [teamName, setTeamName] = useState('');
  const [organizationExists, setOrganizationExists] = useState(false);
  const [resolvedOrganizationName, setResolvedOrganizationName] = useState('');
  const [emailDomain, setEmailDomain] = useState('');
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [checkingOrganization, setCheckingOrganization] = useState(false);
  const [creatingAccount, setCreatingAccount] = useState(false);
  const navigate = useNavigate();

  const normalizedInviteCode = inviteCode.trim().toUpperCase();
  const creatingNewOrganization = !organizationExists;
  const effectiveOrganizationName = useMemo(
    () => resolvedOrganizationName || organizationName.trim(),
    [organizationName, resolvedOrganizationName]
  );

  const handleOrganizationContinue = async (e) => {
    e.preventDefault();
    setError('');
    setCheckingOrganization(true);

    try {
      const response = await discoverOrganization({
        organizationName,
        workEmail,
      });
      setOrganizationExists(Boolean(response.organizationExists));
      setResolvedOrganizationName(response.organizationName || organizationName.trim());
      setEmailDomain(response.emailDomain || '');
      setStep(2);
    } catch (err) {
      if (err.response?.data) {
        setError(String(err.response.data));
      } else {
        setError('We could not verify your organization right now. Please try again.');
      }
    } finally {
      setCheckingOrganization(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setCreatingAccount(true);

    try {
      await signUp({
        username,
        firstName,
        lastName,
        workEmail,
        password,
        inviteCode: normalizedInviteCode,
        organizationName: effectiveOrganizationName,
        teamName,
      });
      setSuccessMsg('Account created! Redirecting to Sign In…');
      setTimeout(() => navigate('/signin', {
        state: {
          joinedViaInvite: organizationExists,
          firstWorkspaceOwner: creatingNewOrganization,
          username,
        },
      }), 1500);
    } catch (err) {
      if (err.response?.status === 409) {
        setError(String(err.response.data));
      } else if (err.response?.data) {
        setError(String(err.response.data));
      } else {
        setError('An error occurred. Try again.');
      }
    } finally {
      setCreatingAccount(false);
    }
  };

  const handleBack = () => {
    setStep(1);
    setError('');
    setSuccessMsg('');
  };

  return (
    <div className="container auth-shell d-flex align-items-center justify-content-center py-5">
      <div className="card auth-card auth-card--wide p-4 shadow-sm" style={{ maxWidth: '520px', width: '100%' }}>
        <h3 className="card-title text-center mb-2">Create Your InciTeam Account</h3>
        <p className="text-muted text-center small mb-4">
          Start with your organization and work email. We will guide you into either joining an existing workspace or creating the first team for a new one.
        </p>

        {error && <div className="alert alert-danger">{error}</div>}
        {successMsg && <div className="alert alert-success">{successMsg}</div>}

        {!successMsg && step === 1 && (
          <form onSubmit={handleOrganizationContinue}>
            <div className="mb-3">
              <label htmlFor="organizationName" className="form-label">Organization Name</label>
              <input
                type="text"
                id="organizationName"
                className="form-control"
                value={organizationName}
                onChange={(e) => setOrganizationName(e.target.value)}
                placeholder="e.g. Apple"
                required
              />
            </div>

            <div className="mb-3">
              <label htmlFor="workEmail" className="form-label">Work Email</label>
              <input
                type="email"
                id="workEmail"
                className="form-control"
                value={workEmail}
                onChange={(e) => setWorkEmail(e.target.value)}
                placeholder="e.g. abc@apple.com"
                required
              />
              <div className="form-text">
                We use your email domain to tell whether your organization already uses InciTeam.
              </div>
            </div>

            <button type="submit" className="btn btn-primary w-100" disabled={checkingOrganization}>
              {checkingOrganization ? 'Checking Organization…' : 'Continue'}
            </button>
          </form>
        )}

        {!successMsg && step === 2 && (
          <form onSubmit={handleSubmit}>
            <div className="rounded border bg-light p-3 mb-4">
              <div className="fw-semibold mb-1">
                {organizationExists ? 'Joining Existing Organization' : 'Creating New Organization'}
              </div>
              <div className="small text-muted">
                Organization: {effectiveOrganizationName}
              </div>
              <div className="small text-muted">
                Work email: {workEmail.trim().toLowerCase()}
              </div>
              {emailDomain && (
                <div className="small text-muted">
                  Email domain: {emailDomain}
                </div>
              )}
            </div>

            {organizationExists ? (
              <div className="alert alert-info small">
                We found an existing InciTeam organization for your email domain. Use a team invite code from your admin or manager to finish creating your account.
              </div>
            ) : (
              <div className="alert alert-info small">
                No InciTeam organization exists yet for your email domain. You are creating the first team and will land in setup after signing in.
              </div>
            )}

            <div className="mb-3">
              <label htmlFor="username" className="form-label">Username</label>
              <input
                type="text"
                id="username"
                className="form-control"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>

            <div className="row g-3">
              <div className="col-sm-6">
                <label htmlFor="firstName" className="form-label">First Name</label>
                <input
                  type="text"
                  id="firstName"
                  className="form-control"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  required
                />
              </div>
              <div className="col-sm-6">
                <label htmlFor="lastName" className="form-label">Last Name</label>
                <input
                  type="text"
                  id="lastName"
                  className="form-control"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="mb-3">
              <label htmlFor="confirmedWorkEmail" className="form-label">Work Email</label>
              <input
                type="email"
                id="confirmedWorkEmail"
                className="form-control"
                value={workEmail}
                readOnly
                disabled
              />
            </div>

            <div className="mb-3">
              <label htmlFor="password" className="form-label">Password</label>
              <input
                type="password"
                id="password"
                className="form-control"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            {organizationExists ? (
              <div className="mb-4">
                <label htmlFor="inviteCode" className="form-label">Team Invite Code</label>
                <input
                  type="text"
                  id="inviteCode"
                  className="form-control"
                  value={inviteCode}
                  onChange={(e) => setInviteCode(e.target.value.toUpperCase())}
                  placeholder="e.g. TEAM-AB12CD34"
                  required
                />
                <div className="form-text">
                  Invite codes come from the team manager or organization admin.
                </div>
              </div>
            ) : (
              <div className="mb-4">
                <label htmlFor="teamName" className="form-label">First Team Name</label>
                <input
                  type="text"
                  id="teamName"
                  className="form-control"
                  value={teamName}
                  onChange={(e) => setTeamName(e.target.value)}
                  placeholder="e.g. Platform Support"
                  required
                />
              </div>
            )}

            <div className="d-flex gap-2">
              <button type="button" className="btn btn-outline-secondary w-50" onClick={handleBack}>
                Back
              </button>
              <button type="submit" className="btn btn-primary w-50" disabled={creatingAccount}>
                {creatingAccount ? 'Creating…' : 'Create Account'}
              </button>
            </div>
          </form>
        )}

        <div className="mt-3 text-center">
          <span>Already have an account? </span>
          <Link to="/signin">Sign In</Link>
        </div>
      </div>
    </div>
  );
}
