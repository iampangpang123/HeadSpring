package tang.mvcframework.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * @Retention(RetentionPolicy.RUNTIME)
        这种类型的Annotations将被JVM保留,所以他们能在运行时被JVM或其他使用反射机制的代码所读取和使用.
 @Target({ ElementType.TYPE, ElementType.METHOD })
   CONSTRUCTOR:用于描述构造器
   2.FIELD:用于描述域
   3.LOCAL_VARIABLE:用于描述局部变量
   4.METHOD:用于描述方法
   5.PACKAGE:用于描述包
   6.PARAMETER:用于描述参数
   7.TYPE:用于描述类、接口(包括注解类型) 或enum声明
 @Documented
 注解表明这个注解应该被 javadoc工具记录. 默认情况下,javadoc是不包括注解的. 但
 如果声明注解时指定了 @Documented,则它会被 javadoc 之类的工具处理,
 所以注解类型信息也会被包括在生成的文档中，是一个标记注解，没有成员
 *
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TangRequestMapping {
	String value() default "";
}
