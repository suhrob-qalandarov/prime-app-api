package org.exp.primeapp.service.face.user;

import org.exp.primeapp.models.dto.request.CreateOrderReq;
import org.exp.primeapp.models.dto.responce.order.UserOrderRes;
import org.exp.primeapp.models.dto.responce.order.UserProfileOrdersRes;
import org.exp.primeapp.models.entities.Session;
import org.exp.primeapp.models.entities.User;

public interface OrderService {
    UserOrderRes createOrder(User user, Session session, CreateOrderReq orderRequest);

    UserProfileOrdersRes getUserProfileOrdersById(Long id);
}
