package br.senai.healthcare.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * registra o segurancainterceptor para toda a api,
 * menos o login e o status (que precisam funcionar sem estar logado).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SegurancaInterceptor seguranca;

    public WebConfig(SegurancaInterceptor seguranca) {
        this.seguranca = seguranca;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(seguranca)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/status");
    }
}
