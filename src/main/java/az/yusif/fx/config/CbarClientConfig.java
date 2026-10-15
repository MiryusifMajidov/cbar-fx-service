package az.yusif.fx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

@Configuration
@EnableRetry
public class CbarClientConfig {

    @Bean
    RestClient cbarRestClient(RestClient.Builder builder, FxProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.cbar().connectTimeout());
        requestFactory.setReadTimeout(properties.cbar().readTimeout());

        return builder
                .baseUrl(properties.cbar().baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
