/** Browser diagnostics only. A supported key system is NOT a DRM licence or encrypted media entitlement. */
export type DrmSystem='Widevine'|'PlayReady'|'FairPlay';
export type Capability={name:DrmSystem;keySystem:string;supported:boolean;reason:string};
export const keySystems:ReadonlyArray<{name:DrmSystem;keySystem:string;videoType:string}>=[
 {name:'Widevine',keySystem:'com.widevine.alpha',videoType:'video/mp4; codecs="avc1.42E01E"'},
 {name:'PlayReady',keySystem:'com.microsoft.playready',videoType:'video/mp4; codecs="avc1.42E01E"'},
 {name:'FairPlay',keySystem:'com.apple.fps',videoType:'video/mp4; codecs="avc1.42E01E"'},
];
export async function probeDrm():Promise<Capability[]>{
 if(typeof window==='undefined'||!window.isSecureContext)
   return keySystems.map(k=>({...k,supported:false,reason:'HTTPS / secure context required'}));
 if(!navigator.requestMediaKeySystemAccess)
   return keySystems.map(k=>({...k,supported:false,reason:'Encrypted Media Extensions unavailable'}));
 const result:Capability[]=[];
 for(const item of keySystems){
  try{
   await navigator.requestMediaKeySystemAccess(item.keySystem,[{
    initDataTypes:['cenc'],
    audioCapabilities:[{contentType:'audio/mp4; codecs="mp4a.40.2"'}],
    videoCapabilities:[{contentType:item.videoType}],
    distinctiveIdentifier:'not-allowed',persistentState:'not-allowed',
    sessionTypes:['temporary']
   }]);
   result.push({...item,supported:true,reason:'Browser advertises temporary EME support; no licence server configured'});
  }catch{
   result.push({...item,supported:false,reason:'Browser rejected this key-system configuration'});
  }
 }
 return result;
}
