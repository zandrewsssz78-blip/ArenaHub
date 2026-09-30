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

    /** Regla del dominio: solo los escenarios ACTIVOS reciben reservas. */
    public boolean admiteReservas() {
        return estado == EstadoEscenario.ACTIVO;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public TipoEscenario getTipo() { return tipo; }
    public String getUbicacion() { return ubicacion; }
    public Integer getCapacidad() { return capacidad; }
    public String getDescripcion() { return descripcion; }
    public EstadoEscenario getEstado() { return estado; }
}
