import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMe, logout } from '../api/auth';

export default function DashboardPage() {
  const [email, setEmail] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    getMe()
      .then(data => setEmail(data.email))
      .catch(() => navigate('/'));
  }, [navigate]);

  function handleLogout() {
    logout();
    navigate('/');
  }

  return (
    <div style={{ padding: '2rem' }}>
      <h2>Dashboard</h2>
      <p>Zalogowano jako: <strong>{email}</strong></p>
      <button
        onClick={handleLogout}
        style={{ padding: '8px 16px', background: '#ef4444', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
      >
        Wyloguj
      </button>
    </div>
  );
}
