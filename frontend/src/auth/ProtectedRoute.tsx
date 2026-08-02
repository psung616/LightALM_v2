import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { FullScreenLoader } from '../components/FullScreenLoader';

export function ProtectedRoute() {
  const { user, loading } = useAuth();

  if (loading) {
    return <FullScreenLoader />;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return <Outlet />;
}

export function AdminRoute() {
  const { user, loading } = useAuth();

  if (loading) {
    return <FullScreenLoader />;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (user.systemRole !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
