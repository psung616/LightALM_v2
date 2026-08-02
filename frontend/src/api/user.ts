import { apiClient } from './client';
import type { PageResponse, SystemRole } from '../types/common';
import type { User } from '../types/user';

export interface CreateUserRequest {
  username: string;
  password: string;
  email: string;
  fullName: string;
  systemRole?: SystemRole;
}

export interface UpdateUserRequest {
  email: string;
  fullName: string;
  systemRole?: SystemRole;
  enabled?: boolean;
}

export async function listUsers(page = 0, size = 20): Promise<PageResponse<User>> {
  const { data } = await apiClient.get<PageResponse<User>>('/users', { params: { page, size } });
  return data;
}

export async function getUser(id: number): Promise<User> {
  const { data } = await apiClient.get<User>(`/users/${id}`);
  return data;
}

export async function createUser(request: CreateUserRequest): Promise<User> {
  const { data } = await apiClient.post<User>('/users', request);
  return data;
}

export async function updateUser(id: number, request: UpdateUserRequest): Promise<User> {
  const { data } = await apiClient.put<User>(`/users/${id}`, request);
  return data;
}

export async function deactivateUser(id: number): Promise<void> {
  await apiClient.delete(`/users/${id}`);
}
