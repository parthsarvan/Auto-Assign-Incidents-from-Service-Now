// src/components/SignUpPage.jsx
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { signUp } from '../services/auth';

export default function SignUpPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [inviteCode, setInviteCode] = useState('');
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const normalizedInviteCode = inviteCode.trim().toUpperCase();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    try {
      await signUp(username, password, normalizedInviteCode);
      setSuccessMsg('Account created! Redirecting to Sign In…');
      setTimeout(() => navigate('/signin', {
        state: {
          joinedViaInvite: Boolean(normalizedInviteCode),
          username,
        },
      }), 1500);
    } catch (err) {
      if (err.response?.status === 409) {
        setError('Username already exists.');
      } else if (err.response?.data) {
        setError(String(err.response.data));
      } else {
        setError('An error occurred. Try again.');
      }
    }
  };

  return (
    <div className="container vh-100 d-flex align-items-center justify-content-center">
      <div className="card p-4 shadow-sm" style={{ maxWidth: '400px', width: '100%' }}>
        <h3 className="card-title text-center mb-2">Join InciTeam</h3>
        <p className="text-muted text-center small mb-3">
          Create your account and use a team invite code to join your organization.
        </p>
        {error && <div className="alert alert-danger">{error}</div>}
        {successMsg && <div className="alert alert-success">{successMsg}</div>}
        {!successMsg && (
          <form onSubmit={handleSubmit}>
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

            <div className="mb-3">
              <label htmlFor="inviteCode" className="form-label">Team Invite Code</label>
              <input
                type="text"
                id="inviteCode"
                className="form-control"
                value={inviteCode}
                onChange={(e) => setInviteCode(e.target.value.toUpperCase())}
                placeholder="e.g. TEAM-AB12CD34"
              />
              <div className="form-text">
                Ask your admin for a team invite code. Only the very first InciTeam account can be created without one.
              </div>
            </div>

            <button type="submit" className="btn btn-primary w-100">Create Account and Join Team</button>
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
