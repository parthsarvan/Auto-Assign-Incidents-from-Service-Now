import React, { useEffect, useState } from 'react';
import {
  createConfigurationItem,
  deleteConfigurationItem,
  fetchConfigurationItems,
  updateConfigurationItem,
} from '../services/admin';
import { getCurrentUser } from '../services/auth';

export default function AdminConfigurationItems() {
  const [items, setItems] = useState([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const isAdmin = getCurrentUser()?.role === 'Admin';

  const loadItems = async () => {
    try {
      const data = await fetchConfigurationItems();
      setItems(data);
    } catch (err) {
      setError('Failed to load configuration items.');
    }
  };

  useEffect(() => {
    loadItems();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editingId) {
        if (!window.confirm('Update this configuration item?')) {
          return;
        }
        await updateConfigurationItem(editingId, { name, description });
        setEditingId(null);
        setName('');
        setDescription('');
        loadItems();
        return;
      }
      if (!window.confirm('Add this configuration item?')) {
        return;
      }
      await createConfigurationItem({ name, description });
      setName('');
      setDescription('');
      loadItems();
    } catch (err) {
      setError('Failed to create configuration item.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this configuration item?')) {
      return;
    }
    await deleteConfigurationItem(id);
    loadItems();
  };

  const handleEdit = (item) => {
    setEditingId(item.ci_id);
    setName(item.name || '');
    setDescription(item.description || '');
  };

  const handleCancel = () => {
    setEditingId(null);
    setName('');
    setDescription('');
  };

  return (
    <div className="container">
      <h4 className="mb-3">Manage Configuration Items</h4>
      {error && <div className="alert alert-danger">{error}</div>}

      {isAdmin ? (
        <div className="card p-3 mb-4">
          <form className="row g-3" onSubmit={handleSubmit}>
            <div className="col-md-4">
              <label className="form-label">Name</label>
              <input
                type="text"
                className="form-control"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
            <div className="col-md-6">
              <label className="form-label">Description</label>
              <input
                type="text"
                className="form-control"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
            <div className="col-12 d-flex gap-2">
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update CI' : 'Add CI'}
              </button>
              {editingId && (
                <button type="button" className="btn btn-outline-secondary" onClick={handleCancel}>
                  Cancel
                </button>
              )}
            </div>
          </form>
        </div>
      ) : (
        <div className="alert alert-info">Read-only access. Contact an admin to make changes.</div>
      )}

      <div className="table-responsive">
        <table className="table table-bordered">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Description</th>
              {isAdmin && <th style={{ width: '180px' }}>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.ci_id}>
                <td>{item.ci_id}</td>
                <td>{item.name}</td>
                <td>{item.description || '-'}</td>
                {isAdmin && (
                  <td className="d-flex gap-2">
                    <button
                      className="btn btn-outline-primary btn-sm"
                      onClick={() => handleEdit(item)}
                    >
                      Update
                    </button>
                    <button
                      className="btn btn-outline-danger btn-sm"
                      onClick={() => handleDelete(item.ci_id)}
                    >
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {items.length === 0 && (
              <tr>
                <td colSpan={isAdmin ? 4 : 3} className="text-center">
                  No configuration items yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
