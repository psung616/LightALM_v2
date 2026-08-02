import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { createProject } from '../../api/project';
import type { AxiosError } from 'axios';
import type { ApiErrorResponse } from '../../types/common';

export function ProjectNewPage() {
  const navigate = useNavigate();
  const [projectKey, setProjectKey] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const project = await createProject({ projectKey: projectKey.toUpperCase(), name, description: description || undefined });
      navigate(`/projects/${project.id}`);
    } catch (err) {
      const message = (err as AxiosError<ApiErrorResponse>).response?.data?.message;
      setError(message ?? '프로젝트 생성에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-lg">
      <h1 className="mb-6 text-xl font-semibold text-slate-900">새 프로젝트</h1>
      <form onSubmit={handleSubmit} className="rounded-lg border border-slate-200 bg-white p-6">
        {error && <p className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}
        <div className="mb-4">
          <label className="mb-1 block text-sm text-slate-600">프로젝트 키 (대문자 3~10자)</label>
          <input
            type="text"
            value={projectKey}
            onChange={(e) => setProjectKey(e.target.value.toUpperCase())}
            required
            maxLength={10}
            placeholder="LALM"
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
          />
        </div>
        <div className="mb-4">
          <label className="mb-1 block text-sm text-slate-600">프로젝트명</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
          />
        </div>
        <div className="mb-6">
          <label className="mb-1 block text-sm text-slate-600">설명</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
          />
        </div>
        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
        >
          {submitting ? '생성 중...' : '생성'}
        </button>
      </form>
    </div>
  );
}
