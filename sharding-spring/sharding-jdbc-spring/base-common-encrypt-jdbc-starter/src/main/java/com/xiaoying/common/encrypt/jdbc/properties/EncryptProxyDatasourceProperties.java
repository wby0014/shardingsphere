package com.xiaoying.common.encrypt.jdbc.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 被代理数据源配置
 * @author binyu.wu
 * @date 2022/11/4 13:47
 */
@ConfigurationProperties(prefix = "encryptjdbc.datasource.proxy")
@Getter
@Setter
public class EncryptProxyDatasourceProperties {
    /**
     * 需要被代理的数据源bean名称
     */
    private String names;
}


