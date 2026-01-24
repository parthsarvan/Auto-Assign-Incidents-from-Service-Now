import React, { useEffect, useState } from 'react';
import { createConfigurationItem, deleteConfigurationItem, fetchConfigurationItems } from '../services/admin';

export default function AdminConfigurationItems() {
  const [items, setItems] = useState([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');

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
      await createConfigurationItem({ name, description });
      setName('');
      setDescription('');
      loadItems();
    } catch (err) {
      setError('Failed to create configuration item.');
    }
  };

  const handleDelete = async (id) => {
    await deleteConfigurationItem(id);
    loadItems();
  };

  return (
    <div className="container">
      <h4 className="mb-3">Manage Configuration Items</h4>
      {error && <div className="alert alert-danger">{error}</div>}

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
          <div className="col-12">
            <button type="submit" className="btn btn-primary">Add CI</button>
          </div>
        </form>
      </div>

      <div className="table-responsive">
        <table className="table table-bordered">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Description</th>
              <th style={{ width: '120px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.ci_id}>
                <td>{item.ci_id}</td>
                <td>{item.name}</td>
                <td>{item.description || '-'}</td>
                <td>
                  <button
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => handleDelete(item.ci_id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {items.length === 0 && (
              <tr>
                <td colSpan="4" className="text-center">No configuration items yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
