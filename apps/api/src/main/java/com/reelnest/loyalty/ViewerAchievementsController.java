package com.reelnest.loyalty;

import com.reelnest.users.CurrentUserService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Opt-in non-monetary milestones from trusted playback completion events. */
@RestController
@RequestMapping("/api/v1/profiles/{profileId}/achievements")
public class ViewerAchievementsController {
    private final JdbcTemplate db;
    private final CurrentUserService users;
    public ViewerAchievementsController(JdbcTemplate db,CurrentUserService users) {
        this.db=db;this.users=users;
    }
    public record Badge(String code,String label,boolean earned,int distinctTitlesRequired) {}
    public record Summary(boolean enabled,int completedDistinctTitles,String scope,List<Badge> badges) {}
    @GetMapping
    @Transactional(readOnly=true)
    public Summary overview(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID profileId) {
        UUID user=users.resolve(jwt).getId();
        var rows=db.query("""
            SELECT p.is_child,p.personalization_enabled FROM profiles p
            WHERE p.id=? AND p.user_id=?
            """,(rs,i)->new boolean[]{rs.getBoolean(1),rs.getBoolean(2)},profileId,user);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile not found");
        boolean[] state=rows.getFirst();
        // No incentives for children; disabling personalization stops access.
        if(state[0]||!state[1])return summary(false,0);
        Integer unique=db.queryForObject("""
            SELECT COUNT(DISTINCT title_id) FROM viewer_events
            WHERE user_id=? AND profile_id=? AND event_type='PLAY_COMPLETE'
              AND occurred_at>=NOW()-interval '90 days'
            """,Integer.class,user,profileId);
        return summary(true,Math.max(0,unique==null?0:unique));
    }
    static Summary summary(boolean enabled,int completed) {
        int count=enabled?Math.max(0,completed):0;
        return new Summary(enabled,count,"Completed titles in the past 90 days; non-redeemable",
            List.of(new Badge("FIRST_STORY","First story",enabled&&count>=1,1),
                new Badge("THREE_STORIES","Three stories",enabled&&count>=3,3),
                new Badge("FIVE_STORIES","Five stories",enabled&&count>=5,5)));
    }
}
