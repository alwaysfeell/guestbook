package sumdu.edu.ua.web.security;

import jakarta.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.stream.Collectors;

@Component
public class SecurityAuditListener {
    private static final Logger log = LoggerFactory.getLogger(SecurityAuditListener.class);

    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        log.info("AUDIT login success: user='{}', roles={}, ip={}, request={}",
                auth.getName(), roles(auth), ip(auth), currentRequest());
    }

    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        Authentication auth = event.getAuthentication();
        log.warn("AUDIT login failure: user='{}', reason={}, ip={}, request={}", auth.getName(),
                event.getException().getClass().getSimpleName(), ip(auth), currentRequest());
    }

    @EventListener
    public void onLogout(LogoutSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        log.info("AUDIT logout: user='{}', ip={}, request={}", auth.getName(), ip(auth), currentRequest());
    }

    @EventListener
    public void onAccessDenied(AuthorizationDeniedEvent<?> event) {
        Authentication auth = event.getAuthentication().get();
        String user = auth == null || auth instanceof AnonymousAuthenticationToken ? "anonymous" : auth.getName();
        log.warn("AUDIT access denied: user='{}', target={}", user, target(event.getObject()));
    }

    private static String roles(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
    }

    private static String ip(Authentication auth) {
        return auth.getDetails() instanceof WebAuthenticationDetails details ? details.getRemoteAddress() : "-";
    }

    private static String currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return target(attrs.getRequest());
        }
        return "-";
    }

    private static String target(Object object) {
        if (object instanceof HttpServletRequest request) {
            return request.getMethod() + " " + request.getRequestURI();
        }
        if (object instanceof MethodInvocation invocation) {
            return invocation.getMethod().getDeclaringClass().getSimpleName() + "." + invocation.getMethod().getName() + "()";
        }
        return String.valueOf(object);
    }
}
