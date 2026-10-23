'use client';

import {useAuth} from 'react-oidc-context';
import {useCallback,useEffect,useState} from 'react';
import Link from 'next/link';
import {Check,ShieldCheck,CreditCard,AlertCircle,RefreshCw} from 'lucide-react';
import {apiFetch,ApiError} from '@/lib/api';

type Plan={code:string;label:string;amountPaise:number;currency:string;active:boolean};
type Subscription={id:string;planCode:string;status:string;periodEnd:string|null;paidThrough:string|null;cancelAtCycleEnd:boolean;providerSubscriptionId:string};
type Billing={premiumAccess:boolean;subscriptions:Subscription[]};
type Checkout={subscriptionId:string;keyId:string;planCode:string;status:string};
type RazorpayCheckout={open:()=>void};
declare global {interface Window {Razorpay?:new (config:Record<string,unknown>)=>RazorpayCheckout}}

async function loadCheckoutSdk():Promise<void>{
  if(window.Razorpay)return;
  await new Promise<void>((resolve,reject)=>{
    const existing=document.querySelector<HTMLScriptElement>('script[data-reelnest-razorpay]');
    if(existing){existing.addEventListener('load',()=>resolve(),{once:true});existing.addEventListener('error',()=>reject(new Error('Checkout unavailable')),{once:true});return;}
    const script=document.createElement('script');
    script.src='https://checkout.razorpay.com/v1/checkout.js';
    script.async=true;script.dataset.reelnestRazorpay='true';
    script.onload=()=>resolve();script.onerror=()=>reject(new Error('Could not load payment checkout'));
    document.body.append(script);
  });
  if(!window.Razorpay)throw new Error('Payment checkout SDK unavailable');
}
export function BillingPanel(){
  const auth=useAuth();const token=auth.user?.access_token;
  const [plans,setPlans]=useState<Plan[]>([]);
  const [billing,setBilling]=useState<Billing|null>(null);
  const [busy,setBusy]=useState('');const [error,setError]=useState('');const [message,setMessage]=useState('');
  const refresh=useCallback(async()=>{
    if(!token)return;
    const result=await apiFetch<Billing>('/api/v1/billing/me',token);setBilling(result);
  },[token]);
  useEffect(()=>{
    let live=true;
    apiFetch<Plan[]>('/api/v1/billing/plans').then(data=>{if(live)setPlans(data);}).catch(()=>{if(live)setError('Plans could not be loaded. Is the API online?');});
    if(token)void refresh().catch(()=>{if(live)setError('Unable to retrieve your subscription.');});
    return()=>{live=false;};
  },[token,refresh]);
  async function subscribe(code:string){
    if(!token){await auth.signinRedirect();return;}
    setBusy(code);setError('');setMessage('');
    try{
      // Load payment SDK first; users are not charged until checkout authorization.
      await loadCheckoutSdk();
      const checkout=await apiFetch<Checkout>('/api/v1/billing/subscriptions',token,{
        method:'POST',body:JSON.stringify({planCode:code})});
      if(!window.Razorpay)throw new Error('Checkout SDK missing');
      const modal=new window.Razorpay({
        key:checkout.keyId,subscription_id:checkout.subscriptionId,
        name:'ReelNest',description:`${code} subscription`,
        theme:{color:'#e76367'},
        handler:()=>{
          setMessage('Payment submitted. Access activates only after the payment provider confirms your subscription.');
          void refresh().catch(()=>{});
        },
        modal:{ondismiss:()=>setMessage('Checkout closed. You can check subscription status below.')}
      });
      modal.open();
    }catch(err){setError(err instanceof ApiError?err.message:err instanceof Error?err.message:'Checkout unavailable');}
    finally{setBusy('');}
  }
  async function cancel(id:string){
    if(!token||!window.confirm('Request cancellation at the end of your billing cycle?'))return;
    setBusy(id);setError('');
    try{
      const result=await apiFetch<Billing>(`/api/v1/billing/subscriptions/${id}/cancel`,token,{method:'POST'});
      setBilling(result);setMessage('Cancellation request sent to the payment provider. Check the displayed period end.');
    }catch(err){setError(err instanceof Error?err.message:'Cancellation failed');}
    finally{setBusy('');}
  }
  return <main className="wrap billing-shell">
    <div className="eyebrow"><CreditCard size={15}/> REELNEST / MEMBERSHIP</div>
    <h1>Choose your way to watch<span className="title-period">.</span></h1>
    <p className="billing-subtitle">Free movies remain free. Paid films require an active, verified subscription. No payment details are stored by ReelNest.</p>
    {error&&<div className="notice" role="alert"><AlertCircle size={18}/>{error}</div>}
    {message&&<div className="notice" role="status"><ShieldCheck size={18}/>{message}</div>}
    <section className="billing-grid" aria-label="Subscription plans">
      {(plans.length?plans:[{code:'FREE',label:'Free',amountPaise:0,currency:'INR',active:true},{code:'STANDARD',label:'Standard',amountPaise:14900,currency:'INR',active:true},{code:'PREMIUM',label:'Premium',amountPaise:29900,currency:'INR',active:true}]).map(plan=><article key={plan.code} className={`billing-card ${plan.code==='STANDARD'?'billing-featured':''}`}>
        <div className="eyebrow">{plan.code==='STANDARD'?'POPULAR PLAN':plan.code==='FREE'?'EXPLORE FREE':'EXTRA SUPPORT'}</div>
        <h2>{plan.label}</h2>
        <p className="billing-price">₹{(plan.amountPaise/100).toLocaleString('en-IN')}<small>/month</small></p>
        <p>{plan.code==='FREE'?'Only titles offered for free.':plan.code==='STANDARD'?'Paid and free content from the licensed catalog.':'The same current paid catalog with a different demonstration price tier.'}</p>
        <div className="billing-benefits"><span><Check size={15}/> Original licensed content</span><span><Check size={15}/> Viewer profiles</span><span><Check size={15}/> Continue watching</span></div>
        {plan.code==='FREE'?<Link href="/browse" className="btn btn-outline">Browse free content</Link>:
          <button className="btn btn-primary" disabled={!!busy||billing?.premiumAccess||!token&&auth.isLoading} onClick={()=>void subscribe(plan.code)}>{busy===plan.code?'Starting checkout...':billing?.premiumAccess?'Membership active':!token?'Sign in to subscribe':'Subscribe securely'}</button>}
      </article>)}
    </section>
    <section className="billing-history" aria-label="Your subscriptions">
      <div className="billing-history-title"><h2>Your membership</h2><button className="btn btn-outline" disabled={!token||!!busy} onClick={()=>void refresh().catch(()=>setError('Could not refresh billing status'))}><RefreshCw size={16}/> Refresh status</button></div>
      {!token?<p>Sign in to view subscriptions and manage billing.</p>:!billing?<p>Loading your membership from the server…</p>:
        <><p>Premium playback: <strong>{billing.premiumAccess?'Active':'Not active'}</strong>. Only verified provider status unlocks paid films.</p>
        {!billing.subscriptions.length&&<p>No subscriptions yet.</p>}
        {billing.subscriptions.map(s=><div className="billing-record" key={s.id}>
          <div><strong>{s.planCode}</strong> <span className="billing-status">{s.status}</span><p>{s.paidThrough?`Verified paid access until: ${new Date(s.paidThrough).toLocaleDateString()}`:'No confirmed paid period yet'}{s.periodEnd?` · Provider cycle end: ${new Date(s.periodEnd).toLocaleDateString()}`:''}</p></div>
          {['ACTIVE','CANCEL_SCHEDULED'].includes(s.status)&&!s.cancelAtCycleEnd&&<button className="btn btn-outline" disabled={!!busy} onClick={()=>void cancel(s.id)}>Cancel renewal</button>}
        </div>)}
        </>}
    </section>
    <p className="billing-legal"><ShieldCheck size={16}/> Payments use Razorpay-hosted checkout. Plans and billing shown here are development defaults; configure correct prices, provider plans, cancellation terms, and merchant compliance before public launch.</p>
  </main>;
}
