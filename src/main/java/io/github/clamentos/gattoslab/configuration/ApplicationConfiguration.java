package io.github.clamentos.gattoslab.configuration;

///
import io.github.clamentos.gattoslab.admin.SecurityInterceptor;
import io.github.clamentos.gattoslab.ingress.RateLimiter;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.utils.PropertyProvider;

///.
import jakarta.el.PropertyNotFoundException;

///.
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

///
@Configuration
@EnableWebMvc

///
public class ApplicationConfiguration implements WebMvcConfigurer {

    ///
    private final String corsConfiguration;

    ///..
    private final RateLimiter rateLimiter;
    private final SecurityInterceptor securityInterceptor;
    private final ObservabilityService observabilityService;

    ///
    public ApplicationConfiguration(
        
        final RateLimiter rateLimiter,
        final SecurityInterceptor securityInterceptor,
        final ObservabilityService observabilityService,
        final PropertyProvider propertyProvider
    
    ) throws PropertyNotFoundException {

        corsConfiguration = propertyProvider.getProperty("app.cors.configuration", String.class);

        this.rateLimiter = rateLimiter;
        this.securityInterceptor = securityInterceptor;
        this.observabilityService = observabilityService;
    }

    ///
    @Override
    public void addInterceptors(final InterceptorRegistry registry) {

		registry.addInterceptor(rateLimiter).addPathPatterns("/**").order(Ordered.HIGHEST_PRECEDENCE);

        registry

            .addInterceptor(securityInterceptor)
            .addPathPatterns("/admin/**")
            .excludePathPatterns("/admin/api/session")
            .order(Ordered.HIGHEST_PRECEDENCE + 1)
        ;

        registry.addInterceptor(observabilityService).addPathPatterns("/**").order(Ordered.HIGHEST_PRECEDENCE + 2);
	}

    ///..
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry

            .addMapping("/**")
            .allowedOrigins(corsConfiguration.split(","))
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true)
        ;
    }

    ///
}
