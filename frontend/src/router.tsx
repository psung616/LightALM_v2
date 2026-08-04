import { createBrowserRouter } from 'react-router-dom';
import { TopNavbar } from './components/TopNavbar';
import { ProjectLayout } from './components/ProjectLayout';
import { ProtectedRoute, AdminRoute } from './auth/ProtectedRoute';
import { GuestOnlyRoute } from './auth/GuestOnlyRoute';
import { LoginPage } from './pages/Login/LoginPage';
import { ProjectListPage } from './pages/ProjectList/ProjectListPage';
import { ProjectNewPage } from './pages/ProjectNew/ProjectNewPage';
import { ProjectDashboardPage } from './pages/ProjectDashboard/ProjectDashboardPage';
import { RequirementListPage } from './pages/RequirementList/RequirementListPage';
import { RequirementDetailPage } from './pages/RequirementDetail/RequirementDetailPage';
import { IssueListPage } from './pages/IssueList/IssueListPage';
import { IssueDetailPage } from './pages/IssueDetail/IssueDetailPage';
import { TraceabilityPage } from './pages/Traceability/TraceabilityPage';
import { ProjectSettingsPage } from './pages/ProjectSettings/ProjectSettingsPage';
import { AdminUsersPage } from './pages/AdminUsers/AdminUsersPage';
import { MyTasksPage } from './pages/MyTasks/MyTasksPage';
import { TestCaseListPage } from './pages/TestCaseList/TestCaseListPage';
import { TestCaseDetailPage } from './pages/TestCaseDetail/TestCaseDetailPage';
import { TestRunListPage } from './pages/TestRunList/TestRunListPage';
import { TestRunDetailPage } from './pages/TestRunDetail/TestRunDetailPage';
import { ReleaseListPage } from './pages/ReleaseList/ReleaseListPage';
import { ReleaseDetailPage } from './pages/ReleaseDetail/ReleaseDetailPage';
import { ApprovalInboxPage } from './pages/ApprovalInbox/ApprovalInboxPage';

export const router = createBrowserRouter([
  {
    element: <GuestOnlyRoute />,
    children: [{ path: '/login', element: <LoginPage /> }],
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <TopNavbar />,
        children: [
          { path: '/', element: <ProjectListPage /> },
          { path: '/projects/new', element: <ProjectNewPage /> },
          { path: '/my-tasks', element: <MyTasksPage /> },
          {
            element: <AdminRoute />,
            children: [{ path: '/admin/users', element: <AdminUsersPage /> }],
          },
        ],
      },
      {
        element: <ProjectLayout />,
        children: [
          { path: '/projects/:projectId', element: <ProjectDashboardPage /> },
          { path: '/projects/:projectId/requirements', element: <RequirementListPage /> },
          { path: '/projects/:projectId/requirements/:reqId', element: <RequirementDetailPage /> },
          { path: '/projects/:projectId/issues', element: <IssueListPage /> },
          { path: '/projects/:projectId/issues/:issueId', element: <IssueDetailPage /> },
          { path: '/projects/:projectId/traceability', element: <TraceabilityPage /> },
          { path: '/projects/:projectId/test-cases', element: <TestCaseListPage /> },
          { path: '/projects/:projectId/test-cases/:tcId', element: <TestCaseDetailPage /> },
          { path: '/projects/:projectId/test-runs', element: <TestRunListPage /> },
          { path: '/projects/:projectId/test-runs/:runId', element: <TestRunDetailPage /> },
          { path: '/projects/:projectId/releases', element: <ReleaseListPage /> },
          { path: '/projects/:projectId/releases/:releaseId', element: <ReleaseDetailPage /> },
          { path: '/projects/:projectId/approvals', element: <ApprovalInboxPage /> },
          { path: '/projects/:projectId/settings', element: <ProjectSettingsPage /> },
        ],
      },
    ],
  },
]);
