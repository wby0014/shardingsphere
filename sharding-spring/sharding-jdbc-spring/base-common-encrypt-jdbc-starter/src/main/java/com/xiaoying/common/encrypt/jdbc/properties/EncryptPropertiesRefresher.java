package com.xiaoying.common.encrypt.jdbc.properties;

import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.xiaoying.common.encrypt.jdbc.common.EncryptJdbcPropertiesConfigurationProperties;
import com.xiaoying.common.encrypt.jdbc.datasource.EncryptDataSourceProxyHolder;
import com.xiaoying.common.encrypt.jdbc.utils.EncryptApplicationContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.EncryptDataSource;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * 加密规则属性自动刷新
 *
 * @author binyu.wu
 * @date 2022/10/26 17:59
 */
@Slf4j
@Component
public class EncryptPropertiesRefresher implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * apollo配置监听器，默认监听的是application命名空间
     *
     * @param changeEvent
     */
    @ApolloConfigChangeListener(interestedKeyPrefixes = {"encryptjdbc.props."})
    public void onChange(ConfigChangeEvent changeEvent) {
        log.info("encryptjdbc receive apollo config change");
        boolean rulePropertiesChanged = false;
        for (String changedKey : changeEvent.changedKeys()) {
            if (changedKey.contains("query.with.cipher.column")) {
                rulePropertiesChanged = true;
                break;
            }
        }
        // 更新规则配置
        if (rulePropertiesChanged) {
            refreshEncryptRuleProperties(changeEvent);
        }
    }

    /**
     * 更新SpringApplicationContext对象，并更新路由
     *
     * @param changeEvent
     */
    private void refreshEncryptRuleProperties(ConfigChangeEvent changeEvent) {
        log.info("refreshing encryptjdbc props properties!");
        // 更新配置
        EncryptJdbcPropertiesConfigurationProperties bean = EncryptApplicationContext.getBean(EncryptJdbcPropertiesConfigurationProperties.class);
        for (Map.Entry<String, Properties> entry : bean.getProps().entrySet()) {
            Collection<Object> allKey = entry.getValue().keySet();
            for (Object key : allKey) {
                if (changeEvent.isChanged("encryptjdbc.props." + entry.getKey() + "." + key)) {
                    ConfigChange configChange = changeEvent.getChange("encryptjdbc.props." + entry.getKey() + "." + key);
                    String newValue = configChange.getNewValue();
                    log.info("encryptjdbc datasource {} props properties {} is change,newValue:{}", entry.getKey(), key, newValue);
                    entry.getValue().setProperty(String.valueOf(key), newValue);
                }
            }
            Map<DataSource, EncryptDataSource> dataSourceProxyMap = EncryptDataSourceProxyHolder.get().getDataSourceProxyMap();
            for (Map.Entry<DataSource, EncryptDataSource> entryMap : dataSourceProxyMap.entrySet()) {
                Map<String, DataSource> dataSourceMap = entryMap.getValue().getDataSourceMap();
                if (dataSourceMap.containsKey(entry.getKey())) {
                    // 说明是当前数据源
                    log.info("encryptjdbc datasource {} refresh properties {}", entry.getKey(), entry.getValue());
                    entryMap.getValue().refreshRuntimeContext(entry.getValue());
                }
            }
        }
        log.info("encryptjdbc props properties refreshed!");
    }

}
