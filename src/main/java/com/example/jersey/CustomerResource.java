package com.example.jersey;

import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.util.Collection;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Produces(MediaType.APPLICATION_JSON_VALUE)
@Path("/customers")
public class CustomerResource {

    private final CustomerRepository customerRepository;

    CustomerResource(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GET
    @Path("/{id}")
    public Customer byId(@PathParam("id") Long id, @Context SecurityContext ctx) {

        LogFactory.getLog(getClass()).info(ctx.getUserPrincipal().getName() + " was here.");

        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("couldn't find a record with ID #" + id + "!"));
    }

    @GET
    public Collection<Customer> customers() {
        return customerRepository.findAll();
    }
}
