package org.exp.primeapp.controller.admin.inventory;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.request.IncomeRequest;
import org.exp.primeapp.models.entities.ProductIncome;
import org.exp.primeapp.service.face.admin.inventory.ProductIncomeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.exp.primeapp.utils.Const.*;

@RestController
@RequestMapping(API + V2 + ADMIN + "/income")
@RequiredArgsConstructor
public class IncomeController {

    private final ProductIncomeService productIncomeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductIncome> createIncome(@Valid @RequestBody IncomeRequest incomeRequest) {
        ProductIncome productIncome = productIncomeService.createIncome(incomeRequest);
        return new ResponseEntity<>(productIncome, HttpStatus.CREATED);
    }
}
