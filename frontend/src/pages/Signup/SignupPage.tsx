import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { signup } from '../../api/auth';
import { getPublicLicenseStatus } from '../../api/license';
import type { ApiErrorResponse } from '../../types/common';

export function SignupPage() {
  const navigate = useNavigate();
  const [checkingLicense, setCheckingLicense] = useState(true);
  const [signupAllowed, setSignupAllowed] = useState(true);
  const [disabledReason, setDisabledReason] = useState<string | null>(null);

  const [form, setForm] = useState({ username: '', email: '', fullName: '', password: '', passwordConfirm: '' });
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const status = await getPublicLicenseStatus();
        setSignupAllowed(status.signupAllowed);
        setDisabledReason(status.reason);
      } catch {
        // 상태 조회 자체가 실패해도 폼은 노출한다(백엔드가 최종적으로 다시 검증한다).
        setSignupAllowed(true);
      } finally {
        setCheckingLicense(false);
      }
    })();
  }, []);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (form.password !== form.passwordConfirm) {
      setError('비밀번호와 비밀번호 확인이 일치하지 않습니다.');
      return;
    }

    setSubmitting(true);
    try {
      await signup(form);
      navigate('/login', { replace: true, state: { signupCompleted: true } });
    } catch (err) {
      if (axios.isAxiosError<ApiErrorResponse>(err) && err.response?.data?.message) {
        setError(err.response.data.message);
      } else {
        setError('회원가입에 실패했습니다.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (checkingLicense) {
    return null;
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50">
      <div className="w-full max-w-sm rounded-lg border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="mb-6 text-xl font-semibold text-slate-900">Light ALM 회원가입</h1>

        {!signupAllowed ? (
          <p className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-700">
            현재 가입이 비활성화되어 있습니다{disabledReason ? `(사유: ${disabledReason})` : ''}.
          </p>
        ) : (
          <form onSubmit={handleSubmit}>
            {error && <p className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}
            <div className="mb-4">
              <label className="mb-1 block text-sm text-slate-600">아이디</label>
              <input
                type="text"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                required
                autoFocus
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
              />
            </div>
            <div className="mb-4">
              <label className="mb-1 block text-sm text-slate-600">이메일</label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
              />
            </div>
            <div className="mb-4">
              <label className="mb-1 block text-sm text-slate-600">이름</label>
              <input
                type="text"
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                required
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
              />
            </div>
            <div className="mb-4">
              <label className="mb-1 block text-sm text-slate-600">비밀번호</label>
              <input
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                required
                minLength={8}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
              />
            </div>
            <div className="mb-6">
              <label className="mb-1 block text-sm text-slate-600">비밀번호 확인</label>
              <input
                type="password"
                value={form.passwordConfirm}
                onChange={(e) => setForm({ ...form, passwordConfirm: e.target.value })}
                required
                minLength={8}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
              />
            </div>
            <button
              type="submit"
              disabled={submitting}
              className="w-full rounded-md bg-primary px-3 py-2 text-sm font-medium text-white hover:bg-primary-hover disabled:opacity-50"
            >
              {submitting ? '가입 중...' : '회원가입'}
            </button>
          </form>
        )}

        <p className="mt-4 text-center text-sm text-slate-500">
          이미 계정이 있으신가요?{' '}
          <Link to="/login" className="text-primary hover:underline">
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}
