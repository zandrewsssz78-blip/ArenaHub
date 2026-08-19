# Requisitos No Funcionales Candidatos

## RNF-1: Latencia en consulta de disponibilidad
- Operación: consulta de escenarios disponibles por fecha (historia 3).
- Umbral: menos de 400 ms de tiempo de respuesta.
- Métrica: percentil 95 (p95).
- Carga: 50 usuarios concurrentes durante 2 minutos.
- Entorno: MySQL con semilla de 500 reservas previas, contenedor Docker, aplicación caliente (primera corrida descartada).

## RNF-2: Correctitud bajo concurrencia en reservas
- Operación: creación de reserva (historia 4) ante solicitudes simultáneas para el mismo escenario/horario.
- Umbral: 0% de reservas duplicadas o solapadas persistidas.
- Métrica: tasa de fallo del 0% en la garantía de unicidad (verificación exacta, no percentil).
- Carga: 20 solicitudes concurrentes liberadas por barrera de sincronización.
- Entorno: MySQL en contenedor Docker, mismo motor que producción, script de prueba en el repositorio.