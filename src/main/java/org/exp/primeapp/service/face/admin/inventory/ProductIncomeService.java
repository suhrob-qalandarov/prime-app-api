package org.exp.primeapp.service.face.admin.inventory;

import org.exp.primeapp.models.dto.request.IncomeRequest;
import org.exp.primeapp.models.dto.response.admin.IncomeStatisticsResponse;
import org.exp.primeapp.models.entities.ProductIncome;
import org.exp.primeapp.models.enums.IncomeFilterType;

public interface ProductIncomeService {
    ProductIncome save(ProductIncome productIncome);
    
    ProductIncome createIncome(IncomeRequest incomeRequest);
    
    IncomeStatisticsResponse getIncomeStatistics(IncomeFilterType filterType);
}

