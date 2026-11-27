package io.github.eduardosantiag0.kaizan_companion.infra.security.config;

import io.github.eduardosantiag0.kaizan_companion.infra.security.TelegramTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TelegramTokenInterceptor secretTokenInterceptor;

    @Value("${app.cors.miniapp-origin}")
    private String miniAppOrigin;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(secretTokenInterceptor)
                .addPathPatterns("/kaizan/api/v1");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/kaizan/api/v1/katago/history")
                .allowedOrigins(miniAppOrigin)
                .allowedMethods("GET")
                .allowedHeaders("Authorization");

        registry.addMapping("/kaizan/api/v1/katago/history/*")
                .allowedOrigins(miniAppOrigin)
                .allowedMethods("DELETE")
                .allowedHeaders("Authorization");

        registry.addMapping("/kaizan/api/v1/practice/today")
                .allowedOrigins(miniAppOrigin)
                .allowedMethods("GET")
                .allowedHeaders("Authorization");

        registry.addMapping("/kaizan/api/v1/practice/today/start")
                .allowedOrigins(miniAppOrigin)
                .allowedMethods("POST")
                .allowedHeaders("Authorization");

        registry.addMapping("/kaizan/api/v1/practice/today/new-round")
                .allowedOrigins(miniAppOrigin)
                .allowedMethods("POST")
                .allowedHeaders("Authorization");

        registry.addMapping("/kaizan/api/v1/practice/sessions")
                .allowedOrigins(miniAppOrigin)
                .allowedMethods("GET")
                .allowedHeaders("Authorization");

        registry.addMapping("/kaizan/api/v1/practice/sessions/*")
                .allowedOrigins(miniAppOrigin)
                .allowedMethods("GET")
                .allowedHeaders("Authorization");

        registry.addMapping("/kaizan/api/v1/practice/sessions/*/exercises/*/result")
                .allowedOrigins(miniAppOrigin)
                .allowedMethods("POST")
                .allowedHeaders("Authorization");

        registry.addMapping("/kaizan/api/v1/practice/streak")
                .allowedOrigins(miniAppOrigin)
                .allowedMethods("GET")
                .allowedHeaders("Authorization");

        registry.addMapping("/kaizan/api/v1/practice/preferences")
                .allowedOrigins(miniAppOrigin)
                .allowedMethods("GET", "PUT")
                .allowedHeaders("Authorization", "Content-Type");
    }

}
