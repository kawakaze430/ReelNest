#!/usr/bin/env bash
set -euo pipefail
# Produce an ORIGINAL artificial sample clip and HLS assets, not copyrighted film footage.
# Requires FFmpeg compiled with libx264 and drawtext.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MEDIA="${ROOT}/apps/web/public/sample"
mkdir -p "$MEDIA/hls"
FONT="${REELNEST_DEMO_FONT:-/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf}"
if [[ ! -f "$FONT" ]]; then
  echo "Set REELNEST_DEMO_FONT to an installed TTF font path." >&2; exit 2
fi
ffmpeg -hide_banner -loglevel error -y \
  -f lavfi -i 'color=c=0x131728:s=960x540:r=24:d=8' \
  -vf "drawbox=x=670:y=40:w=230:h=460:color=0x633f86@0.65:t=fill,drawbox=x=710:y=90:w=230:h=340:color=0xe2627d@0.3:t=fill,drawgrid=w=80:h=80:t=1:c=white@0.06,drawtext=fontfile=${FONT}:text='REELNEST':fontcolor=white:fontsize=74:x=55:y=185,drawtext=fontfile=${FONT}:text='ORIGINAL TEST CLIP':fontcolor=0xff9b91:fontsize=28:x=61:y=290,drawtext=fontfile=${FONT}:text='8 seconds - fictional media':fontcolor=0xd8c9d9:fontsize=19:x=61:y=344,fade=t=in:st=0:d=0.7,fade=t=out:st=7:d=1" \
  -an -c:v libx264 -preset veryfast -crf 24 -pix_fmt yuv420p -movflags +faststart \
  "$MEDIA/demo.mp4"
# Local public HLS demo only. Do not use this publicly accessible path for premium content.
ffmpeg -hide_banner -loglevel error -y \
  -i "$MEDIA/demo.mp4" -c:v libx264 -preset veryfast -crf 26 \
  -force_key_frames 'expr:gte(t,n_forced*2)' -g 48 -keyint_min 48 -sc_threshold 0 \
  -hls_time 2 -hls_playlist_type vod -hls_flags independent_segments \
  -hls_segment_filename "$MEDIA/hls/segment_%03d.ts" \
  -f hls "$MEDIA/hls/index.m3u8"
ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1 "$MEDIA/demo.mp4"
echo "Generated original test MP4 + HLS: $MEDIA"
