'use client';

import Link from 'next/link';
import {usePathname} from 'next/navigation';
import {useEffect,useState,useSyncExternalStore} from 'react';
import {Compass,Film,Menu,UserRound,X} from 'lucide-react';
import {direction,isLocale,localeLabels,messages,type Locale} from '@/lib/localization';

const primary=[
  {href:'/browse',label:'Browse'}, {href:'/discover',label:'For You'},
  {href:'/browse?type=MOVIE',label:'Movies'}, {href:'/browse?type=SERIES',label:'Series'},
  {href:'/pricing',label:'Plans'}, {href:'/my-list',label:'My List'}, {href:'/collections',label:'Collections'},
];
const secondary=[
  {href:'/continue-watching',label:'Continue Watching'}, {href:'/channels',label:'Channels'}, {href:'/watch-party',label:'Watch Party'},
  {href:'/creator',label:'Creator Studio'}, {href:'/admin',label:'Review Console'},
  {href:'/admin/analytics',label:'Analytics'}, {href:'/admin/editorial',label:'Editorial'}, {href:'/admin/support',label:'Support queue'}, {href:'/lab',label:'HLS Lab'},
  {href:'/account/privacy',label:'Privacy'}, {href:'/support',label:'Help & FAQ'},
  {href:'/status',label:'Build status'}, {href:'/lab/drm',label:'DRM device diagnostics'},
];
const LOCALE_EVENT='reelnest:locale-changed';
function subscribeLocale(notify:()=>void){
  window.addEventListener('storage',notify);
  window.addEventListener(LOCALE_EVENT,notify);
  return()=>{window.removeEventListener('storage',notify);window.removeEventListener(LOCALE_EVENT,notify);};
}
function localeSnapshot():Locale{
  try{const value=window.localStorage.getItem('reelnest-ui-locale');return isLocale(value)?value:'en';}
  catch{return 'en';}
}
function serverLocaleSnapshot():Locale{return 'en';}
export function Navbar(){
  const pathname=usePathname();
  return <NavbarView key={pathname} pathname={pathname}/>;
}

function NavbarView({pathname}:{pathname:string}){
  const [menuOpen,setMenuOpen]=useState(false);
  const locale=useSyncExternalStore(subscribeLocale,localeSnapshot,serverLocaleSnapshot);
  useEffect(()=>{document.documentElement.lang=locale;document.documentElement.dir=direction(locale);},[locale]);
  const t=messages[locale];
  function chooseLocale(next:Locale){
    window.localStorage.setItem('reelnest-ui-locale',next);
    window.dispatchEvent(new Event(LOCALE_EVENT));
  }
  const translated:Record<string,string>={'Browse':t.browse,'For You':t.forYou,'Movies':t.movies,'Series':t.series,
    'Plans':t.plans,'My List':t.myList,'Collections':t.collections,'Channels':t.channels,'Watch Party':t.party};
  const link=(item:{href:string;label:string})=><Link key={item.href} href={item.href}
    onClick={()=>setMenuOpen(false)}
    className={pathname===item.href.split('?')[0]?'nav-link nav-link-active':'nav-link'}>{translated[item.label]||item.label}</Link>;
  return <header className="nav-shell"><nav className="nav wrap" aria-label="Primary navigation">
    <Link className="logo" href="/" aria-label="ReelNest home" onClick={()=>setMenuOpen(false)}>
      <span className="logo-mark"><Film size={21}/></span><span>reel<span className="logo-accent">nest</span><span className="logo-dot">.</span></span>
    </Link>
    <div className={`nav-links ${menuOpen?'nav-links-open':''}`} id="reelnest-nav-links">
      {primary.map(link)}
      <details className="nav-more"><summary>More</summary><div className="nav-more-links">{secondary.map(link)}</div></details>
      <div className="nav-mobile-secondary">{secondary.map(link)}</div>
    </div>
    <div className="nav-actions"><label className="locale-picker" aria-label={t.language}><span className="sr-only">{t.language}</span><select value={locale} onChange={e=>chooseLocale(e.target.value as Locale)} aria-label={t.language}>{(Object.keys(localeLabels) as Locale[]).map(l=><option key={l} value={l}>{localeLabels[l]}</option>)}</select></label>
      <Link href="/browse" className="icon-link" aria-label={t.explore}><Compass size={19}/></Link>
      <Link href="/profiles" className="profile-link"><UserRound size={16}/><span>{t.profiles}</span></Link>
      <button type="button" className="nav-menu-toggle" aria-expanded={menuOpen}
        aria-controls="reelnest-nav-links" aria-label={menuOpen?'Close navigation':'Open navigation'}
        onClick={()=>setMenuOpen(open=>!open)}>{menuOpen?<X size={22}/>:<Menu size={22}/>}</button>
    </div>
  </nav></header>;
}
