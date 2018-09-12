/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.core.jdbc.core.statement;

import com.google.common.base.Optional;
import io.shardingsphere.core.constant.SQLType;
import io.shardingsphere.core.event.ShardingEventBusInstance;
import io.shardingsphere.core.event.merger.MergeEvent;
import io.shardingsphere.core.event.routing.RoutingEvent;
import io.shardingsphere.core.executor.BatchPreparedStatementExecuteUnit;
import io.shardingsphere.core.executor.batch.BatchPreparedStatementExecutor;
import io.shardingsphere.core.executor.prepared.PreparedStatementExecutor;
import io.shardingsphere.core.executor.sql.SQLExecuteUnit;
import io.shardingsphere.core.executor.sql.execute.result.StreamQueryResult;
import io.shardingsphere.core.jdbc.adapter.AbstractShardingPreparedStatementAdapter;
import io.shardingsphere.core.jdbc.core.ShardingContext;
import io.shardingsphere.core.jdbc.core.connection.ShardingConnection;
import io.shardingsphere.core.jdbc.core.resultset.GeneratedKeysResultSet;
import io.shardingsphere.core.jdbc.core.resultset.ShardingResultSet;
import io.shardingsphere.core.jdbc.metadata.JDBCTableMetaDataConnectionManager;
import io.shardingsphere.core.merger.MergeEngine;
import io.shardingsphere.core.merger.MergeEngineFactory;
import io.shardingsphere.core.merger.MergedResult;
import io.shardingsphere.core.merger.QueryResult;
import io.shardingsphere.core.metadata.table.executor.TableMetaDataLoader;
import io.shardingsphere.core.parsing.parser.sql.dal.DALStatement;
import io.shardingsphere.core.parsing.parser.sql.dml.insert.InsertStatement;
import io.shardingsphere.core.parsing.parser.sql.dql.DQLStatement;
import io.shardingsphere.core.parsing.parser.sql.dql.select.SelectStatement;
import io.shardingsphere.core.routing.PreparedStatementRoutingEngine;
import io.shardingsphere.core.routing.SQLRouteResult;
import io.shardingsphere.core.routing.router.sharding.GeneratedKey;
import lombok.AccessLevel;
import lombok.Getter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * PreparedStatement that support sharding.
 * 
 * @author zhangliang
 * @author caohao
 * @author maxiaoguang
 * @author panjuan
 */
@Getter
public final class ShardingPreparedStatement extends AbstractShardingPreparedStatementAdapter {
    
    private final ShardingConnection connection;
    
    private final PreparedStatementRoutingEngine routingEngine;
    
    private final List<BatchPreparedStatementExecuteUnit> batchStatementUnits = new LinkedList<>();
    
    private final String sql;

    private int batchCount;
    
    @Getter(AccessLevel.NONE)
    private final PreparedStatementExecutor preparedStatementExecutor;
    
    @Getter(AccessLevel.NONE)
    private final BatchPreparedStatementExecutor batchPreparedStatementExecutor;
    
    @Getter(AccessLevel.NONE)
    private SQLRouteResult routeResult;
    
    @Getter(AccessLevel.NONE)
    private ResultSet currentResultSet;
    
    public ShardingPreparedStatement(final ShardingConnection connection, final String sql) {
        this(connection, sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT, false);
    }
    
    public ShardingPreparedStatement(final ShardingConnection connection, final String sql, final int resultSetType, final int resultSetConcurrency) {
        this(connection, sql, resultSetType, resultSetConcurrency, ResultSet.HOLD_CURSORS_OVER_COMMIT, false);
    }
    
    public ShardingPreparedStatement(final ShardingConnection connection, final String sql, final int autoGeneratedKeys) {
        this(connection, sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT, Statement.RETURN_GENERATED_KEYS == autoGeneratedKeys);
    }
    
    public ShardingPreparedStatement(final ShardingConnection connection, final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) {
        this(connection, sql, resultSetType, resultSetConcurrency, resultSetHoldability, false);
    }
    
