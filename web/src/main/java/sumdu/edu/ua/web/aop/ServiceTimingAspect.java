package sumdu.edu.ua.web.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Aspect
@Component
@Order(1)
public class ServiceTimingAspect {
    private static final Logger log = LoggerFactory.getLogger(ServiceTimingAspect.class);

    @Around("execution(* sumdu.edu.ua.core.service..*(..))")
    public Object measure(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String method = signature.getDeclaringType().getSimpleName() + "." + signature.getName() + "()";
        long start = System.nanoTime();
        try {
            Object result = pjp.proceed();
            log.info("[timing] {} took {} ms", method, elapsed(start));
            return result;
        } catch (Throwable ex) {
            log.info("[timing] {} took {} ms and threw {}", method, elapsed(start), ex.getClass().getSimpleName());
            throw ex;
        }
    }

    private static String elapsed(long start) {
        return String.format(Locale.ROOT, "%.2f", (System.nanoTime() - start) / 1_000_000.0);
    }
}
