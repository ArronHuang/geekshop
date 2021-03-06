/*
 * Copyright (c) 2020 掘艺网络(jueyi.co).
 * All rights reserved.
 */

package co.jueyi.geekshop.types.administrator;

import co.jueyi.geekshop.custom.validator.ValidPassword;
import lombok.Data;

import javax.validation.constraints.Email;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class CreateAdministratorInput {
    private String firstName;
    private String lastName;
    @Email
    private String emailAddress;
    @ValidPassword
    private String password;
    private List<Long> roleIds = new ArrayList<>();
}
