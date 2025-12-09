package org.exp.primeapp.service.face.admin.inventory;

import org.exp.primeapp.models.dto.request.IncomeRequest;
import org.exp.primeapp.models.entities.ProductIncome;

public interface ProductIncomeService {
    ProductIncome save(ProductIncome productIncome);
    
    ProductIncome createIncome(IncomeRequest incomeRequest);
}

