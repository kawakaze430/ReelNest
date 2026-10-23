'use client';
import { useAuth } from 'react-oidc-context';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
export default function AuthCallbackPage() {
  const auth = useAuth();
  const router = useRouter();
  useEffect(() => { if (auth.isAuthenticated) router.replace('/profiles'); }, [auth.isAuthenticated, router]);
  return <main className="wrap empty">{auth.error ? `Sign-in failed: ${auth.error.message}` : 'Finishing secure sign-in...'}</main>;
}