    private ShardingPreparedStatement(final ShardingConnection connection, final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability, final boolean returnGeneratedKeys) {
        this.connection = connection;
        this.sql = sql;
        ShardingContext shardingContext = connection.getShardingDataSource().getShardingContext();
        routingEngine = new PreparedStatementRoutingEngine(sql, shardingContext.getShardingRule(),
                shardingContext.getMetaData().getTable(), shardingContext.getDatabaseType(), shardingContext.isShowSQL(), shardingContext.getMetaData().getDataSource());
        preparedStatementExecutor = new PreparedStatementExecutor(resultSetType, resultSetConcurrency, resultSetHoldability, returnGeneratedKeys, connection);
        batchPreparedStatementExecutor = new BatchPreparedStatementExecutor(resultSetType, resultSetConcurrency, resultSetHoldability, returnGeneratedKeys, connection);
    }
    
    @Override
    public ResultSet executeQuery() throws SQLException {
        ResultSet result;
        try {
            clearPrevious();
            sqlRoute();
            initPreparedStatementExecutor();
            MergeEngine mergeEngine = MergeEngineFactory.newInstance(
                    connection.getShardingDataSource().getShardingContext().getShardingRule(), preparedStatementExecutor.executeQuery(), routeResult.getSqlStatement(),
                    connection.getShardingDataSource().getShardingContext().getMetaData().getTable());
            result = new ShardingResultSet(preparedStatementExecutor.getResultSets(), merge(mergeEngine), this);
        } finally {
            clearBatch();
        }
        currentResultSet = result;
        return result;
    }
  
    @Override
    public int executeUpdate() throws SQLException {
        try {
            clearPrevious();
            sqlRoute();
            initPreparedStatementExecutor();
            return preparedStatementExecutor.executeUpdate();
        } finally {
            refreshTableMetaData();
            clearBatch();
        }
    }
    
