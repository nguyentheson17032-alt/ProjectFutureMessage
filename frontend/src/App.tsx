import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { GuestOnly, RequireAuth } from './auth/RequireAuth'
import { Shell } from './components/Shell'
import { ComposePage } from './pages/ComposePage'
import { EditMessagePage } from './pages/EditMessagePage'
import { InboxPage } from './pages/InboxPage'
import { LandingPage } from './pages/LandingPage'
import { LoginPage } from './pages/LoginPage'
import { MessageDetailPage } from './pages/MessageDetailPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { RegisterPage } from './pages/RegisterPage'
import { SentPage } from './pages/SentPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Shell />}>
            <Route path="/" element={<LandingPage />} />
            <Route
              path="/login"
              element={
                <GuestOnly>
                  <LoginPage />
                </GuestOnly>
              }
            />
            <Route
              path="/register"
              element={
                <GuestOnly>
                  <RegisterPage />
                </GuestOnly>
              }
            />
            <Route element={<RequireAuth />}>
              <Route path="/inbox" element={<InboxPage />} />
              <Route path="/sent" element={<SentPage />} />
              <Route path="/compose" element={<ComposePage />} />
              <Route path="/messages/:id" element={<MessageDetailPage />} />
              <Route path="/messages/:id/edit" element={<EditMessagePage />} />
            </Route>
            <Route path="/home" element={<Navigate to="/" replace />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
