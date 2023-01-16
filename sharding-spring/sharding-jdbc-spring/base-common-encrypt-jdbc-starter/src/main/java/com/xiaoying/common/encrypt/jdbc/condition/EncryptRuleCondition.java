package com.xiaoying.common.encrypt.jdbc.condition;

import org.apache.shardingsphere.spring.boot.util.PropertyUtil;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
* encrypt jdbc 配置规则生效条件
* @author binyu.wu
* @date 2022/11/1 14:14
*/
public final class EncryptRuleCondition extends SpringBootCondition {

    private static final String ENCRYPT_JDBC_RULES_PREFIX = "encryptjdbc.rules";

    @Override
    public ConditionOutcome getMatchOutcome(final ConditionContext conditionContext, final AnnotatedTypeMetadata annotatedTypeMetadata) {
        boolean isEncrypt = PropertyUtil.containPropertyPrefix(conditionContext.getEnvironment(), ENCRYPT_JDBC_RULES_PREFIX);
        return isEncrypt ? ConditionOutcome.match() : ConditionOutcome.noMatch("Can't find encrypt jdbc rule configuration in environment.");
    }
}
