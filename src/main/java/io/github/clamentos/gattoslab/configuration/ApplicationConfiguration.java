package io.github.clamentos.gattoslab.configuration;

///
import io.github.clamentos.gattoslab.ingress.RateLimiter;
import io.github.clamentos.gattoslab.ingress.RequestEnricher;
import io.github.clamentos.gattoslab.ingress.SecurityInterceptor;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.session.SessionRole;
import io.github.clamentos.gattoslab.session.SessionService;

///.
import jakarta.el.PropertyNotFoundException;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    private final PropertyProvider propertyProvider;

    ///..
    private final RequestEnricher requestEnricher;
    private final RateLimiter rateLimiter;
    private final ObservabilityService observabilityService;
    private final SessionService sessionService;

    ///
    @Autowired
    public ApplicationConfiguration(

        final PropertyProvider propertyProvider,
        final RequestEnricher requestEnricher,
        final RateLimiter rateLimiter,
        final ObservabilityService observabilityService,
        final SessionService sessionService

    ) throws PropertyNotFoundException {

        this.propertyProvider = propertyProvider;

        this.requestEnricher = requestEnricher;
        this.rateLimiter = rateLimiter;
        this.observabilityService = observabilityService;
        this.sessionService = sessionService;
    }

    ///
    @Override
    public void addInterceptors(final InterceptorRegistry registry) {

        final boolean ratelimitingEnabled = propertyProvider.getProperty("app.ratelimit.enabled", Boolean.class);
        final boolean securityEnabled = propertyProvider.getProperty("app.session.enabled", Boolean.class);
        final boolean metricsEnabled = propertyProvider.getProperty("app.metrics.enabled", Boolean.class);

        int precedence = Integer.MIN_VALUE;

        registry.addInterceptor(requestEnricher).addPathPatterns("/**").order(precedence++);
        if(ratelimitingEnabled) registry.addInterceptor(rateLimiter).addPathPatterns("/**").order(precedence++);

        if(securityEnabled) {

            final SecurityInterceptor adminInterceptor = new SecurityInterceptor(propertyProvider, SessionRole.ADMIN, sessionService);
            registry.addInterceptor(adminInterceptor).addPathPatterns("/admin/**").order(precedence++);
        }

        if(metricsEnabled) registry.addInterceptor(observabilityService).addPathPatterns("/**").order(precedence);
	}

    ///..
    @Override
    public void addCorsMappings(final CorsRegistry registry) {

        registry

            .addMapping("/**")
            .allowedOrigins(propertyProvider.getProperty("app.session.cors.origins", String.class).split(","))
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
        threadPoolTaskScheduler.setVirtualThreads(true);

        return threadPoolTaskScheduler;
    }

    ///
}
