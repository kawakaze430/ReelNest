/* No cookies, user identifiers or access tokens; read-only public catalog.
   Device login, DRM licences, content rights and video URLs are NOT implemented. */
(function(root){
  'use strict';
  function validateOrigin(input){
    if(typeof input!=='string')return '';
    try{
      const url=new URL(input);
      if(url.protocol!=='https:' || url.username || url.password ||
         url.pathname!=='/' || url.search || url.hash)return '';
      return url.origin;
    }catch{return '';}
  }
  function normalizeTitle(item){
    if(!item || typeof item!=='object' ||
       typeof item.id!=='string' || !/^[a-fA-F0-9-]{36}$/.test(item.id) ||
       typeof item.name!=='string' || item.name.length<1 || item.name.length>255)return null;
    return {id:item.id,name:item.name,synopsis:typeof item.synopsis==='string'?item.synopsis.slice(0,500):'',
      genre:typeof item.genre==='string'?item.genre.slice(0,80):'',ageRating:typeof item.ageRating==='string'?item.ageRating.slice(0,20):''};
  }
  function directionalNext(index,length,key,columns){
    if(length<=0)return -1;
    const offset=key==='ArrowLeft'?-1:key==='ArrowRight'?1:key==='ArrowUp'?-columns:key==='ArrowDown'?columns:0;
    const candidate=index+offset;
    return offset!==0 && candidate>=0 && candidate<length?candidate:index;
  }
  const api={validateOrigin,normalizeTitle,directionalNext};
  if(typeof module==='object'&&module.exports){module.exports=api;return;}
  const doc=root.document;
  const status=doc.getElementById('status');
  const container=doc.getElementById('titles');
  const detail=doc.getElementById('detail');
  const title=doc.getElementById('detail-title');
  const description=doc.getElementById('detail-description');
  const close=doc.getElementById('close-detail');
  const origin=validateOrigin(root.REELNEST_API_ORIGIN);
  let entries=[];
  function focusTiles(){const tiles=container.querySelectorAll('button');if(tiles.length)tiles[0].focus();}
  function show(item){
    title.textContent=item.name;description.textContent=item.synopsis;
    detail.hidden=false;container.hidden=true;close.focus();
  }
  close.addEventListener('click',()=>{detail.hidden=true;container.hidden=false;focusTiles();});
  doc.addEventListener('keydown',event=>{
    if(event.key==='Escape'||event.key==='Backspace'||event.key==='BrowserBack'){
      if(!detail.hidden){event.preventDefault();close.click();}return;
    }
    if(!detail.hidden)return;
    if(!['ArrowUp','ArrowDown','ArrowLeft','ArrowRight'].includes(event.key))return;
    const buttons=Array.from(container.querySelectorAll('button'));
    const index=buttons.indexOf(doc.activeElement);
    if(index<0)return;
    const columns=doc.documentElement.clientWidth<1050?2:4;
    const next=directionalNext(index,buttons.length,event.key,columns);
    if(next!==index){event.preventDefault();buttons[next].focus();}
  });
  if(!origin){status.textContent='Catalog unavailable: package this client with a reviewed HTTPS API origin.';return;}
  const controller=new AbortController();
  const timeout=root.setTimeout(()=>controller.abort(),10000);
  root.fetch(origin+'/api/v1/catalog?page=0&size=24',{method:'GET',mode:'cors',credentials:'omit',cache:'no-store',signal:controller.signal})
    .then(r=>{if(!r.ok)throw new Error('Catalog unavailable');return r.json();})
    .then(json=>{
      if(!json || !Array.isArray(json.content))throw new Error('Invalid catalog response');
      entries=json.content.slice(0,24).map(normalizeTitle).filter(Boolean);
      status.textContent=entries.length?'Select a title with your remote. Video playback is not enabled.':'No published titles available.';
      for(const item of entries){
        const button=doc.createElement('button');button.type='button';button.className='tile';
        const name=doc.createElement('strong');name.textContent=item.name;
        const meta=doc.createElement('small');meta.textContent=item.genre+(item.ageRating?' · '+item.ageRating:'');
        button.append(name,meta);button.addEventListener('click',()=>show(item));container.append(button);
      }
      focusTiles();
    })
    .catch(()=>{status.textContent='Catalog unavailable. Check API connectivity and authorized origin.';})
    .finally(()=>root.clearTimeout(timeout));
})(typeof window!=='undefined'?window:{});
