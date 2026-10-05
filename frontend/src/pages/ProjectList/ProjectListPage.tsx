import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { listProjects } from '../../api/project';
import { StatusBadge } from '../../components/Badge';
import { useAuth } from '../../auth/AuthContext';

/** ADR-015 D6: 프로젝트 생성은 System Admin(ADMIN) 전용 — "새 프로젝트" 버튼과 생성 안내문은 ADMIN에게만 보인다. */
export function ProjectListPage() {
  const { user } = useAuth();
  const isSystemAdmin = user?.systemRole === 'ADMIN';
  const projectsQuery = useQuery({
    queryKey: ['projects'],
    queryFn: () => listProjects(0, 100),
  });

  return (
    <div className="mx-auto max-w-5xl">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">프로젝트 목록</h1>
        {isSystemAdmin && (
          <Link
            to="/projects/new"
            className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover"
          >
            새 프로젝트
          </Link>
        )}
      </div>

      {projectsQuery.isLoading && <p className="text-sm text-slate-500">불러오는 중...</p>}
      {projectsQuery.isError && <p className="text-sm text-red-600">프로젝트 목록을 불러오지 못했습니다.</p>}

      {projectsQuery.data && projectsQuery.data.content.length === 0 && (
        <p className="text-sm text-slate-500">
          {isSystemAdmin
            ? '접근 가능한 프로젝트가 없습니다. 새 프로젝트를 만들어 보세요.'
            : '참여 중인 프로젝트가 없습니다. 시스템 관리자나 프로젝트 관리자에게 멤버 추가를 요청하세요.'}
        </p>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {projectsQuery.data?.content.map((project) => (
          <Link
            key={project.id}
            to={`/projects/${project.id}`}
            className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition hover:border-slate-400 hover:shadow"
          >
            <div className="mb-2 flex items-center justify-between">
              <span className="text-xs font-medium uppercase text-slate-400">{project.projectKey}</span>
              <StatusBadge status={project.status} />
            </div>
            <p className="font-semibold text-slate-900">{project.name}</p>
            {project.description && <p className="mt-1 line-clamp-2 text-sm text-slate-500">{project.description}</p>}
          </Link>
        ))}
      </div>
    </div>
  );
}
