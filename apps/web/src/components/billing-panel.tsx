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
type PlanChangePreview={currentPlan:string;targetPlan:string;currentAmountPaise:number;
  targetAmountPaise:number;direction:string;automaticChangeAvailable:boolean;
  proratedAmountPaise:number|null;notice:string};
type Market={country:string;currency:string;checkoutAvailable:boolean;notice:string;visiblePlans:string[]};
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
  const auth=useAuth();
  return <BillingView key={auth.user?.profile.sub ?? 'anonymous'} auth={auth}/>;
}

function BillingView({auth}:{auth:ReturnType<typeof useAuth>}){
  const token=auth.user?.access_token;
  const [plans,setPlans]=useState<Plan[]>([]);
  const [billing,setBilling]=useState<Billing|null>(null);
  const [selectedMarket,setSelectedMarket]=useState('IN');
  const [changeTarget,setChangeTarget]=useState('PREMIUM');
  const [changePreview,setChangePreview]=useState<PlanChangePreview|null>(null);
  const [marketResult,setMarketResult]=useState<{token:string;country:string;data:Market|null}|null>(null);
  const market=marketResult && marketResult.token===token && marketResult.country===selectedMarket?marketResult.data:null;
  const [busy,setBusy]=useState('');const [error,setError]=useState('');const [message,setMessage]=useState('');
  const refresh=useCallback(async()=>{
    if(!token)return;
    const result=await apiFetch<Billing>('/api/v1/billing/me',token);setBilling(result);
  },[token]);
  useEffect(()=>{
    let live=true;
    apiFetch<Plan[]>('/api/v1/billing/plans').then(data=>{if(live)setPlans(data);}).catch(()=>{if(live)setError('Plans could not be loaded. Is the API online?');});
    if(token)void apiFetch<Billing>('/api/v1/billing/me',token)
      .then(result=>{if(live)setBilling(result);})
      .catch(()=>{if(live)setError('Unable to retrieve your subscription.');});
    return()=>{live=false;};
  },[token]);
  useEffect(()=>{
    if(!token)return;
    let alive=true;
    void apiFetch<Market>(`/api/v1/billing/markets/${selectedMarket}`,token)
      .then(data=>{if(alive)setMarketResult({token,country:selectedMarket,data});})
      .catch(()=>{if(alive)setMarketResult({token,country:selectedMarket,data:null});});
    return()=>{alive=false;};
  },[selectedMarket,token]);
  async function subscribe(code:string){
    if(!token){await auth.signinRedirect();return;}
    if(!market?.checkoutAvailable || market.country!=='IN' || !market.visiblePlans.includes(code)){
      setError('Checkout is unavailable for this selected market.');return;
    }
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
  async function inspectPlanChange(){
    if(!token||!billing?.premiumAccess)return;
    setError('');setChangePreview(null);setBusy('plan-change');
    try{
      const data=await apiFetch<PlanChangePreview>(
        `/api/v1/billing/plan-change-preview?targetPlan=${encodeURIComponent(changeTarget)}`,token);
      setChangePreview(data);
    }catch(e){setError(e instanceof Error?e.message:'Plan change unavailable.');}
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
    <section aria-label="Market checkout availability">
      <label>View market availability (informational only) <select value={selectedMarket}
        onChange={e=>setSelectedMarket(e.target.value)}>
        <option value="IN">India (IN)</option><option value="US">United States (US)</option>
        <option value="GB">United Kingdom (GB)</option><option value="AE">United Arab Emirates (AE)</option>
        <option value="CA">Canada (CA)</option>
      </select></label>
      <p role="status">{market?.notice??'Sign in to check currently configured market availability.'}</p>
      <p className="studio-file-note">Selecting a market does not prove your location, determine applicable tax,
        change rights, or activate a currency. Real checkout remains INR and provider-verified.</p>
    </section>
    <section className="billing-grid" aria-label="Subscription plans">
      {(plans.length?plans:[{code:'FREE',label:'Free',amountPaise:0,currency:'INR',active:true},{code:'STANDARD',label:'Standard',amountPaise:14900,currency:'INR',active:true},{code:'PREMIUM',label:'Premium',amountPaise:29900,currency:'INR',active:true}]).map(plan=><article key={plan.code} className={`billing-card ${plan.code==='STANDARD'?'billing-featured':''}`}>
        <div className="eyebrow">{plan.code==='STANDARD'?'POPULAR PLAN':plan.code==='FREE'?'EXPLORE FREE':'EXTRA SUPPORT'}</div>
        <h2>{plan.label}</h2>
        <p className="billing-price">₹{(plan.amountPaise/100).toLocaleString('en-IN')}<small>/month</small></p>
        <p>{plan.code==='FREE'?'Only titles offered for free.':plan.code==='STANDARD'?'Paid and free content from the licensed catalog.':'The same current paid catalog with a different demonstration price tier.'}</p>
        <div className="billing-benefits"><span><Check size={15}/> Original licensed content</span><span><Check size={15}/> Viewer profiles</span><span><Check size={15}/> Continue watching</span></div>
        {plan.code==='FREE'?<Link href="/browse" className="btn btn-outline">Browse free content</Link>:
          <button className="btn btn-primary" disabled={!!busy||billing?.premiumAccess||auth.isLoading||
            (!!token&&(!market?.checkoutAvailable||!market.visiblePlans.includes(plan.code)))} onClick={()=>void subscribe(plan.code)}>{busy===plan.code?'Starting checkout...':billing?.premiumAccess?'Membership active':!token?'Sign in to subscribe':'Subscribe securely'}</button>}
      </article>)}
    </section>
    {billing?.premiumAccess&&<section className="studio-panel" aria-label="Plan change review">
      <h2>Considering a different membership?</h2>
      <p>Preview current published plan prices. This does not amend a subscription, calculate
        prorated charges or authorize additional video access.</p>
      <label>Requested plan <select value={changeTarget} onChange={e=>{
        setChangeTarget(e.target.value);setChangePreview(null);
      }}><option value="STANDARD">Standard</option><option value="PREMIUM">Premium</option></select></label>
      <button type="button" className="btn btn-outline" disabled={!!busy}
        onClick={()=>void inspectPlanChange()}>Review a plan change</button>
      {changePreview&&<div role="status">
        <p>{changePreview.currentPlan} (₹{(changePreview.currentAmountPaise/100).toFixed(2)}) → {changePreview.targetPlan} (₹{(changePreview.targetAmountPaise/100).toFixed(2)})</p>
        <p>{changePreview.notice}</p>
        <p><Link href="/support">Contact support about plan changes</Link>.</p>
      </div>}
    </section>}
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
