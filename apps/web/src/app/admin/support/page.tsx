import type {Metadata} from 'next';
import {AdminSupportCases} from '@/components/admin-support-cases';
export const metadata:Metadata={title:'Support Review | ReelNest',robots:{index:false,follow:false}};
export default function AdminSupport(){return <AdminSupportCases/>;}
