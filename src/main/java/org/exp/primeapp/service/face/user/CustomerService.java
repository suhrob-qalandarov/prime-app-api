package org.exp.primeapp.service.face.user;

import org.exp.primeapp.models.entities.Customer;
import org.exp.primeapp.models.entities.User;

public interface CustomerService {
    Customer getOrCreateCustomer(String phoneNumber, String fullName, User currentUser);
}
