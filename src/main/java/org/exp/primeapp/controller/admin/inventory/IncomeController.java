package org.exp.primeapp.controller.admin.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.request.IncomeRequest;
import org.exp.primeapp.models.dto.response.admin.IncomeStatisticsResponse;
import org.exp.primeapp.models.entities.ProductIncome;
import org.exp.primeapp.models.enums.IncomeFilterType;
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

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductIncome> createIncome(@Valid @RequestBody IncomeRequest incomeRequest) {
        ProductIncome productIncome = productIncomeService.createIncome(incomeRequest);
        return new ResponseEntity<>(productIncome, HttpStatus.CREATED);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'VISITOR')")
    public ResponseEntity<IncomeStatisticsResponse> getIncomeStatistics(
            @RequestParam(value = "filter", defaultValue = "TODAY") IncomeFilterType filterType) {
        IncomeStatisticsResponse statistics = productIncomeService.getIncomeStatistics(filterType);
        return new ResponseEntity<>(statistics, HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VISITOR')")
    public ResponseEntity<IncomeStatisticsResponse> getAllIncomes(
            @RequestParam(value = "filter", defaultValue = "TODAY") IncomeFilterType filterType) {
        IncomeStatisticsResponse statistics = productIncomeService.getIncomeStatistics(filterType);
        return new ResponseEntity<>(statistics, HttpStatus.OK);
    }
}
