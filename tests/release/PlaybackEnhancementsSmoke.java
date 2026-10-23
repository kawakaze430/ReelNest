import com.reelnest.playback.PlaybackCues;
import com.reelnest.playback.ResumePolicy;

/** Pure-Java checks with javac: no Maven/Spring runtime required. */
public final class PlaybackEnhancementsSmoke {
    static int checks=0;
    static void eq(Object actual,Object expected) {
        if(!java.util.Objects.equals(actual,expected))
            throw new AssertionError("Expected "+expected+" but got "+actual);
        checks++;
    }
    public static void main(String[] args) {
        eq(ResumePolicy.resumePosition(-1,100),0);
        eq(ResumePolicy.resumePosition(0,100),0);
        eq(ResumePolicy.resumePosition(40,100),40);
        eq(ResumePolicy.resumePosition(95,100),0);
        eq(ResumePolicy.resumePosition(3590,3600),0);
        eq(ResumePolicy.resumePosition(3500,3600),3500);
        eq(ResumePolicy.resumePosition(25,30),25);
        eq(ResumePolicy.resumePosition(29,30),0);
        eq(ResumePolicy.resumePosition(500,0),0);
        eq(ResumePolicy.boundedProgress(105,100),100);
        eq(ResumePolicy.boundedProgress(100,100),100);
        boolean threw=false;
        try { ResumePolicy.boundedProgress(116,100); } catch(IllegalArgumentException e) {threw=true;}
        eq(threw,true);
        threw=false;
        try { ResumePolicy.boundedProgress(5,0); } catch(IllegalArgumentException e) {threw=true;}
        eq(threw,true);
        eq(PlaybackCues.EMPTY.validFor(100),true);
        eq(new PlaybackCues(0,15,20,25,85).validFor(100),true);
        eq(new PlaybackCues(10,10,null,null,null).validFor(100),false);
        eq(new PlaybackCues(null,10,null,null,null).validFor(100),false);
        eq(new PlaybackCues(0,110,null,null,null).validFor(100),false);
        eq(new PlaybackCues(null,null,null,null,100).validFor(100),false);
        eq(new PlaybackCues(null,null,0,5,99).validFor(100),true);
        System.out.println(checks+" playback enhancement policy assertions passed");
    }
}
