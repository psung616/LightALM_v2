import { Link, NavLink, Outlet, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getProject, listMembers } from '../api/project';
import { useAuth } from '../auth/AuthContext';
import { FullScreenLoader } from './FullScreenLoader';

const navItemClass = ({ isActive }: { isActive: boolean }) =>
  `block rounded-md px-3 py-2 text-sm ${isActive ? 'bg-primary text-white' : 'text-slate-600 hover:bg-slate-100'}`;

export function ProjectLayout() {
  const { projectId } = useParams<{ projectId: string }>();
  const id = Number(projectId);
  const { user, logout } = useAuth();

  const projectQuery = useQuery({
    queryKey: ['project', id],
    queryFn: () => getProject(id),
    enabled: Number.isFinite(id),
  });

  const membersQuery = useQuery({
    queryKey: ['project', id, 'members'],
    queryFn: () => listMembers(id),
    enabled: Number.isFinite(id),
  });

  if (projectQuery.isLoading) {
    return <FullScreenLoader />;
  }
  if (projectQuery.isError || !projectQuery.data) {
    return (
      <div className="flex h-screen items-center justify-center">
        <p className="text-slate-600">프로젝트를 불러올 수 없습니다.</p>
      </div>
    );
  }

  const project = projectQuery.data;
  const myRole = membersQuery.data?.find((m) => m.userId === user?.id)?.role;
  const canManageSettings = user?.systemRole === 'ADMIN' || myRole === 'PROJECT_ADMIN';

  return (
    <div className="flex min-h-screen bg-slate-50">
      <aside className="flex w-60 flex-col border-r border-slate-200 bg-white px-4 py-5">
        <Link to="/" className="mb-4 text-sm text-slate-500 hover:text-slate-800">
          ← 프로젝트 목록
        </Link>
        <div className="mb-6">
          <p className="text-xs font-medium uppercase text-slate-400">{project.projectKey}</p>
          <p className="truncate text-base font-semibold text-slate-900">{project.name}</p>
        </div>
        <nav className="flex flex-1 flex-col gap-1">
          <NavLink to={`/projects/${id}`} end className={navItemClass}>
            대시보드
          </NavLink>
          <NavLink to={`/projects/${id}/requirements`} className={navItemClass}>
            요구사항
          </NavLink>
          <NavLink to={`/projects/${id}/issues`} className={navItemClass}>
            이슈
          </NavLink>
          <NavLink to={`/projects/${id}/traceability`} className={navItemClass}>
            추적성
          </NavLink>
          <NavLink to={`/projects/${id}/test-cases`} className={navItemClass}>
            테스트케이스
          </NavLink>
          <NavLink to={`/projects/${id}/test-runs`} className={navItemClass}>
            테스트런
          </NavLink>
          <NavLink to={`/projects/${id}/releases`} className={navItemClass}>
            릴리스
          </NavLink>
          {canManageSettings && (
            <NavLink to={`/projects/${id}/approvals`} className={navItemClass}>
              승인함
            </NavLink>
          )}
          {canManageSettings && (
            <NavLink to={`/projects/${id}/settings`} className={navItemClass}>
              설정
            </NavLink>
          )}
        </nav>
        <div className="mt-6 border-t border-slate-200 pt-4">
          <p className="mb-2 truncate text-sm text-slate-600">{user?.fullName}</p>
          <button
            type="button"
            onClick={() => void logout()}
            className="w-full rounded-md border border-slate-300 px-3 py-1 text-sm text-slate-700 hover:bg-slate-100"
          >
            로그아웃
          </button>
        </div>
      </aside>
      <main className="flex-1 px-8 py-6">
        <Outlet />
      </main>
    </div>
  );
}
