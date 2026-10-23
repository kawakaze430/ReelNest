import type {MetadataRoute} from 'next';
import {publicSiteOrigin} from '@/lib/site-origin';

type SeoTitle={id:string};
/** Fail closed: only the backend's explicitly approved, currently licensed titles. */
export default async function sitemap():Promise<MetadataRoute.Sitemap>{
  const origin=publicSiteOrigin(process.env.NEXT_PUBLIC_APP_ORIGIN);
  const base:MetadataRoute.Sitemap=['','/browse','/pricing'].map(path=>({
    url:`${origin}${path||'/'}`,changeFrequency:'weekly' as const,priority:path?0.6:1.0
  }));
  if(!origin.startsWith('https://'))return base;
  const api=process.env.API_INTERNAL_BASE_URL||process.env.NEXT_PUBLIC_API_BASE_URL;
  if(!api)return base;
  try{
    // Bounded traversal, no access tokens and no private catalog details.
    for(let page=0;page<5;page++){
      const response=await fetch(`${api}/api/v1/catalog/seo-index?page=${page}&size=100`,{
        cache:'no-store',signal:AbortSignal.timeout(2500)
      });
      if(!response.ok)break;
      const titles=await response.json() as SeoTitle[];
      if(!Array.isArray(titles))break;
      for(const title of titles){
        if(typeof title.id==='string' && /^[0-9a-f-]{36}$/i.test(title.id))
          base.push({url:`${origin}/title/${encodeURIComponent(title.id)}`,changeFrequency:'weekly',priority:0.5});
      }
      if(titles.length<100)break;
    }
  }catch{/* No indexing of titles when rights service is unavailable. */}
  return base;
}
