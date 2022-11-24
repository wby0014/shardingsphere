package com.xiaoying.common.encrypt.jdbc.properties;

import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.xiaoying.common.encrypt.jdbc.EncryptJdbcConfiguration;
import com.xiaoying.common.encrypt.jdbc.common.EncryptJdbcPropertiesConfigurationProperties;
import com.xiaoying.common.encrypt.jdbc.utils.EncryptApplicationContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.EncryptDataSource;
import org.springframework.beans.BeansException;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 加密规则属性刷新
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
            if ("encryptjdbc.props.query.with.cipher.column".equals(changedKey)) {
                rulePropertiesChanged = true;
                break;
            }
        }
        // 更新规则配置
        if (rulePropertiesChanged) {
            log.info("encryptjdbc apollo config change refresh");
            refreshEncryptRuleProperties(changeEvent);
        }
    }

    /**
     * 更新SpringApplicationContext对象，并更新路由
     *
     * @param changeEvent
     */
    private void refreshEncryptRuleProperties(ConfigChangeEvent changeEvent) {
        log.info("Refreshing encryptjdbc props properties!");
        //更新配置
        ConfigChange configChange = changeEvent.getChange("encryptjdbc.props.query.with.cipher.column");
        String newValue = configChange.getNewValue();
        String propertyName = configChange.getPropertyName().substring(configChange.getPropertyName().indexOf("query."));
        List<EncryptDataSource> beansOfType = EncryptApplicationContext.getBeansOfType(EncryptDataSource.class);
        if (!CollectionUtils.isEmpty(beansOfType)) {
            EncryptJdbcPropertiesConfigurationProperties bean = EncryptApplicationContext.getBean(EncryptJdbcPropertiesConfigurationProperties.class);
            bean.getProps().setProperty(propertyName,newValue);
            for (EncryptDataSource encryptDataSource : beansOfType) {
                encryptDataSource.refreshRuntimeContext(bean.getProps());
            }
        }
        log.info("Encryptjdbc props properties refreshed!");
    }

}
