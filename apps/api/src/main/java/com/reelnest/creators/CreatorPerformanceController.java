package com.reelnest.creators;

import com.reelnest.analytics.AggregatePrivacyPolicy;
import com.reelnest.users.CurrentUserService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Engagement-only creator statement. Deliberately NO royalty or payment claims. */
@RestController
@RequestMapping("/api/v1/creator/performance")
public class CreatorPerformanceController {
    private final JdbcTemplate db;
    private final CurrentUserService users;
    public CreatorPerformanceController(JdbcTemplate db,CurrentUserService users){this.db=db;this.users=users;}
    public record TitleStats(UUID titleId,String name,long eligibleProfiles,long starts,long completions,
                             boolean suppressionApplied) {}
    public record Statement(String note,int days,List<TitleStats> titles) {}
    @GetMapping
    public Statement statement(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="30") int days){
        if(days<1||days>90)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"days must be 1-90");
        UUID user=users.resolve(jwt).getId();
        Integer creatorCount=db.queryForObject("SELECT COUNT(*) FROM creators WHERE user_id=? AND status='APPROVED'",
            Integer.class,user);
        if(creatorCount==null || creatorCount!=1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Approved creator required");
        List<TitleStats> results=db.query("""
            SELECT t.id,t.name,COUNT(DISTINCT e.profile_id),
                   COUNT(*) FILTER(WHERE e.event_type='PLAY_START'),
                   COUNT(*) FILTER(WHERE e.event_type='PLAY_COMPLETE')
            FROM titles t JOIN creators c ON c.id=t.creator_id
            LEFT JOIN viewer_events e ON e.title_id=t.id
               AND e.occurred_at>=NOW()-(? * interval '1 day')
               AND EXISTS (SELECT 1 FROM profiles p WHERE p.id=e.profile_id AND p.personalization_enabled)
            WHERE c.user_id=?
            GROUP BY t.id,t.name ORDER BY t.name LIMIT 100
            """,(r,i)->{
                long profiles=r.getLong(3),starts=r.getLong(4),completes=r.getLong(5);
                boolean suppressed=!AggregatePrivacyPolicy.safe(profiles,starts+completes);
                return new TitleStats(r.getObject(1,UUID.class),r.getString(2),
                    suppressed?0:profiles,suppressed?0:starts,suppressed?0:completes,suppressed);
            },days,user);
        return new Statement("Consented, privacy-thresholded engagement only. NOT a royalty, income, invoice or payout statement.",days,results);
    }
}
