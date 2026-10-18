package com.handgelus.infrastructure.adapter.in.web.soporte;

import com.handgelus.domain.exception.AccesoDenegadoException;
import com.handgelus.domain.model.Actor;
import com.handgelus.infrastructure.adapter.out.seguridad.UsuarioAutenticado;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Permite declarar un parámetro {@link Actor} en los controladores: es el usuario del token. */
@Component
public class ActorArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Actor.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuario.comoActor();
        }
        throw new AccesoDenegadoException("Debe iniciar sesión para realizar esta acción.");
    }
}
