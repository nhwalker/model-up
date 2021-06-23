package io.github.nhwalker.modelup.processor.generators.methods;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.MethodSpec;

import io.github.nhwalker.modelup.processor.descriptors.KeyDescriptor;

public class FieldGetterMethodSpec {

  private static MethodSpec.Builder signiture(KeyDescriptor key) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder(key.name());
    builder.returns(key.type());
    builder.addModifiers(Modifier.PUBLIC);
    if (!key.overrides().isEmpty()) {
      builder.addAnnotation(Override.class);
    }
    return builder;
  }

  public static MethodSpec.Builder createForInterface(KeyDescriptor key) {
    MethodSpec.Builder builder = signiture(key);
    builder.addModifiers(Modifier.ABSTRACT);
    return builder;
  }

  public static MethodSpec.Builder createForClass(KeyDescriptor key) {
    MethodSpec.Builder builder = signiture(key);
    builder.addStatement("return $L", key.name());
    return builder;
  }
}
