package com.healerjean.proj.utils.validate.validator;


import com.healerjean.proj.utils.validate.anno.NameInclude;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 校验数据
 */
public class NameIncludeValidator implements ConstraintValidator<NameInclude, String> {

    private String type;

    /**
     * 获取注解中的值
     *
     * @param constraintAnnotation
     */
    @Override
    public void initialize(NameInclude constraintAnnotation) {
        type = constraintAnnotation.type();
    }

    /**
     * @param value   字段数据
     * @param context
     * @return
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (StringUtils.isBlank(value)) {
            return true;
        }
        if ("Mail".equals(type) && "HealerJean".equals(value)) {
            return true;
        } else {
            return false;
        }
    }
}
