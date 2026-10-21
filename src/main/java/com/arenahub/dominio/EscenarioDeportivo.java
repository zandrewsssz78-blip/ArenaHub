package com.arenahub.dominio;

import jakarta.persistence.*;

@Entity
@Table(name = "escenario_deportivo")
public class EscenarioDeportivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEscenario tipo;

    @Column(nullable = false, length = 200)
    private String ubicacion;

    private Integer capacidad;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEscenario estado;

    protected EscenarioDeportivo() {
        // requerido por JPA
    }

    public EscenarioDeportivo(String nombre, TipoEscenario tipo, String ubicacion, EstadoEscenario estado) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.ubicacion = ubicacion;
        this.estado = estado;
    }

    public EscenarioDeportivo(String nombre, TipoEscenario tipo, String ubicacion,
                              Integer capacidad, String descripcion, EstadoEscenario estado) {
        this(nombre, tipo, ubicacion, estado);
        this.capacidad = capacidad;
        this.descripcion = descripcion;
    }

    public EscenarioDeportivo(Long id, String nombre, TipoEscenario tipo, String ubicacion,
                              Integer capacidad, String descripcion, EstadoEscenario estado) {
        this(nombre, tipo, ubicacion, capacidad, descripcion, estado);
        this.id = id;
    }

    /** Regla del dominio: solo los escenarios ACTIVOS reciben reservas. */
    public boolean admiteReservas() {
        return estado == EstadoEscenario.ACTIVO;
    }

    public void desactivar() {
        this.estado = EstadoEscenario.INACTIVO;
    }

    public void activar() {
        this.estado = EstadoEscenario.ACTIVO;
    }

    public void cambiarEstado(EstadoEscenario nuevoEstado) {
        if (nuevoEstado == null) {
            throw new ReglaNegocioException("El nuevo estado no puede ser nulo");
        }
        this.estado = nuevoEstado;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public TipoEscenario getTipo() { return tipo; }
    public String getUbicacion() { return ubicacion; }
    public Integer getCapacidad() { return capacidad; }
    public String getDescripcion() { return descripcion; }
    public EstadoEscenario getEstado() { return estado; }
}
