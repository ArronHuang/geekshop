/*
 * Copyright (c) 2020 掘艺网络(jueyi.co).
 * All rights reserved.
 */

package co.jueyi.geekshop.resolver;

import co.jueyi.geekshop.common.Constant;
import co.jueyi.geekshop.types.administrator.Administrator;
import co.jueyi.geekshop.types.history.HistoryEntry;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
public class HistoryResolver implements GraphQLResolver<Administrator> {
    public CompletableFuture<Administrator> getAdministrator(HistoryEntry historyEntry, DataFetchingEnvironment dfe) {
        final DataLoader<Long, Administrator> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_HISTORY_ENTRY_ADMINISTRATOR);

        return dataLoader.load(historyEntry.getAdministratorId());
    }
}
