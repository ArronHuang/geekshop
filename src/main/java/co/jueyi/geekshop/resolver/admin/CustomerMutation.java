/*
 * Copyright (c) 2020 掘艺网络(jueyi.co).
 * All rights reserved.
 */

package co.jueyi.geekshop.resolver.admin;

import co.jueyi.geekshop.common.RequestContext;
import co.jueyi.geekshop.common.utils.BeanMapper;
import co.jueyi.geekshop.custom.security.Allow;
import co.jueyi.geekshop.entity.AddressEntity;
import co.jueyi.geekshop.entity.CustomerEntity;
import co.jueyi.geekshop.service.CustomerService;
import co.jueyi.geekshop.types.address.Address;
import co.jueyi.geekshop.types.common.*;
import co.jueyi.geekshop.types.customer.*;
import co.jueyi.geekshop.types.history.HistoryEntry;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class CustomerMutation implements GraphQLMutationResolver {
    private final CustomerService customerService;

    /**
     * Create a new Customer. If a password is provided, a new User will also be created and linked to the Customer.
     */
    @Allow(Permission.CreateCustomer)
    public Customer createCustomer(CreateCustomerInput input, String password, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        CustomerEntity customerEntity = this.customerService.create(ctx, input, password);
        return BeanMapper.map(customerEntity, Customer.class);
    }

    /**
     * Update an existing Customer
     */
    @Allow(Permission.UpdateCustomer)
    public Customer adminUpdateCustomer(UpdateCustomerInput input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        CustomerEntity customerEntity = this.customerService.update(ctx, input);
        return BeanMapper.map(customerEntity, Customer.class);
    }

    /**
     * Delete a Customer
     */
    @Allow(Permission.DeleteCustomer)
    public DeletionResponse deleteCustomer(Long id, DataFetchingEnvironment dfe) {
        return this.customerService.softDelete(id);
    }

    /**
     * Create a new Address and associate it with the Customer specified by customerId
     */
    @Allow(Permission.CreateCustomer)
    public Address adminCreateCustomerAddress(Long customerId, CreateAddressInput input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        AddressEntity addressEntity = this.customerService.createAddress(ctx, customerId, input);
        return BeanMapper.map(addressEntity, Address.class);
    }

    /**
     * Update an existing Address
     */
    @Allow(Permission.UpdateCustomer)
    public Address adminUpdateCustomerAddress(UpdateAddressInput input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        AddressEntity addressEntity = this.customerService.updateAddress(ctx, input);
        return BeanMapper.map(addressEntity, Address.class);
    }

    /**
     * Delete an existing Address
     */
    @Allow(Permission.DeleteCustomer)
    public Boolean adminDeleteCustomerAddress(Long id, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        return this.customerService.deleteAddress(ctx, id);
    }

    @Allow(Permission.UpdateCustomer)
    public Customer addNoteToCustomer(AddNoteToCustomerInput input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        CustomerEntity customerEntity = this.customerService.addNoteToCustomer(ctx, input);
        return BeanMapper.map(customerEntity, Customer.class);
    }

    @Allow(Permission.UpdateCustomer)
    public HistoryEntry updateCustomerNode(UpdateCustomerNoteInput input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        return this.customerService.updateCustomerNode(ctx, input);
    }

    @Allow(Permission.UpdateCustomer)
    public DeletionResponse deleteCustomerNote(Long id, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        return this.customerService.deleteCustomerNote(ctx, id);
    }
}
