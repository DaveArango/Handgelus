package com.handgelus.infrastructure;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regla de la arquitectura hexagonal: los adaptadores de entrada (web) hablan con la aplicación
 * a través de los puertos de entrada y nunca dependen de adaptadores de salida.
 */
class ReglasDependenciaAdaptadoresTest {

    private static final String PAQUETE_WEB = "com.handgelus.infrastructure.adapter.in.web";
    private static final String PAQUETE_SALIDA = "com.handgelus.infrastructure.adapter.out.";

    @Test
    void losControladoresNoDependenDeAdaptadoresDeSalida() throws Exception {
        List<Class<?>> controladores = clasesDe(PAQUETE_WEB);
        assertFalse(controladores.isEmpty(), "No se encontraron clases en " + PAQUETE_WEB);

        List<String> violaciones = new ArrayList<>();
        for (Class<?> c : controladores) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType().getName().startsWith(PAQUETE_SALIDA)) {
                    violaciones.add(c.getSimpleName() + "." + f.getName() + " -> " + f.getType().getSimpleName());
                }
            }
            for (Constructor<?> k : c.getDeclaredConstructors()) {
                for (Class<?> p : k.getParameterTypes()) {
                    if (p.getName().startsWith(PAQUETE_SALIDA)) {
                        violaciones.add(c.getSimpleName() + "(constructor) -> " + p.getSimpleName());
                    }
                }
            }
        }
        assertTrue(violaciones.isEmpty(), "Adaptadores de entrada acoplados a adaptadores de salida: " + violaciones);
    }

    /** Clases compiladas del paquete indicado (solo el nivel del paquete, sin subpaquetes). */
    private static List<Class<?>> clasesDe(String paquete) throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource(paquete.replace('.', '/'));
        List<Class<?>> clases = new ArrayList<>();
        if (url == null) {
            return clases;
        }
        File[] archivos = new File(url.toURI()).listFiles((d, n) -> n.endsWith(".class") && !n.contains("$"));
        if (archivos == null) {
            return clases;
        }
        for (File a : archivos) {
            clases.add(Class.forName(paquete + "." + a.getName().replace(".class", "")));
        }
        return clases;
    }
}
