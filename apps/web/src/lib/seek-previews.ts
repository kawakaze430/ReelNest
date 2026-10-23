/** Private, entitlement-scoped seek previews; no token-bearing image URLs. */
export type PreviewCue = {start:number;end:number;filename:string;crop?:{x:number;y:number;width:number;height:number}};
const STAMP=/^(\d{2}):(\d{2}):(\d{2})\.(\d{3})$/;
function seconds(value:string):number|null{
  const match=STAMP.exec(value);
  if(!match)return null;
  const [,hh,mm,ss,ms]=match.map(Number);
  if(mm>=60||ss>=60)return null;
  return hh*3600+mm*60+ss+ms/1000;
}
function parse(input:string,sprite:boolean):PreviewCue[]{
  if(typeof input!=='string'||input.length>40000||!input.startsWith('WEBVTT\n'))return [];
  const blocks=input.replace(/\r/g,'').split(/\n\s*\n/).slice(1);
  const result:PreviewCue[]=[];
  for(const block of blocks){
    const lines=block.trim().split('\n');
    if(lines.length!==2)return [];
    const match=/^(\S+) --> (\S+)$/.exec(lines[0]);
    if(!match)return [];
    const start=seconds(match[1]),end=seconds(match[2]);
    if(start===null||end===null||start<0||end<=start||
      (result.length&&start<result[result.length-1].end)||result.length>=120)return [];
    if(sprite){
      const image=/^(sprite_\d{2}\.jpg)#xywh=(\d+),(\d+),320,180$/.exec(lines[1]);
      if(!image)return [];
      const x=Number(image[2]),y=Number(image[3]);
      if(x>2880||y>1620||x%320!==0||y%180!==0)return [];
      result.push({start,end,filename:image[1],crop:{x,y,width:320,height:180}});
    }else{
      if(!/^thumb_\d{4}\.jpg$/.test(lines[1]))return [];
      result.push({start,end,filename:lines[1]});
    }
  }
  return result;
}
export function parsePreviewVtt(input:string):PreviewCue[]{return parse(input,false);}
export function parseSpriteVtt(input:string):PreviewCue[]{return parse(input,true);}
export function previewAt(cues:PreviewCue[],time:number):PreviewCue|null{
  if(!Number.isFinite(time)||time<0)return null;
  return cues.find(c=>time>=c.start&&time<c.end)??
    (cues.length&&time===cues[cues.length-1].end?cues[cues.length-1]:null);
}
export function previewUrl(manifestUrl:string,filename:string):string|null{
  if(!/^(thumb_\d{4}\.jpg|sprite_\d{2}\.jpg|preview\.vtt|sprites\.vtt)$/.test(filename))return null;
  if(!manifestUrl.endsWith('/master.m3u8'))return null;
  return `${manifestUrl.slice(0,-'master.m3u8'.length)}previews/${filename}`;
}
