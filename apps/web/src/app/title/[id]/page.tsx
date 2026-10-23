import type {Metadata} from 'next';
import {publicSiteOrigin} from '@/lib/site-origin';
import {TitleClient} from '@/components/title/title-client';

type SeoTitle={id:string;name:string;synopsis:string;type:string;genre:string;language:string;ageRating:string;releaseYear:number|null};
type Props={params:Promise<{id:string}>};
const privateRobots={index:false,follow:false} as const;

/** Never index demo, unpublished, unlicensed or unapproved titles. */
export async function generateMetadata({params}:Props):Promise<Metadata>{
  const {id}=await params;
  const origin=publicSiteOrigin(process.env.NEXT_PUBLIC_APP_ORIGIN);
  if(!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id))
    return {title:'Title unavailable | ReelNest',robots:privateRobots};
  const api=process.env.API_INTERNAL_BASE_URL||process.env.NEXT_PUBLIC_API_BASE_URL||'http://localhost:8080';
  try{
    const response=await fetch(`${api}/api/v1/catalog/${encodeURIComponent(id)}/seo`,{
      cache:'no-store',signal:AbortSignal.timeout(2500)
    });
    if(!response.ok)return {title:'Title unavailable | ReelNest',robots:privateRobots};
    const title=await response.json() as SeoTitle;
    if(title.id!==id || !title.name || title.name.length>250 || !title.synopsis || title.synopsis.length>5000)
      return {title:'Title unavailable | ReelNest',robots:privateRobots};
    const url=`${origin}/title/${encodeURIComponent(id)}`;
    return {title:`${title.name} | ReelNest`,description:title.synopsis.slice(0,155),
      alternates:{canonical:url},robots:{index:true,follow:true},
      openGraph:{title:title.name,description:title.synopsis.slice(0,155),url,type:'website'}};
  }catch{return {title:'Title unavailable | ReelNest',robots:privateRobots};}
}
export default function TitlePage(){return <TitleClient/>;}
