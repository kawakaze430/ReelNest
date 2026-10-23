'use client';
import {useParams} from 'next/navigation';
import {ProtectedPlayer} from '@/components/protected-player';
export default function WatchPage(){const {id}=useParams<{id:string}>();return <ProtectedPlayer titleId={id}/>;}
