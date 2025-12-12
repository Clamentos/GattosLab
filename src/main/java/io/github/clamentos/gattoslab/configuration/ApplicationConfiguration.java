package io.github.clamentos.gattoslab.configuration;

///
import io.github.clamentos.gattoslab.admin.SecurityInterceptor;
import io.github.clamentos.gattoslab.ingress.RateLimiter;
import io.github.clamentos.gattoslab.observability.ObservabilityService;

///.
import jakarta.el.PropertyNotFoundException;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
    @Autowired
    public ApplicationConfiguration(

        final PropertyProvider propertyProvider,
        final RateLimiter rateLimiter,
        final SecurityInterceptor securityInterceptor,
        final ObservabilityService observabilityService

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
    public void addCorsMappings(final CorsRegistry registry) {

        registry

            .addMapping("/**")
            .allowedOrigins(corsConfiguration.split(","))
            .allowedMethods("GET", "POST", "PUT", "DELETE")
        ;
    }

    ///..
    @Bean
    public TaskScheduler batchScheduler(final PropertyProvider propertyProvider) throws PropertyNotFoundException {

        final ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();

        threadPoolTaskScheduler.setPoolSize(propertyProvider.getProperty("app.batch.poolSize", Integer.class));
        threadPoolTaskScheduler.setAwaitTerminationSeconds(propertyProvider.getProperty("app.batch.terminationWaitPeriod", Integer.class));
        threadPoolTaskScheduler.setThreadNamePrefix("GattosLabBatch");
        threadPoolTaskScheduler.setVirtualThreads(false);

        return threadPoolTaskScheduler;
    }

    ///
}
