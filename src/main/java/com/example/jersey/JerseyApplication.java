package com.example.jersey;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.stream.Stream;

@SpringBootApplication
public class JerseyApplication {

    public static void main(String[] args) {
        SpringApplication.run(JerseyApplication.class, args);
    }
}

@Priority(Priorities.AUTHENTICATION)
@Component
class BridgingSecurityFilter implements ContainerRequestFilter {

    @Context
    UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestContext.setSecurityContext(new BridgingSecurityContext(this.uriInfo,
                SecurityContextHolder.getContext()));
    }

    public static class BridgingSecurityContext implements SecurityContext {

        private final UriInfo uriInfo;
        private final org.springframework.security.core.context.SecurityContext spring;

        public BridgingSecurityContext(UriInfo info,
                                       org.springframework.security.core.context.SecurityContext spring) {
            this.spring = spring;
            this.uriInfo = info;
        }

        @Override
        public Principal getUserPrincipal() {
            return () -> spring.getAuthentication().getName();
        }

        @Override
        public boolean isUserInRole(String role) {
            return Authentication.class.cast(
                    spring.getAuthentication()).getAuthorities()
                    .stream()
                    .anyMatch(x -> x.getAuthority().toLowerCase().contains(role));
        }

        @Override
        public boolean isSecure() {
            return this.uriInfo.getAbsolutePath().toString().toLowerCase().startsWith("https");
        }

        @Override
        public String getAuthenticationScheme() {
            return "spring-security";
        }
    }
}

@Configuration
@EnableWebSecurity
class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http

                .authorizeRequests()
                .antMatchers("/customers*").hasRole("ADMIN")
                .anyRequest().authenticated()
                .and()
                .httpBasic();
    }

    @Bean
    UserDetailsService customUserDetailsService() {
        UserDetails rob = User.withUsername("rwinch").password("password").roles("ADMIN", "USER").build();
        UserDetails josh = User.withUsername("jlong").password("password").roles("USER").build();
        return new InMemoryUserDetailsManager(Arrays.asList(rob, josh));
    }

}

@Configuration
class JerseyConfig {

    @Component
    static class CustomExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

        @Override
        public Response toResponse(IllegalArgumentException exception) {
            return Response.serverError().entity(exception.getMessage()).build();
        }
    }

    @Bean
    ApplicationRunner init(CustomerRepository cr) {
        return args ->
                Stream.of("A", "B", "C")
                        .forEach(c -> cr.save(new Customer(null, c)));
    }

    @Bean
    CustomerResource customerResource(CustomerRepository r) {
        return new CustomerResource(r);
    }


    @Bean
    ResourceConfig config(CustomerResource cr,
                          BridgingSecurityFilter securityFilter,
                          ExceptionMapper<IllegalArgumentException> em) {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(em);
        resourceConfig.register(cr);
        resourceConfig.register(securityFilter);
        return resourceConfig;
    }
}