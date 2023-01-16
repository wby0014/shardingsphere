package com.xiaoying.common.encrypt.jdbc.datasource;

import com.xiaoying.common.encrypt.jdbc.common.EncryptJdbcPropertiesConfigurationProperties;
import com.xiaoying.common.encrypt.jdbc.properties.EncryptRuleConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.shardingsphere.encrypt.api.EncryptRuleConfiguration;
import org.apache.shardingsphere.encrypt.yaml.config.YamlEncryptRuleConfiguration;
import org.apache.shardingsphere.encrypt.yaml.swapper.EncryptRuleConfigurationYamlSwapper;
import org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.EncryptDataSource;
import org.apache.shardingsphere.underlying.common.exception.ShardingSphereException;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * encrypt datasource 处理器
 *
 * @author binyu.wu
 * @date 2023/1/12 18:32
 */
@Slf4j
public class EncryptDataSourcePostProcessor implements BeanPostProcessor {

    private final EncryptRuleConfigurationProperties encryptRule;
    private final EncryptJdbcPropertiesConfigurationProperties props;
    private List<String> includes;

    public EncryptDataSourcePostProcessor(EncryptRuleConfigurationProperties encryptRule, EncryptJdbcPropertiesConfigurationProperties props, List<String> includes) {
        this.encryptRule = encryptRule;
        this.props = props;
        this.includes = includes;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource) {
            if (includes.contains(beanName)) {
                log.info("DataSource {} is brokered", beanName);
                YamlEncryptRuleConfiguration ruleConfiguration = createEncryptRuleConfiguration(beanName);
                ProxyFactory proxyFactory = new ProxyFactory();
                proxyFactory.setTarget(bean);
                EncryptJdbcDataSourceProxyAdvice encryptJdbcDataSourceProxyAdvice = new EncryptJdbcDataSourceProxyAdvice(new EncryptRuleConfigurationYamlSwapper().swap(ruleConfiguration), props.getProps().get(beanName), beanName);
                proxyFactory.addAdvice(encryptJdbcDataSourceProxyAdvice);
                return proxyFactory.getProxy();
            }

            if (bean instanceof EncryptDataSource) {
                log.info("Unwrap the bean of the data source," +
                        " and return the original data source to replace the data source proxy.");
                return ((EncryptDataSource) bean).getDataSource();
            }
        }
        return bean;
    }


    private YamlEncryptRuleConfiguration createEncryptRuleConfiguration(String beanName) {
        if (StringUtils.isEmpty(beanName) || null == encryptRule) {
            log.error("[encryptjdbc.rules.datasource." + beanName + ".tables] configuration is not exists!");
            throw new ShardingSphereException("[encryptjdbc.rules.datasource." + beanName + ".tables] configuration is not exists!");
        }
        try {
            YamlEncryptRuleConfiguration ruleConfiguration;
            Map<String, YamlEncryptRuleConfiguration> encryptRuleDatasourceMap = encryptRule.getDatasource();
            if (null == encryptRuleDatasourceMap || encryptRuleDatasourceMap.size() == 0) {
                log.error("[encryptjdbc.rules.datasource." + beanName + ".tables] configuration is not exists!");
                throw new ShardingSphereException("[encryptjdbc.rules.datasource." + beanName + ".tables] configuration is not exists!");
            } else {
                ruleConfiguration = encryptRuleDatasourceMap.get(beanName);
            }
            if (null == ruleConfiguration) {
                log.error("[encryptjdbc.rules.datasource." + beanName + ".tables] configuration is not exists,please check");
                throw new ShardingSphereException("[encryptjdbc.rules.datasource." + beanName + ".tables] configuration is not exists,please check");
            }
            return ruleConfiguration;
        } catch (Exception e) {
            log.error("create encrypt jdbc rule configuration error!", e);
            throw new ShardingSphereException("create encrypt jdbc rule configuration error!");
        }
    }


    class EncryptJdbcDataSourceProxyAdvice implements MethodInterceptor {

        private EncryptRuleConfiguration encryptRuleConfiguration;
        private Properties props;
        private String originDataSourceName;

        public EncryptJdbcDataSourceProxyAdvice(EncryptRuleConfiguration encryptRuleConfiguration, Properties props, String originDataSourceName) {
            this.encryptRuleConfiguration = encryptRuleConfiguration;
            this.props = props;
            this.originDataSourceName = originDataSourceName;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            EncryptDataSource encryptDataSource = EncryptDataSourceProxyHolder.get().putDataSource((DataSource) invocation.getThis(), originDataSourceName, encryptRuleConfiguration, props);
            Method method = invocation.getMethod();
            Object[] arguments = invocation.getArguments();
            Method m = BeanUtils.findDeclaredMethod(EncryptDataSource.class, method.getName(), method.getParameterTypes());
            if (m != null) {
                return m.invoke(encryptDataSource, arguments);
            } else {
                return invocation.proceed();
            }
        }
    }

}
