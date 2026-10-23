import {SupportCaseCenter} from '@/components/support-case-center';
import Link from 'next/link';
import type {Metadata} from 'next';

export const metadata:Metadata={title:'Help & FAQ | ReelNest',description:'Help with streaming, profiles, subscriptions and creator uploads.'};
const faq=[
  {q:'Why does a title say video unavailable?',a:'Only published videos with approved, current distribution rights and completed processing can play. Availability also depends on your profile, country and subscription.'},
  {q:'How do I fix buffering or playback errors?',a:'Check your connection, retry the authorized playback session, use Data saver for a lower rendition, or try another supported browser. Never share access tokens or payment details in a support request.'},
  {q:'Why can I not see subtitles or another audio language?',a:'Track choices only appear when the uploaded, approved media package contains them. A language preference cannot create missing tracks.'},
  {q:'Can I watch on another device?',a:'Sign in with the same account and choose the same profile. Continue Watching and supported playback preferences synchronize when the API is available. Active stream limits still apply.'},
  {q:'How do I manage my membership?',a:'Open Account Billing to view verified subscription information and available cancellation or payment options. Payment status is verified on the server.'},
  {q:'How do I request deletion or review privacy settings?',a:'Open Account Privacy. Some financial or legal records may need to be retained under applicable obligations.'},
  {q:'How do creators submit films?',a:'Use Creator Studio. Publishing requires creator approval, rights evidence, upload processing and moderator review.'},
];
const configured=process.env.NEXT_PUBLIC_SUPPORT_EMAIL??'';
const email=/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(configured)?configured:'';
export default function SupportPage(){
  return <main className="wrap support-page"><div className="eyebrow">REELNEST SUPPORT</div><h1>Help & frequently asked questions</h1>
    <p>Streaming access, device support, creator uploads and account settings. This is a development preview; availability and support operations may be limited.</p>
    <div className="support-faq">{faq.map(item=><details key={item.q}><summary>{item.q}</summary><p>{item.a}</p></details>)}</div>
    <section className="support-actions"><h2>Still need help?</h2>
      {email?<p>Contact our configured support mailbox: <a href={`mailto:${email}`}>{email}</a>. Do not include passwords, tokens or payment card data.</p>:
        <p>Direct support contact has not been configured for this deployment. Operators must set NEXT_PUBLIC_SUPPORT_EMAIL to a monitored support address before a public launch.</p>}
      <div className="hero-buttons"><Link href="/account/billing" className="btn btn-outline">Billing</Link>
        <Link href="/account/privacy" className="btn btn-outline">Privacy</Link>
        <Link href="/status" className="btn btn-outline">System status</Link></div>
    </section>
    <SupportCaseCenter/>
  </main>;
}
