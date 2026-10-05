import { Link, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useTheme } from '../theme/ThemeContext';

export function TopNavbar() {
  const { user, logout } = useAuth();
  const { colorMode, toggleColorMode } = useTheme();

  return (
    <div className="flex min-h-screen flex-col bg-surface-muted">
      <header className="flex items-center justify-between border-b border-border bg-surface px-6 py-3">
        <div className="flex items-center gap-6">
          <Link to="/" className="text-lg font-semibold text-text">
            Light ALM
          </Link>
          <Link to="/my-tasks" className="text-sm text-text-muted hover:text-text">
            내 작업
          </Link>
          {user?.systemRole === 'ADMIN' && (
            <Link to="/admin/users" className="text-sm text-text-muted hover:text-text">
              사용자 관리
            </Link>
          )}
          {user?.systemRole === 'ADMIN' && (
            <Link to="/admin/licenses" className="text-sm text-text-muted hover:text-text">
              라이센스 관리
            </Link>
          )}
          {user?.systemRole === 'ADMIN' && (
            <Link to="/admin/theme" className="text-sm text-text-muted hover:text-text">
              테마 설정
            </Link>
          )}
        </div>
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={toggleColorMode}
            aria-label={colorMode === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'}
            title={colorMode === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'}
            className="rounded-md border border-border px-2 py-1 text-sm text-text-muted hover:bg-surface-muted"
          >
            {colorMode === 'dark' ? '🌙' : '☀️'}
          </button>
          <span className="text-sm text-text-muted">{user?.fullName}</span>
          <button
            type="button"
            onClick={() => void logout()}
            className="rounded-md border border-border px-3 py-1 text-sm text-text-muted hover:bg-surface-muted"
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
