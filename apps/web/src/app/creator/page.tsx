import type { Metadata } from 'next';
import { CreatorStudio } from '@/components/creator-studio';
export const metadata: Metadata = {title:'Creator Studio | ReelNest',description:'Submit licensed independent films for review.'};
export default function CreatorPage() { return <CreatorStudio/>; }
