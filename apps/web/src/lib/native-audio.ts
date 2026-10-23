/** Safari's optional AudioTrackList is not part of every browser's DOM types.
 * Access is feature-detected; never assume a browser supports native track selection.
 */
export type NativeAudioTrack = { language: string; label: string; enabled: boolean };
export type NativeAudioTrackList = {
  length: number;
  [index: number]: NativeAudioTrack;
  addEventListener?: (type: string, listener: EventListener) => void;
  removeEventListener?: (type: string, listener: EventListener) => void;
};

export function nativeAudioList(video: {audioTracks?: NativeAudioTrackList}): NativeAudioTrackList|null {
  const list=video.audioTracks;
  return list && Number.isInteger(list.length) && list.length>=0 && list.length<=32 ? list : null;
}

export function nativeAudioOptions(list: NativeAudioTrackList): {id:number;language:string;label:string}[] {
  const options=[];
  for(let i=0;i<list.length;i++) {
    const track=list[i];
    if(!track)continue;
    options.push({id:i,language:track.language||'',label:track.label||`Audio ${i+1}`});
  }
  return options;
}

export function selectNativeAudio(list: NativeAudioTrackList, id:number):boolean {
  if(!Number.isInteger(id) || id<0 || id>=list.length || !list[id])return false;
  for(let i=0;i<list.length;i++) if(list[i])list[i].enabled=i===id;
  return true;
}
