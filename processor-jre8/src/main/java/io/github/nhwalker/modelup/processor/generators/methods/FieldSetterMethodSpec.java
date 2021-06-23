package io.github.nhwalker.modelup.processor.generators.methods;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.MethodSpec;

import io.github.nhwalker.modelup.processor.descriptors.KeyDescriptor;

public class FieldSetterMethodSpec {

  private static MethodSpec.Builder signiture(KeyDescriptor key) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder(key.name());
    builder.addParameter(key.type(), key.name());
    builder.addModifiers(Modifier.PUBLIC);

    // TODO will not work?
//    if (key.overridesSet()) {
//      builder.addAnnotation(Override.class);
//    }

    return builder;
  }

  public static MethodSpec.Builder createForInterface(KeyDescriptor key) {
    MethodSpec.Builder builder = signiture(key);
    builder.addModifiers(Modifier.ABSTRACT);
    return builder;
  }

  public static MethodSpec.Builder createForClass(KeyDescriptor key) {
    MethodSpec.Builder builder = signiture(key);
    builder.addStatement("this.$L = $L", key.name(), key.name());
    return builder;
  }
}
