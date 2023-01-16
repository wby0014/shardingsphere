package com.xiaoying.common.encrypt.jdbc;

import com.xiaoying.common.encrypt.jdbc.common.EncryptJdbcPropertiesConfigurationProperties;
import com.xiaoying.common.encrypt.jdbc.datasource.EncryptDataSourcePostProcessor;
import com.xiaoying.common.encrypt.jdbc.properties.EncryptPropertiesRefresher;
import com.xiaoying.common.encrypt.jdbc.properties.EncryptProxyDatasourceProperties;
import com.xiaoying.common.encrypt.jdbc.properties.EncryptRuleConfigurationProperties;
import com.xiaoying.common.encrypt.jdbc.utils.EncryptApplicationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.transaction.spring.ShardingTransactionTypeScanner;
import org.apache.shardingsphere.underlying.common.config.inline.InlineExpressionParser;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * EncryptJdbc 自动配置
 *
 * @author binyu.wu
 * @date 2022/11/1 14:39
 */
@Slf4j
@Configuration
@ComponentScan("org.apache.shardingsphere.spring.boot.converter")
@EnableConfigurationProperties({
        EncryptProxyDatasourceProperties.class,
        EncryptRuleConfigurationProperties.class,
        EncryptJdbcPropertiesConfigurationProperties.class})
@ConditionalOnProperty(prefix = "encryptjdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@RequiredArgsConstructor
@Import({EncryptApplicationContext.class})
public class EncryptJdbcConfiguration implements EnvironmentAware {

    private final EncryptRuleConfigurationProperties encryptRule;

    private final EncryptJdbcPropertiesConfigurationProperties props;

    private final List<String> proxyDataSources = new ArrayList<>();

    @Bean
    public EncryptPropertiesRefresher encryptPropertiesRefresher() {
        return new EncryptPropertiesRefresher();
    }

    @Bean
    public ShardingTransactionTypeScanner shardingTransactionTypeScanner() {
        return new ShardingTransactionTypeScanner();
    }

    @ConditionalOnMissingBean(EncryptDataSourcePostProcessor.class)
    @Bean
    public EncryptDataSourcePostProcessor encryptDataSourcePostProcessor() {
        return new EncryptDataSourcePostProcessor(encryptRule, props, proxyDataSources);
    }

    /**
     * 初始化环境属性和数据源
     *
     * @param environment
     */
    @Override
    public final void setEnvironment(final Environment environment) {
        String proxyPrefix = "encryptjdbc.datasource.proxy.";
        proxyDataSources.addAll(getDataSourceNames(environment, proxyPrefix));
    }


    private List<String> getDataSourceNames(final Environment environment, final String prefix) {
        StandardEnvironment standardEnv = (StandardEnvironment) environment;
        standardEnv.setIgnoreUnresolvableNestedPlaceholders(true);
        return null == standardEnv.getProperty(prefix + "name")
                ? new InlineExpressionParser(standardEnv.getProperty(prefix + "names")).splitAndEvaluate() : Collections.singletonList(standardEnv.getProperty(prefix + "name"));
    }

}
