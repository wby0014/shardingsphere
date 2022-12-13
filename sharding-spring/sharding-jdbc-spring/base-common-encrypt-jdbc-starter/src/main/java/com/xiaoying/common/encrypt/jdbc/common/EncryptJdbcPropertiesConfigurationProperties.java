package com.xiaoying.common.encrypt.jdbc.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * encryptjdbc 属性配置
 *
 * @author binyu.wu
 * @date 2022/11/1 14:13
 */
@ConfigurationProperties(prefix = "encryptjdbc")
@Getter
@Setter
public class EncryptJdbcPropertiesConfigurationProperties {

    private Map<String, Properties> props = new LinkedHashMap<>();
}
