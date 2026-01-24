import React, { useEffect, useState } from 'react';
import { fetchUsers, updateUserRole } from '../services/admin';
import { getCurrentUser } from '../services/auth';

export default function AdminUserAccess() {
  const [users, setUsers] = useState([]);
  const [pendingRoles, setPendingRoles] = useState({});
  const [error, setError] = useState('');
  const isAdmin = getCurrentUser()?.role === 'Admin';

  const loadUsers = async () => {
    try {
      const data = await fetchUsers();
      setUsers(data);
      const initialRoles = {};
      data.forEach((user) => {
        initialRoles[user.id] = user.role;
      });
      setPendingRoles(initialRoles);
    } catch (err) {
      setError('Failed to load users.');
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleRoleChange = (id, value) => {
    setPendingRoles((prev) => ({ ...prev, [id]: value }));
  };

  const handleUpdate = async (id) => {
    setError('');
    const role = pendingRoles[id];
    if (!role) {
      setError('Please select a role before saving.');
      return;
    }
    if (!window.confirm('Update this user role?')) {
      return;
    }
    try {
      await updateUserRole(id, role);
      loadUsers();
    } catch (err) {
      setError('Failed to update user role.');
    }
  };

  return (
    <div className="container">
      <h4 className="mb-3">Manage User Access</h4>
      {error && <div className="alert alert-danger">{error}</div>}
      {!isAdmin && (
        <div className="alert alert-info">Read-only access. Contact an admin to make changes.</div>
      )}

      <div className="table-responsive">
        <table className="table table-bordered">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Username</th>
              <th>Role</th>
              {isAdmin && <th style={{ width: '160px' }}>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>{user.id}</td>
                <td>{user.username}</td>
                <td>
                  <select
                    className="form-select"
                    value={pendingRoles[user.id] || user.role}
                    onChange={(e) => handleRoleChange(user.id, e.target.value)}
                    disabled={!isAdmin}
                  >
                    <option value="User">User</option>
                    <option value="Admin">Admin</option>
                  </select>
                </td>
                {isAdmin && (
                  <td>
                    <button
                      className="btn btn-outline-primary btn-sm"
                      onClick={() => handleUpdate(user.id)}
                    >
                      Update Role
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {users.length === 0 && (
              <tr>
                <td colSpan={isAdmin ? 4 : 3} className="text-center">No users found.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
