import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
const require=createRequire(import.meta.url);
const root=path.resolve(import.meta.dirname,'..');
let ts;
for(const location of ['typescript',path.join(root,'apps/web/node_modules/typescript'),'/opt/nvm/versions/node/v22.16.0/lib/node_modules/typescript']){
  try{ts=require(location);break;}catch{}
}
if(!ts)throw new Error('TypeScript compiler missing; install apps/web dependencies');
function load(relative){
  const src=fs.readFileSync(path.join(root,relative),'utf8');
  const js=ts.transpileModule(src,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;
  const mod={exports:{}};
  new Function('exports','module',js)(mod.exports,mod);
  return mod.exports;
}
const {parsePreviewVtt,previewAt,previewUrl}=load('apps/web/src/lib/seek-previews.ts');
const good='WEBVTT\n\n00:00:00.000 --> 00:00:10.000\nthumb_0001.jpg\n\n00:00:10.000 --> 00:00:20.000\nthumb_0002.jpg\n';
const cues=parsePreviewVtt(good);
assert.equal(cues.length,2);
assert.equal(previewAt(cues,12)?.filename,'thumb_0002.jpg');
assert.equal(previewAt(cues,20)?.filename,'thumb_0002.jpg');
assert.equal(previewAt(cues,-1),null);
assert.deepEqual(parsePreviewVtt(good.replace('thumb_0001.jpg','../../secrets.txt')),[]);
assert.deepEqual(parsePreviewVtt(good.replace('00:00:10.000 --> 00:00:20.000','00:00:09.000 --> 00:00:20.000')),[]);
assert.equal(previewUrl('/api/v1/playback/sessions/uuid/files/master.m3u8','thumb_0001.jpg'),
  '/api/v1/playback/sessions/uuid/files/previews/thumb_0001.jpg');
assert.equal(previewUrl('https://cdn.example/processed/abc/master.m3u8','preview.vtt'),
  'https://cdn.example/processed/abc/previews/preview.vtt');
assert.equal(previewUrl('https://cdn.example/processed/abc/master.m3u8','../secret'),null);
const {cleanPreferences}=load('apps/web/src/lib/player-preferences.ts');
assert.equal(cleanPreferences({autoplayNext:true}).autoplayNext,true);
assert.equal(cleanPreferences({autoplayNext:'true'}).autoplayNext,false);
assert.equal(cleanPreferences({}).autoplayNext,false);
console.log('Build 16 frontend pure TS behavior: 11 assertions passed');
