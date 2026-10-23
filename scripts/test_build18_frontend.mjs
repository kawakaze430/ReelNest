import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const root=path.resolve(import.meta.dirname,'..');
let ts;
for(const loc of ['typescript',path.join(root,'apps/web/node_modules/typescript'),'/opt/nvm/versions/node/v22.16.0/lib/node_modules/typescript']){
  try{ts=require(loc);break;}catch{}
}
if(!ts)throw new Error('TypeScript compiler missing');
function load(file){
  const src=fs.readFileSync(path.join(root,file),'utf8');
  const js=ts.transpileModule(src,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;
  const mod={exports:{}};
  new Function('exports','module',js)(mod.exports,mod);
  return mod.exports;
}
const {nativeAudioList,nativeAudioOptions,selectNativeAudio}=load('apps/web/src/lib/native-audio.ts');
assert.equal(nativeAudioList({}),null);
assert.equal(nativeAudioList({audioTracks:{length:40}}),null);
const list={length:2,0:{language:'eng',label:'English',enabled:true},1:{language:'hin',label:'Hindi',enabled:false}};
assert.equal(nativeAudioList({audioTracks:list}),list);
assert.deepEqual(nativeAudioOptions(list),[{id:0,language:'eng',label:'English'},{id:1,language:'hin',label:'Hindi'}]);
assert.equal(selectNativeAudio(list,1),true);
assert.equal(list[0].enabled,false);
assert.equal(list[1].enabled,true);
assert.equal(selectNativeAudio(list,5),false);
assert.equal(selectNativeAudio(list,-1),false);
assert.equal(selectNativeAudio(list,1.5),false);
assert.equal(list[1].enabled,true);
const {inspectSrt}=load('apps/web/src/lib/caption-qa.ts');
const good=inspectSrt('1\n00:00:00,100 --> 00:00:01,200\nHello!\n\n2\n00:00:01,200 --> 00:00:02,800\nGoodbye!');
assert.equal(good.valid,true);
assert.equal(good.cues,2);
assert.equal(good.lastEndSeconds,2.8);
assert.equal(inspectSrt('1\n00:00:00,100 --> 00:00:01,200\nHello!\n\n2\n00:00:01,000 --> 00:00:02,800\nOverlaps').valid,false);
assert.equal(inspectSrt('1\n00:00:00,100 --> 00:00:01,200\n<script>').valid,false);
assert.equal(inspectSrt('1\n00:00:00,100 --> 00:00:01,200\nHello').valid,true);
assert.equal(inspectSrt('').valid,false);
assert.equal(inspectSrt('1\n00:00:00,100 --> 00:00:00,400\nThis is a very long sentence to read in a short time.').warnings.length>0,true);
console.log('Build 18 native audio and caption QA: 18 assertions passed');
