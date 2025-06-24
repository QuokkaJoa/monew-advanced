package com.part2.monew.config;

import com.part2.monew.entity.DataSourceType;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class RoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {

        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return DataSourceType.STANDBY;

        }
            return DataSourceType.MAIN;

    }
}