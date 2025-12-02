
package com.riverflow.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    // --- Pointcuts ---

    /** Controller layer: all methods under com.riverflow.controller.. */
    @Pointcut("execution(* com.riverflow.controller..*(..))")
    public void allControllerMethods() {}

    /** Service layer: all methods under com.riverflow.service.. */
    @Pointcut("execution(* com.riverflow.service..*(..))")
    public void allServiceMethods() {}

    /** Optional: classes annotated with @RestController (Spring endpoints) */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void allRestControllers() {}

    /** Optional: methods annotated with any Spring request mapping */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void allRequestMappings() {}

    /** Combined endpoint pointcut: either controller classes or request-mapped methods */
    @Pointcut("allRestControllers() || allRequestMappings()")
    public void allEndpointMethods() {}

    // --- Advices for Service layer ---

    @Before("allServiceMethods()")
    public void logBeforeService(JoinPoint jp) {
        log.info("[SERVICE] Before {}.{} args={}",
                jp.getSignature().getDeclaringTypeName(),
                jp.getSignature().getName(),
                jp.getArgs());
    }

    @After("allServiceMethods()")
    public void logAfterService(JoinPoint jp) {
        log.info("[SERVICE] After {}.{}",
                jp.getSignature().getDeclaringTypeName(),
                jp.getSignature().getName());
    }

    @AfterReturning(pointcut = "allServiceMethods()", returning = "result")
    public void logAfterReturningService(JoinPoint jp, Object result) {
        log.info("[SERVICE] {}.{} returned: {}",
                jp.getSignature().getDeclaringTypeName(),
                jp.getSignature().getName(),
                result);
    }

    @AfterThrowing(pointcut = "allServiceMethods()", throwing = "ex")
    public void logAfterThrowingService(JoinPoint jp, Throwable ex) {
        log.error("[SERVICE] {}.{} threw: {}",
                jp.getSignature().getDeclaringTypeName(),
                jp.getSignature().getName(),
                ex.toString(), ex);
    }

    @Around("allServiceMethods()")
    public Object logAroundService(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String cls = pjp.getSignature().getDeclaringTypeName();
        String method = pjp.getSignature().getName();
        log.debug("[SERVICE] Enter {}.{} args={}", cls, method, pjp.getArgs());
        try {
            Object ret = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.debug("[SERVICE] Exit {}.{} took {}ms return={}", cls, method, elapsed, ret);
            return ret;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[SERVICE] Error in {}.{} after {}ms: {}", cls, method, elapsed, t.toString(), t);
            throw t;
        }
    }

    // --- Advices for Controller/Endpoint layer ---

    @Before("allEndpointMethods()")
    public void logBeforeEndpoint(JoinPoint jp) {
        log.info("[HTTP] Before {}.{} args={}",
                jp.getSignature().getDeclaringTypeName(),
                jp.getSignature().getName(),
                jp.getArgs());
    }

    @After("allEndpointMethods()")
    public void logAfterEndpoint(JoinPoint jp) {
        log.info("[HTTP] After {}.{}", 
                jp.getSignature().getDeclaringTypeName(),
                jp.getSignature().getName());
    }

    @AfterReturning(pointcut = "allEndpointMethods()", returning = "result")
    public void logAfterReturningEndpoint(JoinPoint jp, Object result) {
        log.info("[HTTP] {}.{} returned: {}",
                jp.getSignature().getDeclaringTypeName(),
                jp.getSignature().getName(),
                result);
    }

    @AfterThrowing(pointcut = "allEndpointMethods()", throwing = "ex")
    public void logAfterThrowingEndpoint(JoinPoint jp, Throwable ex) {
        log.error("[HTTP] {}.{} threw: {}", 
                jp.getSignature().getDeclaringTypeName(),
                jp.getSignature().getName(),
                ex.toString(), ex);
    }

    @Around("allEndpointMethods()")
    public Object logAroundEndpoint(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String cls = pjp.getSignature().getDeclaringTypeName();
        String method = pjp.getSignature().getName();
        log.debug("[HTTP] Enter {}.{} args={}", cls, method, pjp.getArgs());
        try {
            Object ret = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.debug("[HTTP] Exit {}.{} took {}ms return={}", cls, method, elapsed, ret);
            return ret;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[HTTP] Error in {}.{} after {}ms: {}", cls, method, elapsed, t.toString(), t);
            throw t;
        }
    }
}
