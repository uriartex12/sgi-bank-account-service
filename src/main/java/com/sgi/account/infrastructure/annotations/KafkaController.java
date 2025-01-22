package com.sgi.account.infrastructure.annotations;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to mark classes as Kafka controllers.
 * This annotation combines the behavior of the @Component and @Controller annotations,
 * allowing the class to be recognized as a Spring bean and a Kafka listener controller.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
@Controller
public @interface KafkaController {}
