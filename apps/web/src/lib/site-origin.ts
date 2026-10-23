/** Site origin is build-time configuration. Reject paths, credentials and invalid protocols. */
export function publicSiteOrigin(value: string | undefined): string {
  try {
    const parsed=new URL(value || 'http://localhost:3000');
    if (!['http:','https:'].includes(parsed.protocol) || parsed.username || parsed.password ||
        parsed.pathname!=='/' || parsed.search || parsed.hash) throw new Error('Invalid origin');
    if (parsed.protocol==='http:' && !['localhost','127.0.0.1'].includes(parsed.hostname))
      throw new Error('Public sites require HTTPS');
    return parsed.origin;
  } catch { return 'http://localhost:3000'; }
}
