package io.github.nhwalker.modelup.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

public class TypeNameUtils {

  public static ClassName rawType(TypeName name) {
    if (name instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) name).rawType;
    } else if (name instanceof ClassName) {
      return (ClassName) name;
    } else {
      throw new IllegalArgumentException("Not known how to convert type " + name.getClass());
    }

  }

  public static String packageName(TypeName name) {
    ClassName type = rawType(name);
    return type.packageName();
  }
}
