/** Fail production image creation before Next.js embeds wrong NEXT_PUBLIC_* values. */
const expected = {
  NEXT_PUBLIC_API_BASE_URL: 'api',
  NEXT_PUBLIC_AUTHORITY: 'authority',
  NEXT_PUBLIC_APP_ORIGIN: 'app'
};
function productionUrl(name) {
  const raw = process.env[name];
  if (!raw || !raw.startsWith('https://')) throw new Error(`${name} must be a configured HTTPS URL`);
  let parsed;
  try { parsed = new URL(raw); } catch { throw new Error(`${name} is not a URL`); }
  if (parsed.username || parsed.password || parsed.hash || parsed.search ||
      /^(localhost|127\.0\.0\.1|0\.0\.0\.0|example\.com|.+\.example\.com)$/i.test(parsed.hostname)) {
    throw new Error(`${name} points at placeholder, loopback, or has unsafe URL components`);
  }
  return parsed;
}
for (const name of Object.keys(expected)) productionUrl(name);
const origin = productionUrl('NEXT_PUBLIC_APP_ORIGIN');
const api = productionUrl('NEXT_PUBLIC_API_BASE_URL');
const authority = productionUrl('NEXT_PUBLIC_AUTHORITY');
if (origin.pathname !== '/' || api.pathname !== '/') throw new Error('App and API URLs must be origins without paths');
if (!authority.pathname.startsWith('/realms/')) throw new Error('Authority must be an OIDC Keycloak realm URL');
if (!process.env.NEXT_PUBLIC_OIDC_CLIENT_ID) throw new Error('OIDC client ID is required');
console.log('Production web URLs validated; safe to compile Next.js static assets.');
