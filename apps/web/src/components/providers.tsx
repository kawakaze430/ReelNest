'use client';

import { AuthProvider } from 'react-oidc-context';
import type { ReactNode } from 'react';

const authority = process.env.NEXT_PUBLIC_AUTHORITY ?? 'http://localhost:8180/realms/reelnest';
const clientId = process.env.NEXT_PUBLIC_OIDC_CLIENT_ID ?? 'reelnest-web';
const appOrigin = process.env.NEXT_PUBLIC_APP_ORIGIN ?? 'http://localhost:3000';

export function Providers({ children }: { children: ReactNode }) {
  return (
    <AuthProvider
      authority={authority}
      client_id={clientId}
      redirect_uri={`${appOrigin}/auth/callback`}
      post_logout_redirect_uri={appOrigin}
      response_type="code"
      scope="openid profile email"
      automaticSilentRenew={true}
      onSigninCallback={() => { window.history.replaceState({}, document.title, '/auth/callback'); }}
    >
      {children}
    </AuthProvider>
  );
}
