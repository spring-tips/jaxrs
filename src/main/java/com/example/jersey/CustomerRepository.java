package com.example.jersey;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
interface CustomerRepository extends JpaRepository<Customer, Long> {
}
