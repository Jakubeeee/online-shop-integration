package com.jakubeeee.integration.service;

import com.jakubeeee.core.service.RestService;
import com.jakubeeee.integration.enums.DataSourceType;
import com.jakubeeee.integration.enums.ProductMappingKey;
import com.jakubeeee.integration.model.CommonProduct;
import com.jakubeeee.integration.model.ShoperProduct;
import com.jakubeeee.integration.model.ShoperProductsCollection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

import static com.jakubeeee.common.utils.LangUtils.toSet;
import static com.jakubeeee.common.utils.ThreadUtils.sleep;
import static com.jakubeeee.integration.enums.DataSourceType.SHOP_PLATFORM;
import static com.jakubeeee.integration.enums.ProductMappingKey.CODE;
import static com.jakubeeee.integration.enums.ProductMappingKey.NAME;
import static com.jakubeeee.integration.model.ProductsTask.UpdatableProperty;
import static com.jakubeeee.integration.model.ProductsTask.UpdatableProperty.*;
import static java.util.Objects.requireNonNull;

@Slf4j
@Service
public class ShoperDataSource implements UpdatableDataSource<ShoperProduct> {

    @Autowired
    RestService restService;

    @Autowired
    ShoperAuthService authService;

    @Value("${shoperProductStockUri}")
    String SHOPER_PRODUCT_STOCK_URI;

    @Value("${shoperAllProductsUri}")
    String SHOPER_ALL_PRODUCTS_URI;

    @Getter
    private Set<ProductMappingKey> allowedProductMappingKeys;

    @Getter
    private Set<UpdatableProperty> allowedUpdatableProperties;

    @PostConstruct
    void initialize() {
        allowedProductMappingKeys = toSet(NAME, CODE);
        allowedUpdatableProperties = toSet(STOCK, PRICE, EAN);
    }

    @Override
    public List<ShoperProduct> getExternalProducts() {
        return getProductsFromAllPages(getPageAmount());
    }

    private List<ShoperProduct> getProductsFromAllPages(int pageAmount) {
        List<ShoperProduct> productList = new ArrayList<>();
        for (int i = 1; i <= pageAmount; i++)
            productList.addAll(getAllProductsFromPage(i));
        return productList;
    }

    private List<ShoperProduct> getAllProductsFromPage(int page) {
        HttpHeaders headers = restService.generateHeaderWithAuthToken(authService.getTokenValue());
        ResponseEntity<ShoperProductsCollection> response = restService.getJsonObject(
                SHOPER_ALL_PRODUCTS_URI + "?limit=50&page=" + page,
                new HttpEntity<>(headers), ShoperProductsCollection.class);
        return requireNonNull(response.getBody()).getList();
    }

    private int getPageAmount() {
        HttpHeaders headers = restService.generateHeaderWithAuthToken(authService.getTokenValue());
        ResponseEntity<ShoperProductsCollection> response = restService.getJsonObject(
                SHOPER_ALL_PRODUCTS_URI + "?limit=50",
                new HttpEntity<>(headers), ShoperProductsCollection.class);
        return requireNonNull(response.getBody()).getPages();
    }

    @Override
    public List<CommonProduct> convertToCommonProducts(List<ShoperProduct> externalProducts) {
        var commonProducts = new ArrayList<CommonProduct>();
        externalProducts.forEach(externalProduct -> {
            CommonProduct commonProduct = CommonProduct.builder()
                    .mappingKeyValue(externalProduct.getCode())
                    .code(externalProduct.getCode())
                    .stock(externalProduct.getProductStock())
                    .priceBrutto(externalProduct.getProductPrice())
                    .ean(externalProduct.getEan())
                    .build();
            commonProduct.addParam("SHOPER_ID", externalProduct.getId());
            commonProducts.add(commonProduct);
        });
        return commonProducts;
    }

    @Override
    public void handleSingleProductUpdate(CommonProduct commonProduct, List<UpdatableProperty> properties, boolean isTesting) {
        if (!isTesting) {
            HttpHeaders headers = restService.generateHeaderWithAuthToken(authService.getTokenValue());
            var requestParams = getRequestParamsForUpdate(commonProduct, properties);
            restService.putJsonObject(
                    SHOPER_PRODUCT_STOCK_URI + "/" + commonProduct.getParam("SHOPER_ID"),
                    new HttpEntity<>(requestParams, headers), Void.class);
        }
        sleep(50);
    }

    private Map<String, String> getRequestParamsForUpdate(CommonProduct commonProduct, List<UpdatableProperty> properties) {
        var requestParams = new HashMap<String, String>();
        for (var property : properties) {
            switch (property) {
                case STOCK:
                    requestParams.put("stock", commonProduct.getStock());
                    break;
                case PRICE:
                    requestParams.put("price", commonProduct.getPriceBrutto());
                    break;
                case EAN:
                    requestParams.put("ean", commonProduct.getEan());
                    break;
            }
        }
        return requestParams;
    }

    @Override
    public void afterHandlingFinishedAction() {
        // no actions are necessary after the update
    }

    @Override
    public String getServiceName() {
        return "SHOPER";
    }

    @Override
    public DataSourceType getType() {
        return SHOP_PLATFORM;
    }

}
