package com.example.datasecurity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchemaConsistencyTests {
    @Test
    void h2AndSqlServerScriptsDefineSameCoreTables() throws Exception {
        Set<String> h2 = tables(Path.of("src/main/resources/schema.sql"));
        Set<String> sqlServer = tables(Path.of("../sql/sqlserver_schema_init.sql"));
        assertEquals(h2, sqlServer);
    }

    private Set<String> tables(Path path) throws Exception {
        String sql = Files.readString(path).toLowerCase();
        Matcher matcher = Pattern.compile("create\\s+table\\s+(?:dbo\\.)?([a-z_]+)").matcher(sql);
        Set<String> tables = new TreeSet<>();
        while (matcher.find()) tables.add(matcher.group(1));
        return tables;
    }
}
