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
const {QoeReplayQueue,retryableQoeError,qoeBackoff}=load('apps/web/src/lib/qoe-replay.ts');
assert.equal(retryableQoeError({status:503}),true);
assert.equal(retryableQoeError({status:429}),true);
assert.equal(retryableQoeError({status:403}),false);
assert.equal(retryableQoeError({status:400}),false);
assert.equal(retryableQoeError(new Error('network')),true);
assert.equal(qoeBackoff(0),500);
assert.equal(qoeBackoff(4),8000);
const sample={eventId:'same-id',eventType:'STARTUP',elapsedMs:10,qualityHeight:null,errorCode:null};
let sends=0;
const queue=new QoeReplayQueue(async()=>{sends++;});
queue.enqueue(sample); // opt-in off by default
assert.equal(queue.queued,0);
queue.setEnabled(true);
queue.enqueue(sample);
await new Promise(resolve=>setImmediate(resolve));
assert.equal(sends,1);
assert.equal(queue.queued,0);
queue.setEnabled(false);
queue.enqueue(sample);
assert.equal(queue.queued,0);
queue.dispose();queue.setEnabled(true);queue.enqueue(sample);
assert.equal(queue.queued,0);
let failSends=0;
const forbidden=new QoeReplayQueue(async()=>{failSends++;throw {status:403};});
forbidden.setEnabled(true);forbidden.enqueue(sample);
await new Promise(resolve=>setImmediate(resolve));
assert.equal(failSends,1);
assert.equal(forbidden.queued,0);
forbidden.dispose();
const {parseSpriteVtt,previewAt,previewUrl}=load('apps/web/src/lib/seek-previews.ts');
const sprite='WEBVTT\n\n00:00:00.000 --> 00:00:10.000\nsprite_01.jpg#xywh=0,0,320,180\n\n00:00:10.000 --> 00:00:20.000\nsprite_01.jpg#xywh=320,0,320,180\n';
const spriteCues=parseSpriteVtt(sprite);
assert.equal(spriteCues.length,2);
assert.equal(previewAt(spriteCues,15)?.crop?.x,320);
assert.deepEqual(parseSpriteVtt(sprite.replace('320,0,320,180','9999,0,320,180')),[]);
assert.deepEqual(parseSpriteVtt(sprite.replace('sprite_01.jpg','../sprite_01.jpg')),[]);
assert.equal(previewUrl('/api/v1/playback/sessions/abc/files/master.m3u8','sprite_01.jpg'),'/api/v1/playback/sessions/abc/files/previews/sprite_01.jpg');
assert.equal(previewUrl('/api/v1/playback/sessions/abc/files/master.m3u8','sprites.vtt'),'/api/v1/playback/sessions/abc/files/previews/sprites.vtt');
console.log('Build 17 QoE replay and sprite helpers: 21 assertions passed');
