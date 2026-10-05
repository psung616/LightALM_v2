import { useState } from 'react';
import axios from 'axios';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createBaseline, listBaselines } from '../../api/baseline';
import { listMembers } from '../../api/project';
import { listRequirements } from '../../api/requirement';
import { listIssues } from '../../api/issue';
import { listTestCases } from '../../api/testCase';
import { useAuth } from '../../auth/AuthContext';
import { Modal } from '../../components/Modal';
import type { ApiErrorResponse } from '../../types/common';
import { BASELINE_TARGET_LABEL } from '../../types/baseline';
import type { BaselineTargetType } from '../../types/baseline';

// ADR-008(Phase 16) / 05-frontend.md §5.12. 베이스라인 목록 + 생성(요구사항/이슈/테스트케이스 검색해 담기).

interface Candidate {
  targetType: BaselineTargetType;
  targetId: number;
  key: string;
  title: string;
}

const candidateId = (c: { targetType: BaselineTargetType; targetId: number }) => `${c.targetType}:${c.targetId}`;

function CreateBaselineModal({ projectId, onClose }: { projectId: number; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [searchType, setSearchType] = useState<BaselineTargetType>('REQUIREMENT');
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState<Candidate[]>([]);
  const [error, setError] = useState<string | null>(null);

  const searchQuery = useQuery({
    queryKey: ['baseline-candidates', projectId, searchType, keyword],
    queryFn: async (): Promise<Candidate[]> => {
      const filters = { keyword: keyword || undefined, size: 50 };
      if (searchType === 'REQUIREMENT') {
        const page = await listRequirements(projectId, filters);
        return page.content.map((r) => ({ targetType: 'REQUIREMENT', targetId: r.id, key: r.reqKey, title: r.title }));
      }
      if (searchType === 'ISSUE') {
        const page = await listIssues(projectId, filters);
        return page.content.map((i) => ({ targetType: 'ISSUE', targetId: i.id, key: i.issueKey, title: i.title }));
      }
      const page = await listTestCases(projectId, filters);
      return page.content.map((t) => ({ targetType: 'TEST_CASE', targetId: t.id, key: t.tcKey, title: t.title }));
    },
  });

  const createMutation = useMutation({
    mutationFn: () =>
      createBaseline(projectId, {
        name,
        description: description || undefined,
        itemRefs: selected.map(({ targetType, targetId }) => ({ targetType, targetId })),
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['baselines', projectId] });
      onClose();
    },
    onError: (err) => {
      if (axios.isAxiosError<ApiErrorResponse>(err) && err.response?.data?.message) {
        setError(err.response.data.message);
      } else {
        setError('베이스라인 생성에 실패했습니다.');
      }
    },
  });

  const selectedIds = new Set(selected.map(candidateId));

  function toggle(candidate: Candidate) {
    setSelected((prev) =>
      selectedIds.has(candidateId(candidate))
        ? prev.filter((c) => candidateId(c) !== candidateId(candidate))
        : [...prev, candidate],
    );
  }

  return (
    <Modal title="베이스라인 생성" onClose={onClose}>
      <div className="flex flex-col gap-3">
        {error && <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="이름 (예: v1.0 출시 기준선)"
          maxLength={150}
          className="rounded-md border border-slate-300 px-3 py-1.5 text-sm"
        />
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="설명(선택)"
          rows={2}
          className="rounded-md border border-slate-300 px-3 py-1.5 text-sm"
        />

        <div className="flex gap-2">
          <select
            value={searchType}
            onChange={(e) => setSearchType(e.target.value as BaselineTargetType)}
            className="rounded-md border border-slate-300 px-2 py-1 text-sm"
          >
            {(Object.keys(BASELINE_TARGET_LABEL) as BaselineTargetType[]).map((t) => (
              <option key={t} value={t}>{BASELINE_TARGET_LABEL[t]}</option>
            ))}
          </select>
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="키워드 검색"
            className="flex-1 rounded-md border border-slate-300 px-3 py-1 text-sm"
          />
        </div>
        <ul className="max-h-48 overflow-y-auto rounded-md border border-slate-200">
          {searchQuery.isLoading && <li className="px-3 py-2 text-sm text-slate-400">불러오는 중...</li>}
          {searchQuery.data?.length === 0 && <li className="px-3 py-2 text-sm text-slate-400">검색 결과가 없습니다.</li>}
          {searchQuery.data?.map((c) => (
            <li key={candidateId(c)} className="border-b border-slate-100 last:border-b-0">
              <label className="flex items-center gap-2 px-3 py-1.5 text-sm text-slate-700">
                <input type="checkbox" checked={selectedIds.has(candidateId(c))} onChange={() => toggle(c)} />
                <span className="font-mono text-xs text-slate-500">{c.key}</span>
                <span className="truncate">{c.title}</span>
              </label>
            </li>
          ))}
        </ul>
        <p className="text-xs text-slate-500">선택된 항목 {selected.length}개</p>

        <div className="flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-100"
          >
            취소
          </button>
          <button
            type="button"
            disabled={!name.trim() || selected.length === 0 || createMutation.isPending}
            onClick={() => createMutation.mutate()}
            className="rounded-md bg-primary px-3 py-1.5 text-sm text-white hover:bg-primary-hover disabled:opacity-50"
          >
            생성
          </button>
        </div>
      </div>
    </Modal>
  );
}

export function BaselineListPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const id = Number(projectId);
  const { user } = useAuth();
  const [showCreate, setShowCreate] = useState(false);

  const baselinesQuery = useQuery({
    queryKey: ['baselines', id],
    queryFn: () => listBaselines(id),
  });

  const membersQuery = useQuery({
    queryKey: ['project', id, 'members'],
    queryFn: () => listMembers(id),
  });

  const myRole = membersQuery.data?.find((m) => m.userId === user?.id)?.role;
  const canCreate = user?.systemRole === 'ADMIN' || myRole === 'PROJECT_ADMIN';

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">베이스라인</h1>
        {canCreate && (
          <button
            type="button"
            onClick={() => setShowCreate(true)}
            className="rounded-md bg-primary px-3 py-1.5 text-sm text-white hover:bg-primary-hover"
          >
            베이스라인 생성
          </button>
        )}
      </div>
      <p className="mb-4 text-sm text-slate-500">
        특정 시점의 요구사항/이슈/테스트케이스 값을 고정해두고, 이후 현재 값과 비교합니다.
      </p>

      <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-2">이름</th>
              <th className="px-4 py-2">설명</th>
              <th className="px-4 py-2">항목 수</th>
              <th className="px-4 py-2">생성자</th>
              <th className="px-4 py-2">생성일시</th>
            </tr>
          </thead>
          <tbody>
            {baselinesQuery.isLoading && (
              <tr><td colSpan={5} className="px-4 py-3 text-slate-400">불러오는 중...</td></tr>
            )}
            {baselinesQuery.data?.length === 0 && (
              <tr><td colSpan={5} className="px-4 py-3 text-slate-400">베이스라인이 없습니다.</td></tr>
            )}
            {baselinesQuery.data?.map((b) => (
              <tr key={b.id} className="border-t border-slate-100 hover:bg-slate-50">
                <td className="px-4 py-2">
                  <Link to={`/projects/${id}/baselines/${b.id}`} className="font-medium text-primary hover:underline">
                    {b.name}
                  </Link>
                </td>
                <td className="max-w-xs truncate px-4 py-2 text-slate-600">{b.description ?? ''}</td>
                <td className="px-4 py-2 text-slate-600">{b.itemCount}</td>
                <td className="px-4 py-2 text-slate-600">{b.createdByName ?? '-'}</td>
                <td className="px-4 py-2 text-slate-500">{new Date(b.createdAt).toLocaleString('ko-KR')}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showCreate && <CreateBaselineModal projectId={id} onClose={() => setShowCreate(false)} />}
    </div>
  );
}
