/*
 * Copyright (c) 2020 掘艺网络(jueyi.co).
 * All rights reserved.
 */

package co.jueyi.geekshop.types.collection;

import co.jueyi.geekshop.types.asset.Asset;
import co.jueyi.geekshop.types.common.ConfigurableOperation;
import co.jueyi.geekshop.types.common.Node;
import co.jueyi.geekshop.types.product.ProductVariant;
import co.jueyi.geekshop.types.product.ProductVariantList;
import co.jueyi.geekshop.types.product.ProductVariantListOptions;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class Collection implements Node {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private String name;
    private String slug;
    private List<CollectionBreadcrumb> breadcrumbs = new ArrayList<>();
    private Integer position;
    private String description;
    private Asset featuredAsset;
    private List<Asset> assets = new ArrayList<>();
    private Collection parent;
    private List<Collection> children = new ArrayList<>();
    private List<ConfigurableOperation> filters = new ArrayList<>();
    private ProductVariantList productVariants;

    public ProductVariantList getProductVariants(ProductVariantListOptions options) {
        return null; // TODO
    }
}
