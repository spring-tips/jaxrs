package com.example.customerservice;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;

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
import java.util.List;
import java.util.stream.Stream;

@SpringBootApplication
public class CustomerServiceApplication {

    @Bean
    ApplicationRunner data(CustomerRepository cr) {
        return args ->
                Stream.of("A", "B", "C")
                        .forEach(x -> cr.save(new Customer(null, x)));
    }

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}

@Component
class GenericExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response.serverError().entity(exception.getMessage()).build();
    }
}

@Configuration
@EnableWebSecurity
class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static UserDetails user(String usr, String... roles) {
        return User.withUsername(usr).password("password").roles(roles).build();
    }

    @Bean
    UserDetailsService customUserDetailsService() {
        List<UserDetails> details = Arrays.asList(user("rwinch", "ADMIN", "USER"),
                user("jlong", "USER"));
        return new InMemoryUserDetailsManager(details);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.httpBasic()
                .and()
                .authorizeRequests()
                .antMatchers("/customers*").hasRole("ADMIN")
                .anyRequest().authenticated();
    }
}

@Component
class BridgingSecurityFilter implements ContainerRequestFilter {

    @Context
    UriInfo uriInfo;

    private static class BridgingSecurityContext implements SecurityContext {

        private final org.springframework.security.core.context.SecurityContext spring;
        private final UriInfo uriInfo;

        private BridgingSecurityContext(org.springframework.security.core.context.SecurityContext spring, UriInfo uriInfo) {
            this.spring = spring;
            this.uriInfo = uriInfo;
        }

        @Override
        public Principal getUserPrincipal() {
            return spring.getAuthentication();
        }

        @Override
        public boolean isUserInRole(String s) {
            return spring.getAuthentication().getAuthorities()
                    .stream()
                    .anyMatch(ga -> ga.getAuthority().contains(s));
        }

        @Override
        public boolean isSecure() {
            return uriInfo.getAbsolutePath().toString().toLowerCase().startsWith("https");
        }

        @Override
        public String getAuthenticationScheme() {
            return "spring-security-authentication";
        }
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        containerRequestContext.setSecurityContext(
                new BridgingSecurityContext(SecurityContextHolder.getContext(), this.uriInfo));
    }
}

@Configuration
class JerseyConfiguration {

    @Bean
    CustomerResource customerResource(CustomerRepository repository) {
        return new CustomerResource(repository);
    }

    @Bean
    ResourceConfig config(CustomerResource cr, GenericExceptionMapper exceptionMapper) {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(exceptionMapper);
        resourceConfig.register(cr);
        return resourceConfig;
    }

}