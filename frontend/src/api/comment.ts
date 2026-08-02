import { apiClient } from './client';
import type { Comment } from '../types/comment';

export type CommentTargetType = 'requirements' | 'issues';

export async function listComments(projectId: number, targetType: CommentTargetType, targetId: number): Promise<Comment[]> {
  const { data } = await apiClient.get<Comment[]>(`/projects/${projectId}/${targetType}/${targetId}/comments`);
  return data;
}

export async function createComment(projectId: number, targetType: CommentTargetType, targetId: number, content: string): Promise<Comment> {
  const { data } = await apiClient.post<Comment>(`/projects/${projectId}/${targetType}/${targetId}/comments`, { content });
  return data;
}

export async function deleteComment(projectId: number, commentId: number): Promise<void> {
  await apiClient.delete(`/projects/${projectId}/comments/${commentId}`);
}
