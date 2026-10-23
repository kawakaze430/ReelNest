import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import { Providers } from '@/components/providers';
import { Navbar } from '@/components/navbar';
import './globals.css';
import {ServiceWorkerRegistration} from '@/components/service-worker-registration';
import { publicSiteOrigin } from '@/lib/site-origin';

export const metadata: Metadata = {
  metadataBase: new URL(publicSiteOrigin(process.env.NEXT_PUBLIC_APP_ORIGIN)),
  title: 'ReelNest — Stories worth finding',
  manifest: '/manifest.webmanifest',
  appleWebApp: {capable: true, title: 'ReelNest', statusBarStyle: 'black-translucent'},
  description: 'ReelNest is an OTT platform prototype with fictional catalog metadata and an original test clip. Licensed streaming titles require rights approval.',
  openGraph: {type:'website',siteName:'ReelNest',title:'ReelNest — Stories worth finding',description:'Independent stories, creators and streaming discovery.'},
  twitter: {card:'summary',title:'ReelNest — Stories worth finding'},
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return <html lang="en"><body><a href="#main-content" className="skip-content">Skip to content</a><Providers><ServiceWorkerRegistration/><Navbar/><div id="main-content" tabIndex={-1}>{children}</div><footer className="site-footer wrap"><div className="footer-brand">reel<span>nest</span>.</div><div>Original streaming platform concept · Development preview</div><div>Demo metadata only · Original test clip only · Licensed content required</div></footer></Providers></body></html>;
}
