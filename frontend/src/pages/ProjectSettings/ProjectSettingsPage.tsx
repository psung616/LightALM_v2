import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getProject,
  listMembers,
  addMember,
  updateMemberRole,
  removeMember,
  updateProject,
  updateGithubIntegration,
  updateJenkinsIntegration,
} from '../../api/project';
import { listUsers } from '../../api/user';
import type { ProjectRole } from '../../types/common';
import { FullScreenLoader } from '../../components/FullScreenLoader';

type Tab = 'general' | 'members' | 'github' | 'jenkins';

export function ProjectSettingsPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const id = Number(projectId);
  const [tab, setTab] = useState<Tab>('general');

  const projectQuery = useQuery({ queryKey: ['project', id], queryFn: () => getProject(id), enabled: Number.isFinite(id) });

  if (projectQuery.isLoading) return <FullScreenLoader />;
  if (projectQuery.isError || !projectQuery.data) return <p className="text-sm text-red-600">프로젝트를 불러오지 못했습니다.</p>;

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-4 text-xl font-semibold text-slate-900">프로젝트 설정</h1>
      <div className="mb-6 flex gap-1 border-b border-slate-200 text-sm">
        {([
          ['general', '일반 정보'],
          ['members', '멤버 관리'],
          ['github', 'GitHub 연동'],
          ['jenkins', 'Jenkins 연동'],
        ] as [Tab, string][]).map(([key, label]) => (
          <button
            key={key}
            type="button"
            onClick={() => setTab(key)}
            className={`px-3 py-2 ${tab === key ? 'border-b-2 border-slate-900 font-medium text-slate-900' : 'text-slate-500'}`}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'general' && <GeneralTab projectId={id} name={projectQuery.data.name} description={projectQuery.data.description ?? ''} status={projectQuery.data.status} />}
      {tab === 'members' && <MembersTab projectId={id} />}
      {tab === 'github' && (
        <GithubTab
          projectId={id}
          repoOwner={projectQuery.data.githubRepoOwner ?? ''}
          repoName={projectQuery.data.githubRepoName ?? ''}
          webhookSecretMasked={projectQuery.data.githubWebhookSecretMasked}
        />
      )}
      {tab === 'jenkins' && (
        <JenkinsTab
          projectId={id}
          baseUrl={projectQuery.data.jenkinsBaseUrl ?? ''}
          jobName={projectQuery.data.jenkinsJobName ?? ''}
          apiUser={projectQuery.data.jenkinsApiUser ?? ''}
        />
      )}
    </div>
  );
}

function GeneralTab({ projectId, name, description, status }: { projectId: number; name: string; description: string; status: string }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({ name, description, status });

  const mutation = useMutation({
    mutationFn: () => updateProject(projectId, form),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['project', projectId] }),
  });

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="mb-3">
        <label className="mb-1 block text-sm text-slate-600">프로젝트명</label>
        <input
          type="text"
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
      </div>
      <div className="mb-3">
        <label className="mb-1 block text-sm text-slate-600">설명</label>
        <textarea
          value={form.description}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
          rows={3}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
      </div>
      <div className="mb-4">
        <label className="mb-1 block text-sm text-slate-600">상태</label>
        <select
          value={form.status}
          onChange={(e) => setForm({ ...form, status: e.target.value })}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        >
          <option value="ACTIVE">ACTIVE</option>
          <option value="ARCHIVED">ARCHIVED</option>
        </select>
      </div>
      <button
        type="button"
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending}
        className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover disabled:opacity-50"
      >
        저장
      </button>
    </div>
  );
}

