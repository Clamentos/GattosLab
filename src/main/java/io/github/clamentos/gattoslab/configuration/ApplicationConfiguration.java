package io.github.clamentos.gattoslab.configuration;

///
import io.github.clamentos.gattoslab.ratelimiting.RateLimiter;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

///
@Configuration

///
public class ApplicationConfiguration implements WebMvcConfigurer {

    ///
    private final RateLimiter rateLimiter;

    ///
    @Autowired
    public ApplicationConfiguration(final RateLimiter rateLimiter) {

        this.rateLimiter = rateLimiter;
    }

    ///
    @Override
    public void addInterceptors(final InterceptorRegistry registry) {

		registry.addInterceptor(rateLimiter).addPathPatterns("/**").order(Ordered.HIGHEST_PRECEDENCE);
	}

    ///
}
