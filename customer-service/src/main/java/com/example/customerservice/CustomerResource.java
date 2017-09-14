package com.example.customerservice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.Collection;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
public class CustomerResource {

    private final Log log = LogFactory.getLog(getClass());
    private final CustomerRepository customerRepository;


    public CustomerResource(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GET
    public Collection<Customer> customers() {
        return this.customerRepository.findAll();
    }

    @GET
    @Path("/{id}")
    public Customer byId(@PathParam("id") Long id, @Context SecurityContext context) {

        this.log.info(context.getUserPrincipal().getName() + " was here.");

        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("couldn't find #" + id + "!"));
    }
}
