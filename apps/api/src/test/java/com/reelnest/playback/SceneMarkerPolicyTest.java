package com.reelnest.playback;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SceneMarkerPolicyTest {
    private static SceneMarkerPolicy.Entry scene(String label,String summary,int start,int end){
        return new SceneMarkerPolicy.Entry(label,summary,start,end);
    }
    @Test void orderedScenesWithinDurationPass() {
        var items=List.of(scene("Opening act","Story introduction",0,30),
            scene("Second act","Characters and conflict",30,90));
        assertEquals(items,SceneMarkerPolicy.validate(items,100));
        assertTrue(SceneMarkerPolicy.validate(List.of(),100).isEmpty());
    }
    @Test void overlappingOrOutOfRangeScenesFailClosed() {
        for(var items:List.of(
            List.of(scene("First","",0,40),scene("Overlap","",39,60)),
            List.of(scene("First","",-1,2)),
            List.of(scene("First","",10,10)),
            List.of(scene("First","",0,101)),
            List.of(scene("<script>","",0,12)),
            List.of(scene("First","unexpected\nnewlines",0,12))
            )) assertThrows(ResponseStatusException.class,()->SceneMarkerPolicy.validate(items,100));
    }
    @Test void invalidOversizedScenesRejected() {
        var tooMany=java.util.stream.IntStream.range(0,41)
            .mapToObj(n->scene("Scene "+n,"",n*2,n*2+1)).toList();
        assertThrows(ResponseStatusException.class,()->SceneMarkerPolicy.validate(tooMany,200));
        assertThrows(ResponseStatusException.class,()->SceneMarkerPolicy.validate(null,100));
        assertThrows(ResponseStatusException.class,()->SceneMarkerPolicy.validate(
            List.of(scene("x","",0,10)),100));
    }
}
