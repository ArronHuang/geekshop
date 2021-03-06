/*
 * Copyright (c) 2020 掘艺网络(jueyi.co).
 * All rights reserved.
 */

package co.jueyi.geekshop.exception;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class IdentifierChangeTokenExpiredException extends AbstractGraphqlException {
    public IdentifierChangeTokenExpiredException() {
        super(ErrorCode.EXPIRED_IDENTIFIER_CHANGE_TOKEN);
    }
}
