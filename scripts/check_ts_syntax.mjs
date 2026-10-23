/** Syntax-only verification of all app TypeScript/TSX, explicitly NOT a typecheck/build. */
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import fs from 'node:fs';
const here=path.dirname(fileURLToPath(import.meta.url));
const project=path.resolve(here, '..');
const require=createRequire(import.meta.url);
let ts;
for (const loc of ['typescript',path.join(project,'apps/web/node_modules/typescript'),'/opt/nvm/versions/node/v22.16.0/lib/node_modules/typescript']) {
  try {ts=require(loc);break;}catch{}
}
if (!ts) {console.error('TypeScript compiler package missing; syntax check BLOCKED');process.exit(2);}
const root=path.join(project,'apps/web/src');
const files=[];
function visit(dir) {
  for(const entry of fs.readdirSync(dir,{withFileTypes:true})) {
    const file=path.join(dir,entry.name);
    if(entry.isDirectory()) visit(file);
    else if(/\.(tsx|ts)$/.test(entry.name)) files.push(file);
  }
}
visit(root);
let errors=0;
for(const file of files) {
  const source=ts.createSourceFile(file,fs.readFileSync(file,'utf8'),ts.ScriptTarget.Latest,true,file.endsWith('tsx')?ts.ScriptKind.TSX:ts.ScriptKind.TS);
  for(const error of source.parseDiagnostics) {
    errors++;
    const pos=source.getLineAndCharacterOfPosition(error.start??0);
    console.error(`${path.relative(project,file)}:${pos.line+1}:${pos.character+1} ${ts.flattenDiagnosticMessageText(error.messageText,' ')}`);
  }
}
console.log(`${files.length} TS/TSX files parsed; ${errors} syntax errors. This is not a semantic typecheck.`);
process.exit(errors?1:0);
