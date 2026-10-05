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
import {
  createCustomFieldDefinition,
  deprecateCustomFieldDefinition,
  listCustomFieldDefinitionsForConfig,
  updateCustomFieldDefinition,
} from '../../api/customField';
import type { ProjectRole } from '../../types/common';
import type { CustomFieldDataType, CustomFieldTargetType } from '../../types/customField';
import { FullScreenLoader } from '../../components/FullScreenLoader';
import { FormLayoutSettingsTab } from './FormLayoutSettingsTab';
import { WorkflowRuleSettingsTab } from './WorkflowRuleSettingsTab';

type Tab = 'general' | 'members' | 'github' | 'jenkins' | 'fields' | 'layout' | 'workflow';

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
          ['fields', '필드'],
          ['layout', '폼 레이아웃'],
          ['workflow', '워크플로우'],
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
      {tab === 'fields' && <CustomFieldSettingsTab projectId={id} />}
      {tab === 'layout' && <FormLayoutSettingsTab projectId={id} />}
      {tab === 'workflow' && <WorkflowRuleSettingsTab projectId={id} />}
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

const CUSTOM_FIELD_TARGET_OPTIONS: [CustomFieldTargetType, string][] = [
  ['REQUIREMENT', '요구사항'],
  ['ISSUE', '이슈'],
  ['TEST_CASE', '테스트케이스'],
];

const CUSTOM_FIELD_DATA_TYPE_OPTIONS: CustomFieldDataType[] = [
  'TEXT',
  'NUMBER',
  'DATE',
  'BOOLEAN',
  'SINGLE_SELECT',
  'MULTI_SELECT',
];

