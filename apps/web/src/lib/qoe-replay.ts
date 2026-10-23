/** Bounded, opt-in, session-scoped QoE replay. No persistent browser storage.
 * The server owns consent, identity, expiry and idempotent event IDs.
 */
export type QoeEventType = 'STARTUP'|'STALL'|'QUALITY_SWITCH'|'PLAYER_ERROR'|'RECOVERY';
export type QoeEvent = {
  eventId:string;eventType:QoeEventType;elapsedMs:number|null;
  qualityHeight:number|null;errorCode:string|null;
};
export type QoeSend = (event:QoeEvent)=>Promise<void>;

export function retryableQoeError(error:unknown):boolean {
  const status=(error && typeof error==='object' && 'status' in error)
    ? (error as {status:unknown}).status : undefined;
  return status===undefined || status===429 || (typeof status==='number' && status>=500 && status<=599);
}
export function qoeBackoff(attempt:number):number {
  return Math.min(8000, 500 * 2 ** Math.max(0,Math.min(4,attempt)));
}

export class QoeReplayQueue {
  private pending: {event:QoeEvent;attempts:number}[]=[];
  private timer:ReturnType<typeof setTimeout>|null=null;
  private busy=false;
  private active=true;
  private enabled=false;
  constructor(private readonly send:QoeSend) {}

  setEnabled(enabled:boolean):void {
    this.enabled=enabled && this.active;
    if(!this.enabled) this.clear();
    else void this.flush();
  }
  enqueue(event:QoeEvent):void {
    if(!this.active || !this.enabled || this.pending.length>=40)return;
    this.pending.push({event,attempts:0});
    void this.flush();
  }
  async flush():Promise<void> {
    if(!this.active || !this.enabled || this.busy || this.timer || !this.pending.length)return;
    this.busy=true;
    try {
      while(this.active && this.enabled && this.pending.length){
        const head=this.pending[0];
        try {
          await this.send(head.event);
          if(this.pending[0]===head)this.pending.shift();
        }catch(error){
          if(this.pending[0]!==head)continue;
          if(!retryableQoeError(error) || head.attempts>=4){this.pending.shift();continue;}
          const delay=qoeBackoff(head.attempts++);
          this.timer=setTimeout(()=>{this.timer=null;void this.flush();},delay);
          break;
        }
      }
    }finally{this.busy=false;}
  }
  clear():void {
    this.pending=[];
    if(this.timer){clearTimeout(this.timer);this.timer=null;}
  }
  dispose():void {this.active=false;this.enabled=false;this.clear();}
  get queued():number {return this.pending.length;}
}
