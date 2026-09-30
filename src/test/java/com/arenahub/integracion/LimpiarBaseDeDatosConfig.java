package com.arenahub.integracion;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Antes de las pruebas borra el esquema de pruebas y vuelve a aplicar las
 * migraciones: así la prueba también detecta errores de migración
 * (frontera "Migración" de docs/esqueleto/.../contrato-prueba-reserva.md).
 */
@TestConfiguration
public class LimpiarBaseDeDatosConfig {

    @Bean
    FlywayMigrationStrategy limpiarYMigrar() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
