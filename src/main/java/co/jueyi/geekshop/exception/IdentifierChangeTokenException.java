/*
 * Copyright (c) 2020 掘艺网络(jueyi.co).
 * All rights reserved.
 */

package co.jueyi.geekshop.exception;

/**
 * This error should be thrown when an error occurs when attempting to update a User's identifier
 * (e.g. change email address).
 *
 * Created on Nov, 2020 by @author bobo
 */
public class IdentifierChangeTokenException extends AbstractGraphqlException {
    public IdentifierChangeTokenException() {
        super(ErrorCode.NOT_RECOGNIZED_IDETIFIER_CHANGE_TOKEN);
    }
}
