package com.monef;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.client.RestClientCustomizer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
@Configuration
class WebConfig {
 @Bean Jackson2ObjectMapperBuilderCustomizer exactMoney(){return builder -> builder.serializerByType(BigDecimal.class,ToStringSerializer.instance);}
 @Bean RestClientCustomizer boundedHttp(){return builder -> {var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());factory.setReadTimeout(Duration.ofSeconds(18));builder.requestFactory(factory);};}
}
