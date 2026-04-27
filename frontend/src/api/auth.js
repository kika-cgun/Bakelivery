export async function login(email, password) {
  const res = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) throw new Error('Invalid credentials');
  const data = await res.json();
  localStorage.setItem('token', data.token);
  localStorage.setItem('email', data.email);
  return data;
}

export async function getMe() {
  const token = localStorage.getItem('token');
  const res = await fetch('/api/auth/me', {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error('Unauthorized');
  return res.json();
}

export function logout() {
  localStorage.removeItem('token');
  localStorage.removeItem('email');
}

export function getToken() {
  return localStorage.getItem('token');
}
