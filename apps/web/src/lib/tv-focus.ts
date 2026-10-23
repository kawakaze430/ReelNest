/** Directional navigation for opt-in lean-back / TV browser mode. */
export type Direction = 'up'|'down'|'left'|'right';
export type FocusRect = {left:number;top:number;width:number;height:number};
export function nearestDirectional(current:FocusRect, items:FocusRect[], direction:Direction): number {
  const cx=current.left+current.width/2,cy=current.top+current.height/2;
  let best=-1,score=Infinity;
  items.forEach((r,i)=>{
    const dx=r.left+r.width/2-cx,dy=r.top+r.height/2-cy;
    const along=direction==='right'?dx:direction==='left'?-dx:direction==='down'?dy:-dy;
    if(along<=1)return;
    const lateral=(direction==='right'||direction==='left')?Math.abs(dy):Math.abs(dx);
    const rank=along+2*lateral; // prefer axis alignment over a closer diagonal
    if(rank<score){score=rank;best=i;}
  });
  return best;
}
