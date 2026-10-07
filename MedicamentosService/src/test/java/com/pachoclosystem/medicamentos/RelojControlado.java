package com.pachoclosystem.medicamentos;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj en UTC cuyo instante fijan y adelantan las pruebas. */
public final class RelojControlado extends Clock {

    private volatile Instant ahora;

    public RelojControlado(Instant inicio) {
        this.ahora = inicio;
    }

    public void fijar(Instant instante) {
        this.ahora = instante;
    }

    public void avanzar(Duration tiempo) {
        this.ahora = ahora.plus(tiempo);
    }

    @Override
    public Instant instant() {
        return ahora;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }
}
