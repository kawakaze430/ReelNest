/** Non-authoritative creator preview of the worker's strict SRT policy.
 * No subtitle text leaves the browser until the creator explicitly saves it.
 * Worker validation against the encoded film duration remains authoritative.
 */
export type CaptionQa = {valid:boolean;cues:number;lastEndSeconds:number;errors:string[];warnings:string[]};
const STAMP=/^(?:(\d{1,2}):)?(\d{2}):(\d{2})[,.](\d{3})$/;
const RANGE=/^(\S+)\s+-->\s+(\S+)$/;
function millis(raw:string):number|null {
  const m=STAMP.exec(raw);
  if(!m)return null;
  const hours=Number(m[1]??0),minutes=Number(m[2]),seconds=Number(m[3]);
  if(hours>23||minutes>=60||seconds>=60)return null;
  return ((hours*60+minutes)*60+seconds)*1000+Number(m[4]);
}
export function inspectSrt(text:string):CaptionQa {
  const errors:string[]=[],warnings:string[]=[];
  const bytes=new TextEncoder().encode(text).byteLength;
  if(bytes>200000)errors.push('SRT must be smaller than 200 KB.');
  const blocks=text.replace(/^\uFEFF/,'').replace(/\r\n?/g,'\n').trim().split(/\n\s*\n/);
  if(!text.trim()||blocks.length>3000)errors.push('SRT must contain 1–3000 cues.');
  let previousEnd=0,lastEnd=0,cues=0;
  for(const block of blocks) {
    const lines=block.split('\n');
    if(/^\d+$/.test(lines[0]?.trim()??''))lines.shift();
    const timing=RANGE.exec(lines.shift()?.trim()??'');
    const start=timing?millis(timing[1]):null,end=timing?millis(timing[2]):null;
    if(start===null||end===null||end<=start||start<previousEnd){
      errors.push(`Cue ${cues+1}: invalid or overlapping timestamps.`);break;
    }
    const content=lines.join('\n').trim();
    if(!content||content.length>600||/[<>\u202e\u202d\x00-\x08\x0b\x0c\x0e-\x1f]/.test(content)){
      errors.push(`Cue ${cues+1}: empty, unsafe or oversized text.`);break;
    }
    if(lines.length>2)warnings.push(`Cue ${cues+1}: more than two lines.`);
    if(lines.some(line=>line.length>42))warnings.push(`Cue ${cues+1}: long line; check mobile readability.`);
    const seconds=(end-start)/1000;
    if(seconds<0.7)warnings.push(`Cue ${cues+1}: displayed for under 0.7 seconds.`);
    if(content.replace(/\s/g,'').length/seconds>20)warnings.push(`Cue ${cues+1}: fast reading speed.`);
    previousEnd=end;lastEnd=end;cues++;
  }
  return {valid:errors.length===0,cues,lastEndSeconds:lastEnd/1000,errors,warnings:warnings.slice(0,12)};
}
