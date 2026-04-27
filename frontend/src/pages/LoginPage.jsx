import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../api/auth';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      await login(email, password);
      navigate('/dashboard');
    } catch {
      setError('Nieprawidłowy email lub hasło');
    }
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#f5f5f5' }}>
      <form
        onSubmit={handleSubmit}
        style={{ display: 'flex', flexDirection: 'column', gap: '12px', width: '320px', background: '#fff', padding: '2rem', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}
      >
        <h2 style={{ margin: 0 }}>Bakelivery</h2>
        <p style={{ margin: 0, color: '#666' }}>Zaloguj się do swojego konta</p>
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={e => setEmail(e.target.value)}
          required
          style={{ padding: '10px', borderRadius: '4px', border: '1px solid #ccc', fontSize: '14px' }}
        />
        <input
          type="password"
          placeholder="Hasło"
          value={password}
          onChange={e => setPassword(e.target.value)}
          required
          style={{ padding: '10px', borderRadius: '4px', border: '1px solid #ccc', fontSize: '14px' }}
        />
        {error && <p style={{ color: 'red', margin: 0, fontSize: '14px' }}>{error}</p>}
        <button
          type="submit"
          style={{ padding: '10px', background: '#d97706', color: '#fff', border: 'none', borderRadius: '4px', fontSize: '14px', cursor: 'pointer' }}
        >
          Zaloguj się
        </button>
      </form>
    </div>
  );
}
