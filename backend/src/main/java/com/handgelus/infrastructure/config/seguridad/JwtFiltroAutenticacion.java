package com.handgelus.infrastructure.config.seguridad;

import com.handgelus.application.port.out.UsuarioRepositoryPort;
import com.handgelus.domain.model.Usuario;
import com.handgelus.infrastructure.adapter.out.seguridad.JwtTokenAdapter;
import com.handgelus.infrastructure.adapter.out.seguridad.UsuarioAutenticado;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el token "Bearer" de cada petición, lo valida y registra al usuario autenticado
 * con su rol (ROLE_CLIENTE, ROLE_ASISTENTE, ROLE_TATUADOR, ROLE_ADMINISTRADOR).
 * Si la cuenta fue desactivada después de emitir el token, la petición queda sin autenticar.
 */
public class JwtFiltroAutenticacion extends OncePerRequestFilter {

    private final JwtTokenAdapter jwt;
    private final UsuarioRepositoryPort usuarios;

    public JwtFiltroAutenticacion(JwtTokenAdapter jwt, UsuarioRepositoryPort usuarios) {
        this.jwt = jwt;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            jwt.validar(cabecera.substring(7).trim())
                    .filter(u -> usuarios.buscarPorId(u.id()).map(Usuario::isActivo).orElse(false))
                    .ifPresent(this::autenticar);
        }
        chain.doFilter(request, response);
    }

    private void autenticar(UsuarioAutenticado usuario) {
        var autenticacion = new UsernamePasswordAuthenticationToken(usuario, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
