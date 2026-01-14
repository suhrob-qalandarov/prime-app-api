package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.InventoryTransaction;

public interface InventoryTransactionRepositoryCustom {
    InventoryTransaction saveWithActivation(InventoryTransaction productIncome);
}