function CustomFieldSettingsTab({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const [targetType, setTargetType] = useState<CustomFieldTargetType>('REQUIREMENT');
  const [newField, setNewField] = useState({
    fieldKey: '',
    label: '',
    dataType: 'TEXT' as CustomFieldDataType,
    required: false,
    defaultValue: '',
  });
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState({ label: '', required: false, defaultValue: '', displayOrder: 0 });

  const queryKey = ['project', projectId, 'config', 'custom-fields', targetType];
  const fieldsQuery = useQuery({
    queryKey,
    queryFn: () => listCustomFieldDefinitionsForConfig(projectId, targetType),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      createCustomFieldDefinition(projectId, {
        targetType,
        fieldKey: newField.fieldKey,
        label: newField.label,
        dataType: newField.dataType,
        required: newField.required,
        defaultValue: newField.defaultValue || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setNewField({ fieldKey: '', label: '', dataType: 'TEXT', required: false, defaultValue: '' });
    },
  });

  const updateMutation = useMutation({
    mutationFn: (fieldId: number) =>
      updateCustomFieldDefinition(projectId, fieldId, {
        label: editForm.label,
        required: editForm.required,
        defaultValue: editForm.defaultValue || undefined,
        displayOrder: editForm.displayOrder,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setEditingId(null);
    },
  });

  const deprecateMutation = useMutation({
    mutationFn: (fieldId: number) => deprecateCustomFieldDefinition(projectId, fieldId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  function startEdit(field: { id: number; label: string; required: boolean; defaultValue: string | null; displayOrder: number }) {
    setEditingId(field.id);
    setEditForm({
      label: field.label,
      required: field.required,
      defaultValue: field.defaultValue ?? '',
      displayOrder: field.displayOrder,
    });
  }

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="mb-4 flex gap-1 border-b border-slate-200 text-sm">
        {CUSTOM_FIELD_TARGET_OPTIONS.map(([key, label]) => (
          <button
            key={key}
            type="button"
            onClick={() => setTargetType(key)}
            className={`px-3 py-2 ${targetType === key ? 'border-b-2 border-slate-900 font-medium text-slate-900' : 'text-slate-500'}`}
          >
            {label}
          </button>
        ))}
      </div>

      <table className="mb-4 w-full text-sm">
        <thead className="border-b border-slate-200 text-left text-xs uppercase text-slate-500">
          <tr>
            <th className="py-2">필드명</th>
            <th className="py-2">키</th>
            <th className="py-2">타입</th>
            <th className="py-2">필수</th>
            <th className="py-2">상태</th>
            <th className="py-2" />
          </tr>
        </thead>
        <tbody>
          {fieldsQuery.data?.map((f) =>
            editingId === f.id ? (
              <tr key={f.id} className="border-b border-slate-100">
                <td className="py-2" colSpan={6}>
                  <div className="flex flex-wrap items-center gap-2">
                    <input
                      type="text"
                      value={editForm.label}
                      onChange={(e) => setEditForm({ ...editForm, label: e.target.value })}
                      placeholder="필드명"
                      className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                    />
                    <input
                      type="text"
                      value={editForm.defaultValue}
                      onChange={(e) => setEditForm({ ...editForm, defaultValue: e.target.value })}
                      placeholder="기본값"
                      className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                    />
                    <input
                      type="number"
                      value={editForm.displayOrder}
                      onChange={(e) => setEditForm({ ...editForm, displayOrder: Number(e.target.value) })}
                      placeholder="순서"
                      className="w-20 rounded-md border border-slate-300 px-2 py-1 text-sm"
                    />
                    <label className="flex items-center gap-1 text-xs text-slate-600">
                      <input
                        type="checkbox"
                        checked={editForm.required}
                        onChange={(e) => setEditForm({ ...editForm, required: e.target.checked })}
                      />
                      필수
                    </label>
                    <button
                      type="button"
                      onClick={() => updateMutation.mutate(f.id)}
                      disabled={updateMutation.isPending}
                      className="rounded-md bg-primary px-3 py-1 text-xs text-white hover:bg-primary-hover disabled:opacity-50"
                    >
                      저장
                    </button>
                    <button type="button" onClick={() => setEditingId(null)} className="rounded-md border border-slate-300 px-3 py-1 text-xs">
                      취소
                    </button>
                  </div>
                </td>
              </tr>
            ) : (
              <tr key={f.id} className="border-b border-slate-100 last:border-0">
                <td className="py-2">{f.label}</td>
                <td className="py-2 text-slate-500">{f.fieldKey}</td>
                <td className="py-2 text-slate-500">{f.dataType}</td>
                <td className="py-2 text-slate-500">{f.required ? 'Y' : 'N'}</td>
                <td className="py-2">
                  <span
                    className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
                      f.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-slate-200 text-slate-600'
                    }`}
                  >
                    {f.status}
                  </span>
                </td>
                <td className="py-2 text-right">
                  <div className="flex justify-end gap-2">
                    <button type="button" onClick={() => startEdit(f)} className="text-xs text-slate-500 hover:underline">
                      편집
                    </button>
                    {f.status === 'ACTIVE' && (
                      <button
                        type="button"
                        onClick={() => {
                          if (window.confirm(`'${f.label}' 필드를 비활성화하시겠습니까? (하드 삭제가 아니며, 기존 값은 보존됩니다)`)) {
                            deprecateMutation.mutate(f.id);
                          }
                        }}
                        className="text-xs text-red-500 hover:underline"
                      >
                        비활성화
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ),
          )}
          {fieldsQuery.data?.length === 0 && (
            <tr>
              <td colSpan={6} className="py-3 text-center text-sm text-slate-400">
                등록된 필드가 없습니다.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      <div className="rounded-md bg-slate-50 p-3">
        <p className="mb-2 text-xs font-medium uppercase text-slate-500">필드 추가</p>
        <div className="flex flex-wrap items-center gap-2">
          <input
            type="text"
            value={newField.fieldKey}
            onChange={(e) => setNewField({ ...newField, fieldKey: e.target.value })}
            placeholder="필드 키 (예: severity_custom)"
            className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
          />
          <input
            type="text"
            value={newField.label}
            onChange={(e) => setNewField({ ...newField, label: e.target.value })}
            placeholder="표시명"
            className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
          />
          <select
            value={newField.dataType}
            onChange={(e) => setNewField({ ...newField, dataType: e.target.value as CustomFieldDataType })}
            className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
          >
            {CUSTOM_FIELD_DATA_TYPE_OPTIONS.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
          <input
            type="text"
            value={newField.defaultValue}
            onChange={(e) => setNewField({ ...newField, defaultValue: e.target.value })}
            placeholder="기본값(선택)"
            className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
          />
          <label className="flex items-center gap-1 text-xs text-slate-600">
            <input
              type="checkbox"
              checked={newField.required}
              onChange={(e) => setNewField({ ...newField, required: e.target.checked })}
            />
            필수
          </label>
          <button
            type="button"
            disabled={!newField.fieldKey || !newField.label || createMutation.isPending}
            onClick={() => createMutation.mutate()}
            className="rounded-md bg-primary px-3 py-1.5 text-sm text-white hover:bg-primary-hover disabled:opacity-50"
          >
            추가
          </button>
        </div>
        {(newField.dataType === 'SINGLE_SELECT' || newField.dataType === 'MULTI_SELECT') && (
          <p className="mt-2 text-xs text-slate-400">
            SINGLE_SELECT/MULTI_SELECT의 선택지(열거형 집합) 관리는 추후(Phase 22 열거형 설정)에서 지원됩니다.
          </p>
        )}
        {createMutation.isError && (
          <p className="mt-2 text-xs text-red-600">필드 추가에 실패했습니다. 이미 같은 키가 있는지 확인해주세요.</p>
        )}
      </div>
    </div>
  );
}
