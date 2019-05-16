package tang.mvcframework.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TangRequestParam {
	/*
	 * 这个方法的作用是：平时使用注解的时候需要这样写：name=xxx,age=xx
	 */
	String value() default "";//有了这个方法，对于名字为value的，可以直接写值
}
