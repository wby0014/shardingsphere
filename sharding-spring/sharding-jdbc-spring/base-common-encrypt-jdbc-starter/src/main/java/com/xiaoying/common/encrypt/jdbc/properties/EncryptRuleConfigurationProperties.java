package com.xiaoying.common.encrypt.jdbc.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * EncryptJdbc 规则配置
 *
 * @author binyu.wu
 * @date 2022/11/1 14:16
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "encryptjdbc.rules")
public class EncryptRuleConfigurationProperties extends EncryptMultiRuleConfigurationProperties {

}
