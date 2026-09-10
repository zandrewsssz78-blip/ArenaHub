# TAREA 4 — Contrato de la prueba única

## Rebanada

Registro de un usuario.

## Especificación de la prueba end-to-end

### Punto de entrada

La prueba comienza fuera de la aplicación mediante una solicitud HTTP de registro de usuario.

La solicitud contiene:

* nombre
* correo
* contraseña

### Precondición de datos

La base de datos PostgreSQL real debe estar disponible y contar con la estructura necesaria para la tabla `USUARIOS`.

El correo utilizado en la prueba no debe existir previamente en la base de datos.

### Acción

Enviar la solicitud HTTP de registro con los datos válidos definidos para la rebanada.

### Aserción observable

La prueba debe comprobar que la aplicación devuelve una respuesta de registro exitoso y que el usuario creado recibe un `id` asignado.

También debe comprobar que la contraseña no se almacena en texto plano, sino mediante el campo correspondiente al hash.

### Estado esperado en la base de datos real

Después del registro debe existir un único registro correspondiente al correo utilizado.

El registro debe contener el nombre enviado, un `id` asignado y el valor correspondiente a la contraseña almacenada como hash.

## Cobertura de fronteras

| Frontera        | ¿La prueba la detecta? | Evidencia                                                                                   |
| --------------- | ---------------------- | ------------------------------------------------------------------------------------------- |
| Entrada externa | Sí                     | La prueba comienza con una solicitud HTTP real.                                             |
| Aplicación      | Sí                     | La solicitud debe atravesar la aplicación para producir el resultado esperado.              |
| Mapeo           | Sí                     | Los datos recibidos deben terminar correctamente en `USUARIOS`.                             |
| Driver          | Sí                     | La aplicación debe comunicarse correctamente con PostgreSQL para persistir los datos.       |
| Migración       | Parcialmente           | Se detecta si la prueba utiliza una base de datos nueva que requiere ejecutar la migración. |
| Base de datos   | Sí                     | Se verifica directamente el estado persistido en PostgreSQL real.                           |
| Respuesta       | Sí                     | La prueba comprueba la respuesta HTTP observable.                                           |

## Frontera que podría romperse sin ser detectada

La migración podría romperse sin que la prueba lo detecte si la base de datos utilizada para la prueba ya contiene previamente la estructura correcta.

Para cubrir esta frontera, la prueba debe ejecutarse contra una base de datos PostgreSQL nueva o vacía, de manera que la estructura de `USUARIOS` tenga que ser creada mediante la migración definida para el proyecto.

## Criterio de éxito

La prueba será satisfactoria cuando una única solicitud de registro atraviese correctamente todas las fronteras:

HTTP → aplicación → mapeo → driver → PostgreSQL

y produzca simultáneamente:

1. una respuesta HTTP exitosa;
2. un usuario con `id` asignado;
3. una contraseña almacenada como hash;
4. un único registro correspondiente al correo utilizado en `USUARIOS`.
