package com.reelnest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the actual migration history of an old disposable V1-V9 database. */
class FlywayUpgradeTest {
    @TempDir Path legacyDirectory;

    @Test
    void upgradesVersionNineToTwentySixInPostgres() throws Exception {
        String url=System.getenv("REELNEST_UPGRADE_DB_URL");
        assumeTrue(url!=null && url.startsWith("jdbc:postgresql://127.0.0.1:5432/reelnest_upgrade")
            && "true".equals(System.getenv("REELNEST_DISPOSABLE_DB")),
            "External PostgreSQL upgrades only run against the explicitly isolated CI database");
        String user=System.getenv().getOrDefault("DB_USER","reelnest");
        String password=System.getenv().getOrDefault("DB_PASSWORD","");
        var migrationDir=Path.of("src/main/resources/db/migration");
        Pattern firstNine=Pattern.compile("V[1-9]__[^/\\\\]+\\.sql");
        int copied=0;
        try (var files=Files.list(migrationDir)) {
            for(Path source:files.toList()) {
                if(firstNine.matcher(source.getFileName().toString()).matches()){
                    Files.copy(source,legacyDirectory.resolve(source.getFileName()));
                    copied++;
                }
            }
        }
        assertEquals(9,copied,"Legacy migration snapshot must contain exactly V1-V9");
        Flyway initial=Flyway.configure().dataSource(url,user,password)
            .locations("filesystem:"+legacyDirectory.toAbsolutePath()).load();
        assertEquals(9,initial.migrate().migrationsExecuted,"Clean legacy database V1-V9");

        Flyway upgrade=Flyway.configure().dataSource(url,user,password)
            .locations("classpath:db/migration").load();
        assertEquals(17,upgrade.migrate().migrationsExecuted,"Migrate existing V9 to V26");
        List<Integer> applied=new ArrayList<>();
        try(var conn=DriverManager.getConnection(url,user,password);
            var statement=conn.createStatement();
            var result=statement.executeQuery(
                "SELECT version,success FROM flyway_schema_history ORDER BY installed_rank")){
            while(result.next()) {
                assertTrue(result.getBoolean("success"),"No failed migration allowed");
                applied.add(Integer.parseInt(result.getString("version")));
            }
        }
        assertEquals(java.util.stream.IntStream.rangeClosed(1,26).boxed().toList(),applied);
        assertEquals(0,upgrade.migrate().migrationsExecuted,"Upgrade must be idempotent");
    }
}
