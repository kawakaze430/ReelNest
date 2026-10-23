import {notFound,redirect} from 'next/navigation';
import type {Metadata} from 'next';
type Props={params:Promise<{slug:string}>};
export const metadata:Metadata={title:'Title | ReelNest',robots:{index:false,follow:false}};
/** Short canonical alias: the destination page performs rights-aware SEO decisions. */
export default async function SlugRedirect({params}:Props){
 const {slug}=await params;
 if(!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug)||slug.length>90)notFound();
 const origin=process.env.API_INTERNAL_BASE_URL||process.env.NEXT_PUBLIC_API_BASE_URL||'http://localhost:8080';
 const response=await fetch(`${origin}/api/v1/catalog/by-slug/${encodeURIComponent(slug)}`,
    {cache:'no-store',signal:AbortSignal.timeout(3000)}).catch(()=>null);
  if(!response||!response.ok)notFound();
  const data=await response.json() as {id?:string;slug?:string};
  if(data.slug!==slug||!data.id||!/^[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}$/i.test(data.id))notFound();
 redirect(`/title/${encodeURIComponent(data.id)}`);
}
