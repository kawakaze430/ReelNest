package com.reelnest.admin;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Bounded read-only metadata export. Not a licence document or bulk-import facility. */
@RestController
@RequestMapping("/api/v1/admin/catalog")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogExportController {
    private final JdbcTemplate jdbc;
    public CatalogExportController(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @GetMapping(value="/export.csv",produces="text/csv")
    public ResponseEntity<byte[]> export(){
        StringBuilder csv=new StringBuilder("id,name,type,genre,language,status,age_rating,release_year\r\n");
        jdbc.query("""
          SELECT id,name,type,genre,language,status,age_rating,release_year
          FROM titles ORDER BY created_at DESC,id DESC LIMIT 10000
          """,rs->{
             for(int i=1;i<=8;i++){
                if(i>1)csv.append(',');
                csv.append(CatalogCsvPolicy.cell(rs.getObject(i)));
             }
             csv.append("\r\n");
        });
        byte[] bytes=csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"reelnest-catalog.csv\"")
          .header(HttpHeaders.CACHE_CONTROL,"no-store")
          .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
          .body(bytes);
    }
}
