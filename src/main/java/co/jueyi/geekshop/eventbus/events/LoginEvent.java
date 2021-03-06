/*
 * Copyright (c) 2020 掘艺网络(jueyi.co).
 * All rights reserved.
 */

package co.jueyi.geekshop.eventbus.events;

import co.jueyi.geekshop.common.RequestContext;
import co.jueyi.geekshop.types.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginEvent extends BaseEvent {
    private final RequestContext ctx;
    private final User uer;
}
