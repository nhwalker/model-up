package io.github.nhwalker.modelup;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(CLASS)
@Target(TYPE)
public @interface ModelUp {

  boolean generateKeys() default true;

  String keysTypeName() default "";

  String keysPackageName() default "";

  boolean generateArgs() default true;

  String argsTypeName() default "";

  String argsPackageName() default "";

  boolean generateRecord() default true;

  String recordTypeName() default "";

  String recordPackageName() default "";

  boolean memorizeHash() default false;
  
  boolean memorizeToString() default false;
  
  Class<?> argsExtension() default void.class; 
}
