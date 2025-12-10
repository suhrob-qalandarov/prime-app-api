package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.ProductIncome;

public interface ProductIncomeRepositoryCustom {
    ProductIncome saveWithActivation(ProductIncome productIncome);
}


