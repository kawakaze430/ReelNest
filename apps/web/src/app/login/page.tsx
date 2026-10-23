'use client';
import { useAuth } from 'react-oidc-context';
import Link from 'next/link';
import { LogIn } from 'lucide-react';
export default function LoginPage() {
  const auth = useAuth();
  return <main className="wrap profiles-page"><div className="auth-gate"><h1>Welcome back.</h1><p>Sign in using the configured Keycloak realm. No credentials are stored by this website.</p>{auth.isAuthenticated?<Link className="btn btn-primary" href="/profiles">Open profiles</Link>:<button disabled={auth.isLoading} onClick={()=>void auth.signinRedirect()} className="btn btn-primary"><LogIn size={17}/> Sign in with Keycloak</button>}</div></main>;
}
