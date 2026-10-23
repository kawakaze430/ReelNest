package com.reelnest.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Only explicit editorial approval + currently licensed, ready media is indexable.
 * This public endpoint returns metadata, never private licence documents or S3 keys.
 */
@RestController
@RequestMapping("/api/v1/catalog")
public class PublicSeoController {
    private final JdbcTemplate jdbc;
    private final String country;
    public PublicSeoController(JdbcTemplate jdbc,
        @Value("${app.playback.trusted-country:IN}") String country) {
        this.jdbc=jdbc;
        if(!country.matches("[A-Z]{2}")) throw new IllegalArgumentException("Invalid catalog SEO country");
        this.country=country;
    }
    public record SeoTitle(UUID id,String name,String synopsis,String type,String genre,
                           String language,String ageRating,Integer releaseYear) {}
    private static final String ELIGIBLE = """
        FROM titles t
        JOIN creators c ON c.id=t.creator_id AND c.status='APPROVED'
        JOIN content_rights r ON r.title_id=t.id AND r.status='APPROVED'
        JOIN rights_territories rt ON rt.rights_id=r.id AND rt.country_code=?
        WHERE t.status='PUBLISHED' AND t.seo_index_approved=TRUE
          AND r.valid_from<=CURRENT_DATE AND r.valid_until>=CURRENT_DATE
          AND EXISTS(SELECT 1 FROM media_assets m WHERE m.title_id=t.id
                     AND m.processing_status='READY' AND m.manifest_key IS NOT NULL)
        """;
    private static final String FIELDS = "SELECT t.id,t.name,t.synopsis,t.type,t.genre,t.language,t.age_rating,t.release_year ";
    private static final org.springframework.jdbc.core.RowMapper<SeoTitle> MAPPER=(rs,n)->
        new SeoTitle(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),
            rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),
            (Integer)rs.getObject(8));

    @GetMapping("/{id}/seo")
    public SeoTitle eligibleTitle(@PathVariable UUID id) {
        return jdbc.query(FIELDS+ELIGIBLE+" AND t.id=? LIMIT 1",MAPPER,country,id)
            .stream().findFirst().orElseThrow(()->new ResponseStatusException(
                HttpStatus.NOT_FOUND,"No approved public SEO metadata for this title"));
    }
    @GetMapping("/seo-index")
    public List<SeoTitle> eligibleTitles(@RequestParam(defaultValue="0") int page,
                                        @RequestParam(defaultValue="100") int size) {
        if(page<0 || page>1000 || size<1 || size>100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid SEO pagination");
        return jdbc.query(FIELDS+ELIGIBLE+" ORDER BY t.id LIMIT ? OFFSET ?",
            MAPPER,country,size,page*size);
    }
}
