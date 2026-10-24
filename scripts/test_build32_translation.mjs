/** Pure TS translation QA tests using locked project TypeScript; no network, media or cloud. */
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {createRequire} from 'node:module';
import {join} from 'node:path';
import vm from 'node:vm';
const root=process.cwd();
const requireProject=createRequire(join(root,'apps/web/package.json'));
const ts=requireProject('typescript');
function load(name,modules={}) {
  const file=join(root,'apps/web/src/lib',name);
  const code=ts.transpileModule(readFileSync(file,'utf8'),{
    compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}
  }).outputText;
  const module={exports:{}};
  const ctx={exports:module.exports,module,TextEncoder,require:(specifier)=>{
    if(Object.prototype.hasOwnProperty.call(modules,specifier))return modules[specifier];
    throw new Error('No external modules allowed in subtitle QA test');
  }};
  vm.runInNewContext(code,ctx,{filename:file,timeout:1000});
  return module.exports;
}
const base=load('caption-qa.ts');
const review=load('subtitle-localization-qa.ts',{'@/lib/caption-qa':base});
const original='1\n00:00:00,000 --> 00:00:02,000\nHello there\n\n2\n00:00:02,100 --> 00:00:04,000\nSee you soon';
const translated='1\n00:00:00,000 --> 00:00:02,000\nHola amigo\n\n2\n00:00:02,100 --> 00:00:04,000\nHasta luego';
let result=review.compareLocalization(original,translated);
assert.equal(result.valid,true);
assert.equal(result.sourceCues,2);
assert.equal(result.translationCues,2);
assert.equal(result.driftedCues,0);
assert.equal(result.unchangedCues,0);
result=review.compareLocalization(original,translated.replace('00:00:02,100','00:00:02,500'));
assert.equal(result.valid,false);
assert.equal(result.driftedCues,1);
result=review.compareLocalization(original,translated.split('\n\n')[0]);
assert.equal(result.valid,false);
assert.equal(result.sourceCues,2);
assert.equal(result.translationCues,1);
result=review.compareLocalization(original,original);
assert.equal(result.valid,true);
assert.equal(result.unchangedCues,2);
result=review.compareLocalization(original,'<script>alert(1)</script>');
assert.equal(result.valid,false);
assert.ok(result.errors.length);
console.log('PASS subtitle localization QA: alignment, drift, missing cues, unchanged translation and unsafe text');
