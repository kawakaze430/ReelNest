'use client';
import {useState} from 'react';
import {compareLocalization,type LocalizationQa} from '@/lib/subtitle-localization-qa';

/** Files remain on this device; no network request or automatic translation occurs. */
export function SubtitleLocalizationReview(){
  const [original,setOriginal]=useState<File|null>(null);
  const [translation,setTranslation]=useState<File|null>(null);
  const [result,setResult]=useState<LocalizationQa|null>(null);
  const [working,setWorking]=useState(false);
  async function review(){
    if(!original||!translation)return;
    setWorking(true);setResult(null);
    try{
      if(original.size>200000||translation.size>200000 ||
        !original.name.toLowerCase().endsWith('.srt')||!translation.name.toLowerCase().endsWith('.srt'))
        throw new Error('Choose two .srt subtitle files, each no larger than 200 KB.');
      const [src,dst]=await Promise.all([original.text(),translation.text()]);
      setResult(compareLocalization(src,dst));
    }catch(e){setResult({valid:false,sourceCues:0,translationCues:0,driftedCues:0,
      unchangedCues:0,warnings:[],errors:[e instanceof Error?e.message:'Cannot inspect subtitle files.']});}
    finally{setWorking(false);}
  }
  return <fieldset className="cue-editor">
    <legend>Translation timing and completeness review</legend>
    <p>Compare a human-translated SRT with the original before uploading. Files stay in your browser; this does not translate, approve or publish captions.</p>
    <div className="cue-editor-fields">
      <label>Original SRT<input type="file" accept=".srt" onChange={e=>{setOriginal(e.target.files?.[0]??null);setResult(null);}}/></label>
      <label>Translated SRT<input type="file" accept=".srt" onChange={e=>{setTranslation(e.target.files?.[0]??null);setResult(null);}}/></label>
    </div>
    <button type="button" className="btn btn-outline" disabled={!original||!translation||working}
      onClick={()=>void review()}>{working?'Comparing...':'Compare original and translated cues'}</button>
    {result&&<div role="status" aria-live="polite">
      <p><strong>{result.valid?'Structure and timings aligned':'Translation needs review'}</strong> · Original {result.sourceCues} cues, translated {result.translationCues} cues.</p>
      {result.errors.map((msg,i)=><p key={'err'+i}>Error: {msg}</p>)}
      {result.warnings.map((msg,i)=><p key={'warn'+i}>Check: {msg}</p>)}
      {result.valid&&<p>Timing checks pass. A human must still verify accuracy, language, context and rights before using the existing subtitle upload.</p>}
    </div>}
  </fieldset>;
}