function MembersTab({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const membersQuery = useQuery({ queryKey: ['project', projectId, 'members'], queryFn: () => listMembers(projectId) });
  const usersQuery = useQuery({ queryKey: ['users', 'all'], queryFn: () => listUsers(0, 200), retry: false });

  const [userId, setUserId] = useState('');
  const [role, setRole] = useState<ProjectRole>('MEMBER');

  const addMutation = useMutation({
    mutationFn: () => addMember(projectId, Number(userId), role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', projectId, 'members'] });
      setUserId('');
    },
  });

  const roleMutation = useMutation({
    mutationFn: ({ uid, r }: { uid: number; r: ProjectRole }) => updateMemberRole(projectId, uid, r),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['project', projectId, 'members'] }),
  });

  const removeMutation = useMutation({
    mutationFn: (uid: number) => removeMember(projectId, uid),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['project', projectId, 'members'] }),
  });

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <table className="mb-4 w-full text-sm">
        <thead className="border-b border-slate-200 text-left text-xs uppercase text-slate-500">
          <tr>
            <th className="py-2">이름</th>
            <th className="py-2">아이디</th>
            <th className="py-2">역할</th>
            <th className="py-2" />
          </tr>
        </thead>
        <tbody>
          {membersQuery.data?.map((m) => (
            <tr key={m.id} className="border-b border-slate-100 last:border-0">
              <td className="py-2">{m.fullName}</td>
              <td className="py-2">{m.username}</td>
              <td className="py-2">
                <select
                  value={m.role}
                  onChange={(e) => roleMutation.mutate({ uid: m.userId, r: e.target.value as ProjectRole })}
                  className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                >
                  <option value="VIEWER">VIEWER</option>
                  <option value="MEMBER">MEMBER</option>
                  <option value="PROJECT_ADMIN">PROJECT_ADMIN</option>
                </select>
              </td>
              <td className="py-2 text-right">
                <button type="button" onClick={() => removeMutation.mutate(m.userId)} className="text-xs text-red-500 hover:underline">
                  제거
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="flex items-center gap-2">
        {usersQuery.data ? (
          <select value={userId} onChange={(e) => setUserId(e.target.value)} className="flex-1 rounded-md border border-slate-300 px-2 py-1.5 text-sm">
            <option value="">사용자 선택</option>
            {usersQuery.data.content
              .filter((u) => !membersQuery.data?.some((m) => m.userId === u.id))
              .map((u) => (
                <option key={u.id} value={u.id}>{u.fullName} ({u.username})</option>
              ))}
          </select>
        ) : (
          <input
            type="number"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            placeholder="사용자 ID 직접 입력"
            className="flex-1 rounded-md border border-slate-300 px-2 py-1.5 text-sm"
          />
        )}
        <select value={role} onChange={(e) => setRole(e.target.value as ProjectRole)} className="rounded-md border border-slate-300 px-2 py-1.5 text-sm">
          <option value="VIEWER">VIEWER</option>
          <option value="MEMBER">MEMBER</option>
          <option value="PROJECT_ADMIN">PROJECT_ADMIN</option>
        </select>
        <button
          type="button"
          disabled={!userId || addMutation.isPending}
          onClick={() => addMutation.mutate()}
          className="rounded-md bg-primary px-3 py-1.5 text-sm text-white hover:bg-primary-hover disabled:opacity-50"
        >
          추가
        </button>
      </div>
    </div>
  );
}

function GithubTab({ projectId, repoOwner, repoName, webhookSecretMasked }: { projectId: number; repoOwner: string; repoName: string; webhookSecretMasked: string | null }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({ repoOwner, repoName, accessToken: '', webhookSecret: '' });

  const mutation = useMutation({
    mutationFn: () =>
      updateGithubIntegration(projectId, {
        repoOwner: form.repoOwner,
        repoName: form.repoName,
        accessToken: form.accessToken || undefined,
        webhookSecret: form.webhookSecret || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      setForm((f) => ({ ...f, accessToken: '', webhookSecret: '' }));
    },
  });

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="mb-3 grid grid-cols-2 gap-3">
        <div>
          <label className="mb-1 block text-sm text-slate-600">repoOwner</label>
          <input
            type="text"
            value={form.repoOwner}
            onChange={(e) => setForm({ ...form, repoOwner: e.target.value })}
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="mb-1 block text-sm text-slate-600">repoName</label>
          <input
            type="text"
            value={form.repoName}
            onChange={(e) => setForm({ ...form, repoName: e.target.value })}
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
        </div>
      </div>
      <div className="mb-3">
        <label className="mb-1 block text-sm text-slate-600">Personal Access Token {webhookSecretMasked && <span className="text-slate-400">(설정됨)</span>}</label>
        <input
          type="password"
          value={form.accessToken}
          onChange={(e) => setForm({ ...form, accessToken: e.target.value })}
          placeholder="변경하려면 입력"
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
      </div>
      <div className="mb-4">
        <label className="mb-1 block text-sm text-slate-600">Webhook Secret</label>
        <input
          type="password"
          value={form.webhookSecret}
          onChange={(e) => setForm({ ...form, webhookSecret: e.target.value })}
          placeholder="변경하려면 입력"
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
      </div>
      <p className="mb-4 rounded-md bg-slate-50 px-3 py-2 text-xs text-slate-500">
        GitHub 저장소 Settings → Webhooks에서 Payload URL을 <code>{`{서버주소}/api/webhooks/github/${projectId}`}</code>로, Secret을 위 값과 동일하게 등록하세요.
      </p>
      <button
        type="button"
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending}
        className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover disabled:opacity-50"
      >
        저장
      </button>
    </div>
  );
}

function JenkinsTab({ projectId, baseUrl, jobName, apiUser }: { projectId: number; baseUrl: string; jobName: string; apiUser: string }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({ baseUrl, jobName, apiUser, apiToken: '' });

  const mutation = useMutation({
    mutationFn: () =>
      updateJenkinsIntegration(projectId, {
        baseUrl: form.baseUrl,
        jobName: form.jobName,
        apiUser: form.apiUser || undefined,
        apiToken: form.apiToken || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      setForm((f) => ({ ...f, apiToken: '' }));
    },
  });

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="mb-3">
        <label className="mb-1 block text-sm text-slate-600">baseUrl</label>
        <input
          type="text"
          value={form.baseUrl}
          onChange={(e) => setForm({ ...form, baseUrl: e.target.value })}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
      </div>
      <div className="mb-3">
        <label className="mb-1 block text-sm text-slate-600">jobName</label>
        <input
          type="text"
          value={form.jobName}
          onChange={(e) => setForm({ ...form, jobName: e.target.value })}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
      </div>
      <div className="mb-3 grid grid-cols-2 gap-3">
        <div>
          <label className="mb-1 block text-sm text-slate-600">apiUser</label>
          <input
            type="text"
            value={form.apiUser}
            onChange={(e) => setForm({ ...form, apiUser: e.target.value })}
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="mb-1 block text-sm text-slate-600">apiToken</label>
          <input
            type="password"
            value={form.apiToken}
            onChange={(e) => setForm({ ...form, apiToken: e.target.value })}
            placeholder="변경하려면 입력"
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
        </div>
      </div>
      <p className="mb-4 rounded-md bg-slate-50 px-3 py-2 text-xs text-slate-500">
        Jenkins Job의 Post-build Action에서 <code>{`/api/webhooks/jenkins/${projectId}`}</code>로 빌드 결과를 POST하도록 구성하세요(헤더 X-Jenkins-Token 필요).
      </p>
      <button
        type="button"
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending}
        className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover disabled:opacity-50"
      >
        저장
      </button>
    </div>
  );
}
