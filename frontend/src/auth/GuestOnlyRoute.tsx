import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { FullScreenLoader } from '../components/FullScreenLoader';

export function GuestOnlyRoute() {
  const { user, loading } = useAuth();

  if (loading) {
    return <FullScreenLoader />;
  }
  if (user) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
