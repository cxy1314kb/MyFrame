package jsu.lzy.persistence;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
/**
 * 字段属性映射
 * @author 大爷来了
 *
 */
public @interface coloumName {
String name();	//列明
boolean isId() default false; //是否为ID
}
