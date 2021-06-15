package io.github.nhwalker.modelup;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.METHOD;
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

  String argsExtension() default "";
  
  boolean defaultConstructor() default true;

  @Documented
  @Retention(CLASS)
  @Target(METHOD)
  @interface InitialArgs {
    boolean inherit() default true;
  }

  @Documented
  @Retention(CLASS)
  @Target(METHOD)
  @interface Sanatize {
    boolean inherit() default true;
  }

  @Documented
  @Retention(CLASS)
  @Target(METHOD)
  @interface Validate {
    boolean inherit() default true;
  }
}
