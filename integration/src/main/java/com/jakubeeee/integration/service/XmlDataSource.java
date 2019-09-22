package com.jakubeeee.integration.service;

import com.jakubeeee.core.service.RestService;
import com.jakubeeee.integration.model.ExternalProduct;
import lombok.RequiredArgsConstructor;
import org.dom4j.Document;

import static com.jakubeeee.common.XmlUtils.stringToXml;

/**
 * Base for service beans used for extracting data from simple xml rest web service.
 */
@RequiredArgsConstructor
public abstract class XmlDataSource<T extends ExternalProduct> implements DataSource<T> {

    private final RestService restService;

    protected Document getProductsXml() {
        String productsXmlAsString = restService.getString(getProductsXmlWebServiceUri());
        return stringToXml(productsXmlAsString).orElseThrow(
                () -> new RuntimeException("An error has occurred while reading products xml file"));
    }

    protected abstract String getProductsXmlWebServiceUri();

}