    @Override
    public boolean execute() throws SQLException {
        try {
            clearPrevious();
            sqlRoute();
            initPreparedStatementExecutor();
            return preparedStatementExecutor.execute();
        } finally {
            refreshTableMetaData();
            clearBatch();
        }
    }
    
    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        Optional<GeneratedKey> generatedKey = getGeneratedKey();
        if (preparedStatementExecutor.isReturnGeneratedKeys() && generatedKey.isPresent()) {
            return new GeneratedKeysResultSet(routeResult.getGeneratedKey().getGeneratedKeys().iterator(), generatedKey.get().getColumn().getName(), this);
        }
        if (1 == preparedStatementExecutor.getStatements().size()) {
            return preparedStatementExecutor.getStatements().iterator().next().getGeneratedKeys();
        }
        return new GeneratedKeysResultSet();
    }
    
    private Optional<GeneratedKey> getGeneratedKey() {
        if (null != routeResult && routeResult.getSqlStatement() instanceof InsertStatement) {
            return Optional.fromNullable(routeResult.getGeneratedKey());
        }
        return Optional.absent();
    }
    
    @Override
    public ResultSet getResultSet() throws SQLException {
        if (null != currentResultSet) {
            return currentResultSet;
        }
        if (1 == preparedStatementExecutor.getStatements().size() && routeResult.getSqlStatement() instanceof DQLStatement) {
            currentResultSet = preparedStatementExecutor.getStatements().iterator().next().getResultSet();
            return currentResultSet;
        }
        List<ResultSet> resultSets = new ArrayList<>(preparedStatementExecutor.getStatements().size());
        List<QueryResult> queryResults = new ArrayList<>(preparedStatementExecutor.getStatements().size());
        for (PreparedStatement each : preparedStatementExecutor.getStatements()) {
            ResultSet resultSet = each.getResultSet();
            resultSets.add(resultSet);
            queryResults.add(new StreamQueryResult(resultSet));
        }
        if (routeResult.getSqlStatement() instanceof SelectStatement || routeResult.getSqlStatement() instanceof DALStatement) {
            MergeEngine mergeEngine = MergeEngineFactory.newInstance(
                    connection.getShardingDataSource().getShardingContext().getShardingRule(), queryResults, routeResult.getSqlStatement(), 
                    connection.getShardingDataSource().getShardingContext().getMetaData().getTable());
            currentResultSet = new ShardingResultSet(resultSets, merge(mergeEngine), this);
        }
        return currentResultSet;
    }
    
    // TODO refresh table meta data by SQL parse result
    private void refreshTableMetaData() throws SQLException {
        if (null != routeResult && null != connection && SQLType.DDL == routeResult.getSqlStatement().getType() && !routeResult.getSqlStatement().getTables().isEmpty()) {
            String logicTableName = routeResult.getSqlStatement().getTables().getSingleTableName();
            TableMetaDataLoader tableMetaDataLoader = new TableMetaDataLoader(connection.getShardingDataSource().getShardingContext().getMetaData().getDataSource(),
                    connection.getShardingDataSource().getShardingContext().getExecuteEngine(), new JDBCTableMetaDataConnectionManager(connection.getShardingDataSource().getDataSourceMap()),
                    connection.getShardingDataSource().getShardingContext().getMaxConnectionsSizePerQuery());
            connection.getShardingDataSource().getShardingContext().getMetaData().getTable().put(
                    logicTableName, tableMetaDataLoader.load(logicTableName, connection.getShardingDataSource().getShardingContext().getShardingRule()));
        }
    }
    
    private void sqlRoute() {
        RoutingEvent event = new RoutingEvent(sql);
        ShardingEventBusInstance.getInstance().post(event);
        try {
            routeResult = routingEngine.route(getParameters());
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            event.setExecuteFailure(ex);
            ShardingEventBusInstance.getInstance().post(event);
            throw ex;
        }
        event.setExecuteSuccess();
        ShardingEventBusInstance.getInstance().post(event);
    }
    
    private void initPreparedStatementExecutor() throws SQLException {
        preparedStatementExecutor.init(routeResult);
        setParametersForStatements();
    }
    
    private void setParametersForStatements() {
        for (int i = 0; i < preparedStatementExecutor.getStatements().size(); i++) {
            replaySetParameter(preparedStatementExecutor.getStatements().get(i), preparedStatementExecutor.getParameterSets().get(i));
        }
    }
    
    private MergedResult merge(final MergeEngine mergeEngine) throws SQLException {
        MergeEvent event = new MergeEvent();
        try {
            ShardingEventBusInstance.getInstance().post(event);
            MergedResult result = mergeEngine.merge();
            event.setExecuteSuccess();
            ShardingEventBusInstance.getInstance().post(event);
            return result;
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            event.setExecuteFailure(ex);
            ShardingEventBusInstance.getInstance().post(event);
            throw ex;
        }
    }
    
    private void clearPrevious() throws SQLException {
        preparedStatementExecutor.clear();
    }
    
    @Override
    public void addBatch() throws SQLException {
        try {
            sqlRoute();
            batchPreparedStatementExecutor.addBatchForRouteUnits(batchCount, routeResult);
            batchCount++;
        } finally {
            currentResultSet = null;
            clearParameters();
        }
    }
    
    @Override
    public int[] executeBatch() throws SQLException {
        try {
            setBatchParametersForStatements();
            return batchPreparedStatementExecutor.executeBatch();
        } finally {
            clearBatch();
        }
    }
    
    private void setBatchParametersForStatements() {
        for (SQLExecuteUnit each : batchPreparedStatementExecutor.getExecuteUnits()) {
            for (List<Object> parameters : each.getRouteUnit().getSqlUnit().getParameterSets()) {
                replaySetParameter((PreparedStatement) each.getStatement(), parameters);
            }
        }
    }
    
    @Override
    public void clearBatch() {
        currentResultSet = null;
        clearParameters();
        batchStatementUnits.clear();
        batchCount = 0;
    }
    
    @Override
    public int getResultSetType() {
        return preparedStatementExecutor.getResultSetType();
    }
    
    @Override
    public int getResultSetConcurrency() {
        return preparedStatementExecutor.getResultSetConcurrency();
    }
    
    @Override
    public int getResultSetHoldability() {
        return preparedStatementExecutor.getResultSetHoldability();
    }
    
    @Override
    public Collection<PreparedStatement> getRoutedStatements() {
        return preparedStatementExecutor.getStatements();
    }
}



