import type { Metadata } from 'next';
import { ReviewConsole } from '@/components/review-console';
export const metadata: Metadata = {title:'Content Review | ReelNest',description:'Development rights and creator review console.'};
export default function AdminPage() { return <ReviewConsole/>; }
