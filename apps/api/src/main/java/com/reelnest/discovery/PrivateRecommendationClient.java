package com.reelnest.discovery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Optional internal ML ranking. It receives ONLY Java-rights-filtered candidates.
 * A model outage never bypasses rights checks; it falls back to the Java ordering.
 */
@Component
public class PrivateRecommendationClient {
    private static final Logger log=LoggerFactory.getLogger(PrivateRecommendationClient.class);
    private final boolean enabled;
    private final String secret;
    private final RestClient client;
    public record Candidate(String id,String title,String synopsis,String genre,String language) {}
    public record Request(List<Candidate> candidates,List<String> seenIds,String preferredLanguage,int limit) {}
    public record Ranked(String id,double score) {}
    public record Reply(List<Ranked> ranked) {}

    public PrivateRecommendationClient(@Value("${app.recommendation.enabled:false}") boolean enabled,
            @Value("${app.recommendation.url:http://recommendation-api:8000}") String url,
            @Value("${app.recommendation.internal-token:}") String secret) {
        if(enabled && (secret==null || secret.length()<24))
            throw new IllegalStateException("ML enabled without a strong internal token");
        this.enabled=enabled;
        this.secret=secret;
        SimpleClientHttpRequestFactory factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(300));
        factory.setReadTimeout(Duration.ofMillis(1100));
        this.client=RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
    /** Rights-filtered search candidates only. Empty result rather than irrelevant fallback for no matches. */
    public List<DiscoveryService.Item> semanticSearch(List<DiscoveryService.Item> eligible,String query,int limit) {
        if(query==null || query.isBlank() || query.length()>200 || limit<1 || limit>24)
            throw new IllegalArgumentException("Invalid query or limit");
        if(eligible.isEmpty())return List.of();
        Map<String,DiscoveryService.Item> allowed=new LinkedHashMap<>();
        List<Candidate> candidates=new ArrayList<>();
        for(var item:eligible) {
            var t=item.title();String id=t.id().toString();
            allowed.put(id,item);
            candidates.add(new Candidate(id,t.name(),t.synopsis()==null?"":t.synopsis(),
                t.genre()==null?"":t.genre(),t.language()==null?"":t.language()));
        }
        if(enabled) {
            try {
                Reply response=client.post().uri("/semantic-search")
                    .header("X-ReelNest-Internal-Token",secret)
                    .body(Map.of("candidates",candidates,"query",query,"limit",limit))
                    .retrieve().body(Reply.class);
                if(response!=null && response.ranked()!=null) {
                    Set<String> used=new HashSet<>();List<DiscoveryService.Item> result=new ArrayList<>();
                    for(Ranked rank:response.ranked()) {
                        if(rank!=null && rank.score()>0 && Double.isFinite(rank.score())
                            && allowed.containsKey(rank.id()) && used.add(rank.id()))result.add(allowed.get(rank.id()));
                        if(result.size()>=limit)break;
                    }
                    return result;
                }
            } catch(RestClientException ex) {
                log.warn("Private search unavailable; using filtered lexical fallback: {}",ex.getClass().getSimpleName());
            }
        }
        String needle=query.toLowerCase(java.util.Locale.ROOT).trim();
        return eligible.stream().filter(item->{var t=item.title();
            return (t.name()!=null && t.name().toLowerCase(java.util.Locale.ROOT).contains(needle)) ||
                   (t.genre()!=null && t.genre().toLowerCase(java.util.Locale.ROOT).contains(needle)) ||
                   (t.synopsis()!=null && t.synopsis().toLowerCase(java.util.Locale.ROOT).contains(needle));
        }).limit(limit).toList();
    }

    public List<DiscoveryService.Item> rerank(List<DiscoveryService.Item> eligible,
                                              Set<UUID> seen,String language,int limit) {
        if(!enabled || eligible.isEmpty())return eligible.stream().limit(limit).toList();
        Map<String,DiscoveryService.Item> allowed=new LinkedHashMap<>();
        List<Candidate> candidates=new ArrayList<>();
        for(DiscoveryService.Item item:eligible) {
            var t=item.title();
            String id=t.id().toString();
            allowed.put(id,item);
            candidates.add(new Candidate(id,t.name(),t.synopsis()==null?"":t.synopsis(),
                    t.genre()==null?"":t.genre(),t.language()==null?"":t.language()));
        }
        List<String> filteredSeen=seen.stream().map(UUID::toString)
            .filter(allowed::containsKey).toList();
        try {
            Reply response=client.post().uri("/rank")
                .header("X-ReelNest-Internal-Token",secret)
                .body(new Request(candidates,filteredSeen,language,limit))
                .retrieve().body(Reply.class);
            if(response==null || response.ranked()==null) return eligible.stream().limit(limit).toList();
            List<DiscoveryService.Item> ranked=new ArrayList<>();
            Set<String> used=new HashSet<>();
            for(Ranked item:response.ranked()) {
                // Never trust the ML boundary for catalog eligibility or duplicates.
                if(item!=null && allowed.containsKey(item.id()) && used.add(item.id())) {
                    ranked.add(allowed.get(item.id()));
                    if(ranked.size()>=limit) return ranked;
                }
            }
            for(var item:eligible) {
                if(used.add(item.title().id().toString()))ranked.add(item);
                if(ranked.size()>=limit)break;
            }
            return ranked;
        } catch(RestClientException ex) {
            log.warn("Internal ML ranker unavailable; using licensed Java fallback: {}",ex.getClass().getSimpleName());
            return eligible.stream().limit(limit).toList();
        }
    }
}
