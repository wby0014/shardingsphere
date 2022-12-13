package com.xiaoying.common.encrypt.jdbc.multidatasource;

import com.xiaoying.common.encrypt.jdbc.common.EncryptJdbcPropertiesConfigurationProperties;
import com.xiaoying.common.encrypt.jdbc.constant.EncryptJdbcConstant;
import com.xiaoying.common.encrypt.jdbc.properties.EncryptRuleConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.encrypt.yaml.config.YamlEncryptRuleConfiguration;
import org.apache.shardingsphere.encrypt.yaml.swapper.EncryptRuleConfigurationYamlSwapper;
import org.apache.shardingsphere.shardingjdbc.api.EncryptDataSourceFactory;
import org.apache.shardingsphere.underlying.common.exception.ShardingSphereException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

/**
 * EncryptJdbc datasource配置
 *
 * @author binyu.wu
 * @date 2022/11/1 16:10
 */
@Slf4j
public class EncryptDataSourceConfiguration implements ApplicationContextAware, InitializingBean {

    private GenericApplicationContext applicationContext;

    private EncryptRuleConfigurationProperties encryptRule;

    private EncryptJdbcPropertiesConfigurationProperties props;

    private Map<String, DataSource> dataSourceMap;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (GenericApplicationContext) applicationContext;
    }

    public EncryptDataSourceConfiguration(EncryptRuleConfigurationProperties encryptRule, EncryptJdbcPropertiesConfigurationProperties props, Map<String, DataSource> dataSourceMap) {
        this.encryptRule = encryptRule;
        this.props = props;
        this.dataSourceMap = dataSourceMap;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Encrypt jdbc datasource configuration start");
        registerDataSource();
        log.info("Encrypt jdbc datasource configuration success!");
    }

    private void registerDataSource() {
        if (null == dataSourceMap) {
            log.error("[encryptjdbc.datasource.proxy.names] must not be null");
            return;
        }
        for (String key : dataSourceMap.keySet()) {
            applicationContext.registerBean(EncryptJdbcConstant.DEFAULT_PROXY_DATASOURCE_NAME.equals(key) ? EncryptJdbcConstant.DEFAULT_ENCRYPT_DATASOURCE : "encryptDataSource_" + key, DataSource.class, () -> createDataSource(key, dataSourceMap.size()));
        }
    }

    private DataSource createDataSource(String key, Integer datasourceNum) {
        try {
            YamlEncryptRuleConfiguration ruleConfiguration;
            if (1 == datasourceNum) {
                Map<String, YamlEncryptRuleConfiguration> encryptRuleDatasourceMap = encryptRule.getDatasource();
                if (null == encryptRuleDatasourceMap || encryptRuleDatasourceMap.size() == 0) {
                    ruleConfiguration = encryptRule;
                } else {
                    ruleConfiguration = encryptRuleDatasourceMap.get(key);
                }
            } else {
                Map<String, YamlEncryptRuleConfiguration> encryptRuleDatasourceMap = encryptRule.getDatasource();
                if (null == encryptRuleDatasourceMap || encryptRuleDatasourceMap.size() == 0) {
                    log.error("[encryptjdbc.rules.datasource." + key + ".tables] configuration is not exists!");
                    throw new ShardingSphereException("[encryptjdbc.rules.datasource." + key + ".tables] configuration is not exists!");
                } else {
                    ruleConfiguration = encryptRuleDatasourceMap.get(key);
                }
            }
            if (null == ruleConfiguration) {
                log.error("[encryptjdbc.rules.datasource." + key + ".tables] configuration is not exists,please check");
                throw new ShardingSphereException("[encryptjdbc.rules.datasource." + key + ".tables] configuration is not exists,please check");
            }
            return EncryptDataSourceFactory.createDataSource(dataSourceMap.get(key), new EncryptRuleConfigurationYamlSwapper().swap(ruleConfiguration), props.getProps().get(key));
        } catch (SQLException e) {
            log.error("Create encrypt jdbc datasource " + key + " error!", e);
            throw new ShardingSphereException("Create encrypt jdbc datasource " + key + " error!");
        }
    }

}
