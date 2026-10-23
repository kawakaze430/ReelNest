import type { Metadata } from 'next';
import {BillingPanel} from '@/components/billing-panel';
export const metadata: Metadata = { title: 'Membership plans | ReelNest', description: 'Explore ReelNest membership concepts. Real paid streaming requires verified payment setup.', alternates: { canonical: '/pricing' } };
export default function Pricing(){return <BillingPanel/>;}
