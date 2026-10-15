package az.yusif.fx.config;

import az.yusif.fx.service.ApiClientService;
import az.yusif.fx.web.filter.ApiKeyFilter;
import az.yusif.fx.web.filter.ErrorResponseWriter;
import az.yusif.fx.web.filter.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {

    @Bean
    FilterRegistrationBean<ApiKeyFilter> apiKeyFilter(ApiClientService apiClientService,
                                                      FxProperties properties,
                                                      ErrorResponseWriter errorWriter) {
        var filter = new ApiKeyFilter(apiClientService, properties.security().adminKey(), errorWriter);
        var registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilter(ErrorResponseWriter errorWriter) {
        var registration = new FilterRegistrationBean<>(new RateLimitFilter(errorWriter));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
