const assert=require('node:assert/strict');
const client=require('../shared/tv.js');
assert.equal(client.validateOrigin('https://api.example.org'),'https://api.example.org');
for(const input of ['','http://api.example.org','https://user:pass@api.example.org',
  'javascript:alert(1)','https://api.example.org/x','https://api.example.org/?key=foo']){
  assert.equal(client.validateOrigin(input),'');
}
const sample={id:'00000000-0000-0000-0000-000000000001',name:'Published',
 synopsis:'Rights-verified title',genre:'Drama',ageRating:'U'};
assert.equal(client.normalizeTitle(sample).name,'Published');
assert.equal(client.normalizeTitle({...sample,name:'X'.repeat(300)}),null);
assert.equal(client.normalizeTitle({...sample,id:'../private'}),null);
assert.equal(client.directionalNext(0,12,'ArrowRight',4),1);
assert.equal(client.directionalNext(0,12,'ArrowDown',4),4);
assert.equal(client.directionalNext(0,12,'ArrowLeft',4),0);
console.log('PASS TV public catalog client origin/metadata/remote navigation');
