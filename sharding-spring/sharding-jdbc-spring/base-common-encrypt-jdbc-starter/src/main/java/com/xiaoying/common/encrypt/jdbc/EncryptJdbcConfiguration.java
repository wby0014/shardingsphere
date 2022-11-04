package com.xiaoying.common.encrypt.jdbc;

import com.xiaoying.common.encrypt.jdbc.common.EncryptJdbcPropertiesConfigurationProperties;
import com.xiaoying.common.encrypt.jdbc.condition.EncryptRuleCondition;
import com.xiaoying.common.encrypt.jdbc.properties.EncryptProxyDatasourceProperties;
import com.xiaoying.common.encrypt.jdbc.properties.EncryptRuleConfigurationProperties;
import com.xiaoying.common.encrypt.jdbc.multidatasource.EncryptDataSourceConfiguration;
import com.xiaoying.common.encrypt.jdbc.utils.EncryptApplicationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.transaction.spring.ShardingTransactionTypeScanner;
import org.apache.shardingsphere.underlying.common.config.inline.InlineExpressionParser;
import org.apache.shardingsphere.underlying.common.exception.ShardingSphereException;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private final Map<String, DataSource> dataSourceMap = new LinkedHashMap<>();

    @Resource
    private Map<String, DataSource> defaultDataSourceMap;


    /**
     * 初始encrypt数据源
     *
     * @return
     */
    @Bean
    @Conditional(EncryptRuleCondition.class)
    public EncryptDataSourceConfiguration encryptDataSourceConfiguration() throws SQLException {
        return new EncryptDataSourceConfiguration(encryptRule, props, dataSourceMap);
    }

    @Bean
    public ShardingTransactionTypeScanner shardingTransactionTypeScanner() {
        return new ShardingTransactionTypeScanner();
    }

    /**
     * 初始化环境属性和数据源
     *
     * @param environment
     */
    @Override
    public final void setEnvironment(final Environment environment) {
        String proxyPrefix = "encryptjdbc.datasource.proxy.";
        for (String each : getDataSourceNames(environment, proxyPrefix)) {
            try {
                dataSourceMap.put(each, getDataSource(each));
            } catch (final ReflectiveOperationException ex) {
                throw new ShardingSphereException("Can't find datasource type!", ex);
            } catch (final NamingException namingEx) {
                throw new ShardingSphereException("Can't find JNDI datasource!", namingEx);
            }
        }
    }

    private List<String> getDataSourceNames(final Environment environment, final String prefix) {
        StandardEnvironment standardEnv = (StandardEnvironment) environment;
        standardEnv.setIgnoreUnresolvableNestedPlaceholders(true);
        return null == standardEnv.getProperty(prefix + "name")
                ? new InlineExpressionParser(standardEnv.getProperty(prefix + "names")).splitAndEvaluate() : Collections.singletonList(standardEnv.getProperty(prefix + "name"));
    }

    @SuppressWarnings("unchecked")
    private DataSource getDataSource(final String dataSourceName) throws ReflectiveOperationException, NamingException {
        return getNotAbsentDataSource(dataSourceName);
    }

    /**
     * 获取数据源
     *
     * @param dataSourceName
     * @return
     */
    private DataSource getNotAbsentDataSource(String dataSourceName) {
        if (StringUtils.isEmpty(dataSourceName)) {
            throw new ShardingSphereException("datasource name is empty!");
        }
        DataSource dataSource;
        try {
            dataSource = defaultDataSourceMap.get(dataSourceName);
            if (null == dataSource) {
                dataSource = EncryptApplicationContext.getBean(dataSourceName, DataSource.class);
                if (null == dataSource) {
                    throw new ShardingSphereException("Can't find datasource " + dataSourceName);
                }
            }
        } catch (Exception e) {
            throw new ShardingSphereException("Can't find datasource " + dataSourceName);
        }
        return dataSource;
    }
}
