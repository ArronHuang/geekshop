/*
 * Copyright (c) 2020 掘艺网络(jueyi.co).
 * All rights reserved.
 */

package co.jueyi.geekshop.types.payment;

import co.jueyi.geekshop.types.common.ConfigArg;
import co.jueyi.geekshop.types.common.ConfigurableOperationDefinition;
import co.jueyi.geekshop.types.common.Node;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class PaymentMethod implements Node {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private String code;
    private Boolean enabled;
    private List<ConfigArg> configArgs = new ArrayList<>();
    private ConfigurableOperationDefinition definition;
}
