package com.handgelus.application.service;

import com.handgelus.domain.exception.ReglaNegocioException;

/** Política mínima de contraseñas. */
final class PoliticaPassword {

    private PoliticaPassword() {
    }

    static void validar(String password) {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Za-zÁÉÍÓÚáéíóúÑñ].*") || !password.matches(".*\\d.*")) {
            throw new ReglaNegocioException("La contraseña debe tener al menos 8 caracteres e incluir letras y números.");
        }
    }
}
