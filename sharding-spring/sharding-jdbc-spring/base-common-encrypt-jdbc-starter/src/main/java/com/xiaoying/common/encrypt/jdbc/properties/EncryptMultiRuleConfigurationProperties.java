package com.xiaoying.common.encrypt.jdbc.properties;

import lombok.Getter;
import lombok.Setter;
import org.apache.shardingsphere.encrypt.yaml.config.YamlEncryptRuleConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author binyu.wu
 * @date 2022/11/3 19:26
 */
@Getter
@Setter
public class EncryptMultiRuleConfigurationProperties extends YamlEncryptRuleConfiguration {

    /**
     * 多数据源规则隔离配置
     */
    private Map<String, YamlEncryptRuleConfiguration> datasource = new LinkedHashMap<>();
}
