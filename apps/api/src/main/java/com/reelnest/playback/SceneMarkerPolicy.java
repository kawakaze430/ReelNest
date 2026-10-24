package com.reelnest.playback;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Pure bounds checks; prevents overlapping, out-of-order, malformed or spoiler-sized scene metadata. */
public final class SceneMarkerPolicy {
    private SceneMarkerPolicy() {}
    public record Entry(String label, String summary, Integer startSeconds, Integer endSeconds) {}
    public static List<Entry> validate(List<Entry> input,int duration) {
        if(input==null || input.size()>40 || duration<1 || duration>86400)
            throw invalid();
        int lastEnd=0;
        for(Entry e:input) {
            if(e==null || e.startSeconds()==null || e.endSeconds()==null ||
               e.startSeconds()<lastEnd || e.startSeconds()<0 ||
               e.endSeconds()<=e.startSeconds() || e.endSeconds()>duration ||
               invalidText(e.label(),2,90) || invalidText(e.summary(),0,240)) throw invalid();
            lastEnd=e.endSeconds();
        }
        return List.copyOf(input);
    }
    private static boolean invalidText(String s,int min,int max) {
        if(s==null || s.length()>max || s.strip().length()<min) return true;
        for(int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if(c=='<' || c=='>' || c=='\u0000' || Character.isISOControl(c)) return true;
        }
        return false;
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Scenes must be ordered, non-overlapping, within duration, and contain safe short labels");
    }
}
