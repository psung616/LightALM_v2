export interface Comment {
  id: number;
  authorId: number | null;
  authorName: string | null;
  content: string;
  createdAt: string;
}
