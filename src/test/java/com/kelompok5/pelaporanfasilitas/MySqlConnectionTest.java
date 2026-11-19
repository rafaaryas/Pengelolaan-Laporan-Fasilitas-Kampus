package com.kelompok5.pelaporanfasilitas;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@Tag("mysql")
@SpringBootTest
@ActiveProfiles("local")
class MySqlConnectionTest {
    @Autowired private DataSource dataSource;

    @Test
    void connectsToRealMySqlAsApplicationUser() throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT DATABASE(), CURRENT_USER(), VERSION(), 1")) {
            assertTrue(result.next());
            assertEquals(System.getenv("DB_NAME"), result.getString(1));
            assertTrue(result.getString(2).startsWith(System.getenv("DB_USERNAME") + "@"));
            assertEquals("MySQL", connection.getMetaData().getDatabaseProductName());
            assertEquals(1, result.getInt(4));
        }
    }
}
