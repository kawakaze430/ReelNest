import {inspectSrt} from '@/lib/caption-qa';

/** Client-side localization check only: not machine translation or editorial approval. */
type Cue={start:number;end:number;text:string};
export type LocalizationQa={valid:boolean;sourceCues:number;translationCues:number;
  driftedCues:number;unchangedCues:number;warnings:string[];errors:string[]};
const STAMP=/^(?:(\d{1,2}):)?(\d{2}):(\d{2})[,.](\d{3})$/;
function milliseconds(value:string):number {
  const match=STAMP.exec(value);
  if(!match)return NaN;
  return ((Number(match[1]??0)*60+Number(match[2]))*60+Number(match[3]))*1000+Number(match[4]);
}
function parse(text:string):Cue[] {
  return text.replace(/^\uFEFF/,'').replace(/\r\n?/g,'\n').trim().split(/\n\s*\n/).map(block=>{
    const lines=block.split('\n');
    if(/^\d+$/.test(lines[0]?.trim()??''))lines.shift();
    const timing=lines.shift()?.trim().match(/^(\S+)\s+-->\s+(\S+)$/);
    if(!timing)throw new Error('Missing source or translated cue timing');
    return {start:milliseconds(timing[1]),end:milliseconds(timing[2]),text:lines.join('\n').trim()};
  });
}
export function compareLocalization(sourceText:string,translatedText:string):LocalizationQa {
  const sourceQa=inspectSrt(sourceText),translationQa=inspectSrt(translatedText);
  const errors=[...sourceQa.errors.map(e=>'Source: '+e),
    ...translationQa.errors.map(e=>'Translation: '+e)];
  if(!sourceQa.valid||!translationQa.valid)
    return {valid:false,sourceCues:sourceQa.cues,translationCues:translationQa.cues,
      driftedCues:0,unchangedCues:0,warnings:[],errors:errors.slice(0,12)};
  const src=parse(sourceText),dst=parse(translatedText);
  if(src.length!==dst.length)
    errors.push(`Different cue counts: ${src.length} original versus ${dst.length} translated.`);
  let drift=0,unchanged=0;
  for(let i=0;i<Math.min(src.length,dst.length);i++){
    if(Math.abs(src[i].start-dst[i].start)>250||Math.abs(src[i].end-dst[i].end)>250)drift++;
    if(src[i].text.toLocaleLowerCase()===dst[i].text.toLocaleLowerCase())unchanged++;
  }
  const warnings:string[]=[];
  if(drift)warnings.push(`${drift} cues differ in timing by more than 250 ms; check synchronization.`);
  if(unchanged)warnings.push(`${unchanged} cues contain unchanged source text; verify proper names versus missed translation.`);
  if(src.length!==dst.length)warnings.push('Confirm there are no dropped or extra translation cues.');
  return {valid:errors.length===0 && drift===0,sourceCues:src.length,
    translationCues:dst.length,driftedCues:drift,unchangedCues:unchanged,
    warnings:warnings.slice(0,12),errors:errors.slice(0,12)};
}
