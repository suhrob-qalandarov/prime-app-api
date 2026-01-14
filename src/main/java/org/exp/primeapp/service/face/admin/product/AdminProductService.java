package org.exp.primeapp.service.face.admin.product;

import org.exp.primeapp.models.dto.request.ProductReq;
import org.exp.primeapp.models.dto.responce.admin.AdminProductDashboardRes;
import org.exp.primeapp.models.dto.responce.admin.AdminProductRes;
import org.exp.primeapp.models.dto.responce.user.ProductPageRes;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AdminProductService {

    AdminProductDashboardRes getProductDashboardRes();

    AdminProductRes getProductById(Long productId);

    AdminProductRes saveProduct(ProductReq productReq);

    AdminProductRes  updateProduct(Long productId, ProductReq productReq);

    AdminProductRes toggleProductUpdate(Long productId);

    List<ProductPageRes> getProductListForIncome();
}