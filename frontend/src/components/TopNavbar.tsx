import { Link, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function TopNavbar() {
  const { user, logout } = useAuth();

  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-3">
        <div className="flex items-center gap-6">
          <Link to="/" className="text-lg font-semibold text-slate-900">
            Light ALM
          </Link>
          <Link to="/my-tasks" className="text-sm text-slate-600 hover:text-slate-900">
            내 작업
          </Link>
          {user?.systemRole === 'ADMIN' && (
            <Link to="/admin/users" className="text-sm text-slate-600 hover:text-slate-900">
              사용자 관리
            </Link>
          )}
        </div>
        <div className="flex items-center gap-3">
          <span className="text-sm text-slate-600">{user?.fullName}</span>
          <button
            type="button"
            onClick={() => void logout()}
            className="rounded-md border border-slate-300 px-3 py-1 text-sm text-slate-700 hover:bg-slate-100"
          >
            로그아웃
          </button>
        </div>
      </header>
      <main className="flex-1 px-6 py-6">
        <Outlet />
      </main>
    </div>
  );
}
