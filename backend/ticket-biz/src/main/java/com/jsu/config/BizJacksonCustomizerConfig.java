package com.jsu.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BizJacksonCustomizerConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer bizObjectMapperVisibilityCustomizer() {
        return builder -> builder.visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    }
}
