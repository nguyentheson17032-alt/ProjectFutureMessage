import { lazy, Suspense, ViewTransition, type ComponentType, type ReactNode } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ApiCacheProvider } from './api/cache'
import { AuthProvider } from './auth/AuthContext'
import { RequireAdmin } from './auth/RequireAdmin'
import { GuestOnly, RequireAuth } from './auth/RequireAuth'
import { PageState } from './components/EmptyState'
import { Shell } from './components/Shell'

const LandingPage = lazy(() => import('./pages/LandingPage').then((m) => ({ default: m.LandingPage })))
const LoginPage = lazy(() => import('./pages/LoginPage').then((m) => ({ default: m.LoginPage })))
const RegisterPage = lazy(() =>
  import('./pages/RegisterPage').then((m) => ({ default: m.RegisterPage })),
)
const ComposePage = lazy(() => import('./pages/ComposePage').then((m) => ({ default: m.ComposePage })))
const InboxPage = lazy(() => import('./pages/InboxPage').then((m) => ({ default: m.InboxPage })))
const SentPage = lazy(() => import('./pages/SentPage').then((m) => ({ default: m.SentPage })))
const MessageDetailPage = lazy(() =>
  import('./pages/MessageDetailPage').then((m) => ({ default: m.MessageDetailPage })),
)
const EditMessagePage = lazy(() =>
  import('./pages/EditMessagePage').then((m) => ({ default: m.EditMessagePage })),
)
const NotFoundPage = lazy(() =>
  import('./pages/NotFoundPage').then((m) => ({ default: m.NotFoundPage })),
)
const AdminLayout = lazy(() =>
  import('./pages/admin/AdminLayout').then((m) => ({ default: m.AdminLayout })),
)
const AdminDashboardPage = lazy(() =>
  import('./pages/admin/AdminDashboardPage').then((m) => ({ default: m.AdminDashboardPage })),
)
const AdminUsersPage = lazy(() =>
  import('./pages/admin/AdminUsersPage').then((m) => ({ default: m.AdminUsersPage })),
)
const AdminUserDetailPage = lazy(() =>
  import('./pages/admin/AdminUserDetailPage').then((m) => ({ default: m.AdminUserDetailPage })),
)
const AdminMessagesPage = lazy(() =>
  import('./pages/admin/AdminMessagesPage').then((m) => ({ default: m.AdminMessagesPage })),
)
const AdminMessageDetailPage = lazy(() =>
  import('./pages/admin/AdminMessageDetailPage').then((m) => ({ default: m.AdminMessageDetailPage })),
)

function RouteFallback() {
  return (
    <ViewTransition exit="slide-down">
      <PageState>Đang tải…</PageState>
    </ViewTransition>
  )
}

function LazyPage({ children }: { children: ReactNode }) {
  return <Suspense fallback={<RouteFallback />}>{children}</Suspense>
}

function Guest({ Page }: { Page: ComponentType }) {
  return (
    <GuestOnly>
      <LazyPage>
        <Page />
      </LazyPage>
    </GuestOnly>
  )
}

export default function App() {
  return (
    <ApiCacheProvider>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route element={<Shell />}>
              <Route
                path="/"
                element={
                  <LazyPage>
                    <LandingPage />
                  </LazyPage>
                }
              />
              <Route path="/login" element={<Guest Page={LoginPage} />} />
              <Route path="/register" element={<Guest Page={RegisterPage} />} />
              <Route element={<RequireAuth />}>
                <Route
                  path="/inbox"
                  element={
                    <LazyPage>
                      <InboxPage />
                    </LazyPage>
                  }
                />
                <Route
                  path="/sent"
                  element={
                    <LazyPage>
                      <SentPage />
                    </LazyPage>
                  }
                />
                <Route
                  path="/compose"
                  element={
                    <LazyPage>
                      <ComposePage />
                    </LazyPage>
                  }
                />
                <Route
                  path="/messages/:id"
                  element={
                    <LazyPage>
                      <MessageDetailPage />
                    </LazyPage>
                  }
                />
                <Route
                  path="/messages/:id/edit"
                  element={
                    <LazyPage>
                      <EditMessagePage />
                    </LazyPage>
                  }
                />
              </Route>
              <Route element={<RequireAdmin />}>
                <Route
                  path="/admin"
                  element={
                    <LazyPage>
                      <AdminLayout />
                    </LazyPage>
                  }
                >
                  <Route
                    index
                    element={
                      <LazyPage>
                        <AdminDashboardPage />
                      </LazyPage>
                    }
                  />
                  <Route
                    path="users"
                    element={
                      <LazyPage>
                        <AdminUsersPage />
                      </LazyPage>
                    }
                  />
                  <Route
                    path="users/:id"
                    element={
                      <LazyPage>
                        <AdminUserDetailPage />
                      </LazyPage>
                    }
                  />
                  <Route
                    path="messages"
                    element={
                      <LazyPage>
                        <AdminMessagesPage />
                      </LazyPage>
                    }
                  />
                  <Route
                    path="messages/:id"
                    element={
                      <LazyPage>
                        <AdminMessageDetailPage />
                      </LazyPage>
                    }
                  />
                </Route>
              </Route>
              <Route path="/home" element={<Navigate to="/" replace />} />
              <Route
                path="*"
                element={
                  <LazyPage>
                    <NotFoundPage />
                  </LazyPage>
                }
              />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ApiCacheProvider>
  )
}
