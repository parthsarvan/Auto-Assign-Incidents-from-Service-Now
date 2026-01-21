// src/components/SignUpPage.jsx
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { signUp } from '../services/auth';

export default function SignUpPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    try {
      // Always send role="User"
      await signUp(username, password, 'User');
      setSuccessMsg('Account created! Redirecting to Sign In…');
      setTimeout(() => navigate('/signin'), 1500);
    } catch (err) {
      if (err.response?.status === 409) {
        setError('Username already exists.');
      } else {
        setError('An error occurred. Try again.');
      }
    }
  };

  return (
    <div className="container vh-100 d-flex align-items-center justify-content-center">
      <div className="card p-4 shadow-sm" style={{ maxWidth: '400px', width: '100%' }}>
        <h3 className="card-title text-center mb-3">Sign Up</h3>
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

            <button type="submit" className="btn btn-primary w-100">Create Account</button>
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
