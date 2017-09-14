package com.example.customerservice;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
