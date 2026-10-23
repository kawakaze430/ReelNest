import type { Metadata } from 'next';
import { Suspense } from 'react';
import { CatalogBrowser } from '@/components/catalog-browser';
export const metadata: Metadata = { title: 'Browse stories | ReelNest', description: 'Explore the ReelNest prototype catalog; streaming availability requires licensed media.', alternates: { canonical: '/browse' } };
export default function BrowsePage() {
  return <Suspense fallback={<div className="wrap empty">Loading titles...</div>}><CatalogBrowser/></Suspense>;
}
