'use client';
import {useEffect,useState} from 'react';
import {useAuth} from 'react-oidc-context';
import {apiFetch} from '@/lib/api';

type Request={id:string;user_id:string|null;request_type:string;status:string;requested_at:string};
export default function PrivacyReview(){
 const auth=useAuth();const [items,setItems]=useState<Request[]>([]);const [error,setError]=useState('');const [busy,setBusy]=useState('');
 const token=auth.user?.access_token;
 useEffect(()=>{if(!token)return;apiFetch<Request[]>('/api/v1/privacy/admin/requests',token)
  .then(setItems).catch(()=>setError('Cannot read review queue. An administrator role is required.'));},[token]);
 async function review(id:string){if(!token)return;
  if(!window.confirm('Erase only a FREE viewer with no billing, creator, or audit records? Keycloak identity deletion is irreversible.'))return;
  setBusy(id);setError('');
  try {await apiFetch(`/api/v1/privacy/admin/requests/${id}/erase-free-viewer`,token,{method:'POST'});
    setItems(items=>items.map(x=>x.id===id?{...x,status:'FULFILLED'}:x));
  }catch(e){setError(e instanceof Error?e.message:'Deletion failed; account remains locked if already started.');}
  finally{setBusy('');}
 }
 return <main className="wrap viewer-shell"><h1>Privacy requests</h1>
 <p>Verified automated erasure is restricted to free viewers without financial, creator or audit records. All other requests require supervised retention review.</p>
 {error&&<p role="alert" className="notice studio-error">{error}</p>}
 <table style={{width:'100%',textAlign:'left'}}><thead><tr><th>Request</th><th>Status</th><th>Received</th><th>Action</th></tr></thead><tbody>{items.map(item=><tr key={item.id}>
  <td>{item.id.slice(0,8)}… ({item.request_type})</td><td>{item.status}</td>
  <td>{new Date(item.requested_at).toLocaleDateString()}</td><td>{item.request_type==='DELETE'&&item.status!=='FULFILLED'?<button disabled={busy===item.id} onClick={()=>review(item.id)} className="btn btn-outline">Erase eligible viewer</button>:null}</td>
 </tr>)}</tbody></table></main>;
}
