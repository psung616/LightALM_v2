import type { SystemRole } from './common';

export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  systemRole: SystemRole;
  enabled: boolean;
}
