package io.github.clamentos.gattoslab.configuration;

///
import io.github.clamentos.gattoslab.admin.SecurityInterceptor;
import io.github.clamentos.gattoslab.ingress.RateLimiter;
import io.github.clamentos.gattoslab.metrics.MetricsService;
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
    private final RateLimiter rateLimiter;
    private final SecurityInterceptor securityInterceptor;
    private final MetricsService metricsService;

    ///..
    private final String corsConfiguration;

    ///
    public ApplicationConfiguration(
        
        final RateLimiter rateLimiter,
        final SecurityInterceptor securityInterceptor,
        final MetricsService metricsService,
        final PropertyProvider propertyProvider
    
    ) throws PropertyNotFoundException {

        this.rateLimiter = rateLimiter;
        this.securityInterceptor = securityInterceptor;
        this.metricsService = metricsService;

        corsConfiguration = propertyProvider.getProperty("app.cors.configuration", String.class);
    }

    ///
    @Override
    public void addInterceptors(final InterceptorRegistry registry) {

		registry.addInterceptor(rateLimiter).addPathPatterns("/**").order(Ordered.HIGHEST_PRECEDENCE);
        registry.addInterceptor(metricsService).addPathPatterns("/**").order(Ordered.HIGHEST_PRECEDENCE + 2);

        registry

            .addInterceptor(securityInterceptor)
            .addPathPatterns("/admin/**")
            .excludePathPatterns("/admin/session")
            .order(Ordered.HIGHEST_PRECEDENCE + 1)
        ;
	}

    ///..
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping(corsConfiguration);
    }

    ///
}
