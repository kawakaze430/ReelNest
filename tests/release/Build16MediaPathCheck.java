import com.reelnest.playback.PlaybackPolicy;
public class Build16MediaPathCheck {
  public static void main(String[] args) {
    String[] allowed={"master.m3u8","720p/index.m3u8","720p/seg_00000.ts",
      "previews/preview.vtt","previews/thumb_0001.jpg",
      "previews/sprites.vtt","previews/sprite_01.jpg","artwork/poster.jpg",
      "captions/en/index.m3u8","captions/hi/captions.vtt","captions/en-us/index.m3u8",
      "audio/a00/index.m3u8","audio/a07/seg_00001.ts"};
    String[] denied={"previews/../../secrets.txt","previews/other.vtt","previews/thumb_0001.png",
      "previews/thumb_00001.jpg","/previews/preview.vtt","previews/%2e%2e/preview.vtt",
      "previews//preview.vtt","previews/./preview.vtt","previews/thumb_0001.jpg?token=x","private.m3u8","secret.ts",
      "../360p/index.m3u8","720p/seg_00000.ts/other","previews/sprite_001.jpg","captions/../../secrets.vtt",
      "captions/en/private.vtt","captions/EN/index.m3u8","captions/en/secret.ts",
      "audio/a08/index.m3u8","audio/a01/../index.m3u8","audio/a01/seg_00001.aac",
      "audio/a01/seg_00001.ts?auth=1","audio/a01/private.m3u8"};
    for(String path:allowed) if(!PlaybackPolicy.safeRelativeMediaPath(path))throw new AssertionError("Allowed: "+path);
    for(String path:denied) if(PlaybackPolicy.safeRelativeMediaPath(path))throw new AssertionError("Denied: "+path);
    System.out.println("Build 18 Java private media path policy: 36 assertions passed");
  }
}
