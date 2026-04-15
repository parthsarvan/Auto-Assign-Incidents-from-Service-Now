// src/components/SignInPage.jsx
import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { getCurrentUser, signIn } from '../services/auth';
import { resolveLandingRoute } from '../services/permissions';

export default function SignInPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || '/'; // default redirect after login
  const joinedViaInvite = Boolean(location.state?.joinedViaInvite);

  useEffect(() => {
    if (location.state?.username) {
      setUsername(location.state.username);
    }
  }, [location.state]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await signIn(username, password);
      const signedInUser = getCurrentUser();
      if (joinedViaInvite) {
        navigate('/welcome', { replace: true, state: { joinedViaInvite: true } });
        return;
      }
      navigate(resolveLandingRoute(signedInUser, from), { replace: true });
    } catch (err) {
      if (err.response?.status === 400) {
        setError(String(err.response.data));
      } else
      if (err.response && err.response.status === 404) {
        setError('User not found. Please sign up first.');
      } else if (err.response && err.response.status === 401) {
        setError('Invalid credentials. Please try again.');
      } else {
        setError('An error occurred. Please try again later.');
      }
    }
  };

  return (
    <div className="container vh-100 d-flex align-items-center justify-content-center">
      <div className="card p-4 shadow-sm" style={{ maxWidth: '400px', width: '100%' }}>
        <h3 className="card-title text-center mb-2">Sign In to InciTeam</h3>
        <p className="text-muted text-center small mb-3">
          Sign in to access your organization and active team workspace.
        </p>
        {error && <div className="alert alert-danger">{error}</div>}
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
          <button type="submit" className="btn btn-primary w-100">Sign In</button>
        </form>
        <div className="mt-3 text-center">
          <span>Joining for the first time? </span>
          <Link to="/signup">Use an invite code</Link>
        </div>
      </div>
    </div>
  );
}
