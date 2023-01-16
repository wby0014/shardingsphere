package com.xiaoying.common.encrypt.jdbc.datasource;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.encrypt.api.EncryptRuleConfiguration;
import org.apache.shardingsphere.shardingjdbc.api.EncryptDataSourceFactory;
import org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.EncryptDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * encrypt datasource 代理
 *
 * @author binyu.wu
 * @date 2023/1/16 11:42
 */
@Slf4j
public class EncryptDataSourceProxyHolder {

    private Map<DataSource, EncryptDataSource> dataSourceProxyMap;

    private EncryptDataSourceProxyHolder() {
        dataSourceProxyMap = new HashMap<>(16);
    }

    private static class Holder {
        private static final EncryptDataSourceProxyHolder INSTANCE;

        static {
            INSTANCE = new EncryptDataSourceProxyHolder();
        }
    }

    public Map<DataSource, EncryptDataSource> getDataSourceProxyMap() {
        return dataSourceProxyMap;
    }

    public static EncryptDataSourceProxyHolder get() {
        return Holder.INSTANCE;
    }

    public EncryptDataSource putDataSource(DataSource dataSource, String originDataSourceName, EncryptRuleConfiguration encryptRuleConfiguration, Properties props) {
        DataSource originalDataSource;
        if (dataSource instanceof EncryptDataSource) {
            EncryptDataSource dataSourceProxy = (EncryptDataSource) dataSource;
            originalDataSource = dataSourceProxy.getDataSource();
        } else {
            originalDataSource = dataSource;
        }

        EncryptDataSource dsProxy = dataSourceProxyMap.get(originalDataSource);
        if (dsProxy == null) {
            synchronized (dataSourceProxyMap) {
                dsProxy = dataSourceProxyMap.get(originalDataSource);
                if (dsProxy == null) {
                    try {
                        dsProxy = (EncryptDataSource) EncryptDataSourceFactory.createDataSource(originalDataSource, originDataSourceName, encryptRuleConfiguration, props);
                        dataSourceProxyMap.put(originalDataSource, dsProxy);
                    } catch (Exception e) {
                        log.error("create encrypt datasource error", e);
                    }
                }
            }
        }
        return dsProxy;
    }
}
