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

/** Private rights register without contract PDFs, source keys or identity secrets. */
@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class RightsComplianceReportController {
 private final JdbcTemplate db;
 public RightsComplianceReportController(JdbcTemplate db){this.db=db;}
 @GetMapping(value="/rights.csv",produces="text/csv")
 public ResponseEntity<byte[]> rights(){
   StringBuilder csv=new StringBuilder("title_id,title_name,creator_id,rights_status,valid_from,valid_until,territories,days_remaining\r\n");
   db.query("""
     SELECT t.id,t.name,r.creator_id,r.status,r.valid_from,r.valid_until,
       COALESCE((SELECT string_agg(rt.country_code, ';' ORDER BY rt.country_code)
         FROM rights_territories rt WHERE rt.rights_id=r.id),'') AS territories,
       r.valid_until-CURRENT_DATE AS days_remaining
     FROM content_rights r JOIN titles t ON t.id=r.title_id
     ORDER BY r.valid_until ASC LIMIT 10000
     """,rs->{
        for(int i=1;i<=8;i++){if(i>1)csv.append(',');csv.append(CatalogCsvPolicy.cell(rs.getObject(i)));}
        csv.append("\r\n");
     });
   return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
     "attachment; filename=\"reelnest-rights-audit.csv\"")
     .header(HttpHeaders.CACHE_CONTROL,"no-store")
     .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
     .body(csv.toString().getBytes(StandardCharsets.UTF_8));
 }
}
