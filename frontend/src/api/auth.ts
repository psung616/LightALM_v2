import { apiClient } from './client';
import type { User } from '../types/user';

export interface SignupRequest {
  username: string;
  email: string;
  fullName: string;
  password: string;
  passwordConfirm: string;
}

export async function login(username: string, password: string): Promise<User> {
  const { data } = await apiClient.post<User>('/auth/login', { username, password });
  return data;
}

export async function signup(request: SignupRequest): Promise<User> {
  const { data } = await apiClient.post<User>('/auth/signup', request);
  return data;
}

export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout');
}

export async function fetchMe(): Promise<User> {
  const { data } = await apiClient.get<User>('/auth/me');
  return data;
}

export async function changeMyPassword(oldPassword: string, newPassword: string): Promise<void> {
  await apiClient.put('/users/me/password', { oldPassword, newPassword });
}
