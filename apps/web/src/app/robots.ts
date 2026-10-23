import type { MetadataRoute } from 'next';
import { publicSiteOrigin } from '@/lib/site-origin';

export default function robots(): MetadataRoute.Robots {
  const origin=publicSiteOrigin(process.env.NEXT_PUBLIC_APP_ORIGIN);
  return {
    rules:[{userAgent:'*',allow:['/','/browse','/pricing'],disallow:[
      '/api/','/account/','/admin/','/auth/','/creator/','/continue-watching',
      '/discover','/login','/my-list','/profiles','/status','/watch/','/lab'
    ]}],
    sitemap:`${origin}/sitemap.xml`,
  };
}
