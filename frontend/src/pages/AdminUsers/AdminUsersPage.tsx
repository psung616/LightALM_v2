import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createUser, deactivateUser, listUsers, updateUser } from '../../api/user';
import type { SystemRole } from '../../types/common';
import { Modal } from '../../components/Modal';

export function AdminUsersPage() {
  const queryClient = useQueryClient();
  const usersQuery = useQuery({ queryKey: ['users', 'all'], queryFn: () => listUsers(0, 200) });

  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({ username: '', password: '', email: '', fullName: '', systemRole: 'USER' as SystemRole });

  const createMutation = useMutation({
    mutationFn: () => createUser(createForm),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users', 'all'] });
      setShowCreate(false);
      setCreateForm({ username: '', password: '', email: '', fullName: '', systemRole: 'USER' });
    },
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: number) => deactivateUser(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users', 'all'] }),
  });

  const roleMutation = useMutation({
    mutationFn: ({ id, email, fullName, systemRole }: { id: number; email: string; fullName: string; systemRole: SystemRole }) =>
      updateUser(id, { email, fullName, systemRole }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users', 'all'] }),
  });

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">사용자 관리</h1>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover"
        >
          새 사용자
        </button>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-3 py-2">아이디</th>
              <th className="px-3 py-2">이메일</th>
              <th className="px-3 py-2">이름</th>
              <th className="px-3 py-2">시스템 역할</th>
              <th className="px-3 py-2">활성화</th>
              <th className="px-3 py-2" />
            </tr>
          </thead>
          <tbody>
            {usersQuery.data?.content.map((u) => (
              <tr key={u.id} className="border-b border-slate-100 last:border-0">
                <td className="px-3 py-2">{u.username}</td>
                <td className="px-3 py-2">{u.email}</td>
                <td className="px-3 py-2">{u.fullName}</td>
                <td className="px-3 py-2">
                  <select
                    value={u.systemRole}
                    onChange={(e) => roleMutation.mutate({ id: u.id, email: u.email, fullName: u.fullName, systemRole: e.target.value as SystemRole })}
                    className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                  >
                    <option value="USER">USER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </td>
                <td className="px-3 py-2">{u.enabled ? '활성' : '비활성'}</td>
                <td className="px-3 py-2 text-right">
                  {u.enabled && (
                    <button type="button" onClick={() => deactivateMutation.mutate(u.id)} className="text-xs text-red-500 hover:underline">
                      비활성화
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showCreate && (
        <Modal title="새 사용자" onClose={() => setShowCreate(false)}>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              createMutation.mutate();
            }}
          >
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">아이디</label>
              <input
                type="text"
                required
                value={createForm.username}
                onChange={(e) => setCreateForm({ ...createForm, username: e.target.value })}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">비밀번호</label>
              <input
                type="password"
                required
                minLength={8}
                value={createForm.password}
                onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">이메일</label>
              <input
                type="email"
                required
                value={createForm.email}
                onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">이름</label>
              <input
                type="text"
                required
                value={createForm.fullName}
                onChange={(e) => setCreateForm({ ...createForm, fullName: e.target.value })}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-4">
              <label className="mb-1 block text-sm text-slate-600">시스템 역할</label>
              <select
                value={createForm.systemRole}
                onChange={(e) => setCreateForm({ ...createForm, systemRole: e.target.value as SystemRole })}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              >
                <option value="USER">USER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </div>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="w-full rounded-md bg-primary px-3 py-2 text-sm font-medium text-white hover:bg-primary-hover disabled:opacity-50"
            >
              생성
            </button>
          </form>
        </Modal>
      )}
    </div>
  );
}
