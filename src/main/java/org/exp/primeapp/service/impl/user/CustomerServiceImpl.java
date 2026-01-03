package org.exp.primeapp.service.impl.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.entities.Customer;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.CustomerRepository;
import org.exp.primeapp.service.face.user.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public Customer getOrCreateCustomer(String phoneNumber, String fullName, User currentUser) {
        log.info("Process customer: phone={}, name={}", phoneNumber, fullName);

        return customerRepository.findByPhoneNumber(phoneNumber)
                .map(customer -> {
                    if (!customer.getFullName().equals(fullName)) {
                        customer.setFullName(fullName);
                        return customerRepository.save(customer);
                    }
                    return customer;
                })
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .phoneNumber(phoneNumber)
                            .fullName(fullName)
                            .profile(currentUser)
                            .orderAmount(0)
                            .isNew(true)
                            .build();
                    return customerRepository.save(newCustomer);
                });
    }
}
