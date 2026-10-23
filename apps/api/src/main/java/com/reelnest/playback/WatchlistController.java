package com.reelnest.playback;

import com.reelnest.catalog.TitleResponse;
import com.reelnest.catalog.TitleRepository;
import com.reelnest.profiles.ProfileRepository;
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

@RestController
@RequestMapping("/api/v1/watchlist")
public class WatchlistController {
    private final JdbcTemplate jdbc;
    private final CurrentUserService users;
    private final ProfileRepository profiles;
    private final TitleRepository titles;
    public WatchlistController(JdbcTemplate jdbc,CurrentUserService users,ProfileRepository profiles,TitleRepository titles) {
        this.jdbc=jdbc;this.users=users;this.profiles=profiles;this.titles=titles;
    }
    private boolean check(Jwt jwt,UUID profileId) {
        UUID userId=users.resolve(jwt).getId();
        var profile=profiles.findByIdAndUserId(profileId,userId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile not found"));
        return profile.isChild();
    }
    @GetMapping
    @Transactional(readOnly=true)
    public List<TitleResponse> list(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID profileId) {
        boolean child=check(jwt,profileId);
        var ids=jdbc.queryForList("""
            SELECT t.id FROM watchlists w JOIN titles t ON t.id=w.title_id
            WHERE w.profile_id=? AND t.status='PUBLISHED'
            ORDER BY w.created_at DESC LIMIT 100
            """,UUID.class,profileId);
        return ids.stream().flatMap(id->titles.findByIdAndStatus(id,"PUBLISHED").stream())
            .filter(t->PlaybackPolicy.allowedAge(child,t.getAgeRating()))
            .map(TitleResponse::from).toList();
    }
    @PutMapping("/{titleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void save(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID profileId,@PathVariable UUID titleId) {
        boolean child=check(jwt,profileId);
        var title=titles.findByIdAndStatus(titleId,"PUBLISHED")
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Title not found"));
        if (!PlaybackPolicy.allowedAge(child,title.getAgeRating()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Profile age restriction");
        int inserted=jdbc.update("INSERT INTO watchlists(profile_id,title_id) VALUES (?,?) ON CONFLICT DO NOTHING",
            profileId,titleId);
        if(inserted==1) jdbc.update("""
            INSERT INTO viewer_events(id,user_id,profile_id,title_id,event_type)
            VALUES(?,?,?,?,'WATCHLIST_ADD')
            """,UUID.randomUUID(),users.resolve(jwt).getId(),profileId,titleId);
    }
    @DeleteMapping("/{titleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remove(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID profileId,@PathVariable UUID titleId) {
        check(jwt,profileId);
        jdbc.update("DELETE FROM watchlists WHERE profile_id=? AND title_id=?",profileId,titleId);
    }
}
