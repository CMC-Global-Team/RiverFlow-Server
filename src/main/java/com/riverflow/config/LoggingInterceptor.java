package com.riverflow.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Centralized Logging Interceptor
 * Automatically logs all HTTP requests and responses
 */
@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        
        if (queryString != null) {
            log.info("[REQUEST] {} {} - Query: {}", method, uri, queryString);
        } else {
            log.info("[REQUEST] {} {}", method, uri);
        }
        
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // Optional: can be used for additional processing
        try{
            string method = requests.getMethod();
            string uri = requests.getRequestURI();
            log.info("Post Handle: {} {}", method, uri);
        }catch(Exception e){
            log.info("Post Handle Exception: {}", e.getMessage());
        }
    }

    @Override
    public void getHandler(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Optional: can be used for additional processing
        try{
            string method = requests.getMethod();
            string uri = requests.getRequestURI();
            log.info("Get Handler: {} {}", method, uri);
        }catch(Exception e){
            log.info("Get Handler Exception: {}", e.getMessage());
        }
    }

    @Override
    public void putHandler(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Optional: can be used for additional processing
        try{
            string method = requests.getMethod();
            string uri = requests.getRequestURI();
            log.info("Put Handler: {} {}", method, uri);
        }catch(Exception e){
            log.info("Put Handler Exception: {}", e.getMessage());
        }
    }
    
    @Override
    public void deleteHandler(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Optional: can be used for additional processing
        try{
            string method = requests.getMethod();
            string uri = requests.getRequestURI();
            log.info("Delete Handler: {} {}", method, uri);
        }catch(Exception e){
            log.info("Delete Handler Exception: {}", e.getMessage());
        }
    }


    

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute("startTime");
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();
        
        if (ex != null) {
            log.error("[RESPONSE] {} {} - Status: {} - Duration: {}ms - Error: {}", 
                method, uri, status, duration, ex.getMessage());
        } else if (status >= 400) {
            log.warn("[RESPONSE] {} {} - Status: {} - Duration: {}ms", 
                method, uri, status, duration);
        } else {
            log.info("[RESPONSE] {} {} - Status: {} - Duration: {}ms", 
                method, uri, status, duration);
        }
    }
}



