/*
 * Copyright (c) 2020 掘艺网络(jueyi.co).
 * All rights reserved.
 */

package co.jueyi.geekshop.types.settings;

import lombok.Data;

import java.util.Date;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class GlobalSettings {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private Boolean trackInventory;
    private ServerConfig serverConfig;
}
