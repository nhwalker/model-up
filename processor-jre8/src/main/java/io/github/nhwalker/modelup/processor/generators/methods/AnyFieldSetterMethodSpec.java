package io.github.nhwalker.modelup.processor.generators.methods;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeVariableName;

import io.github.nhwalker.modelup.ModelKey;
import io.github.nhwalker.modelup.processor.descriptors.KeyDescriptor;
import io.github.nhwalker.modelup.processor.descriptors.ModelDescriptor;

public class AnyFieldSetterMethodSpec {

  private static MethodSpec.Builder base(ModelDescriptor definition) {

    TypeVariableName typeVariableName = TypeVariableName.get(findTypeVariableName(definition));

    ParameterizedTypeName fieldKeyWildcardType = //
        ParameterizedTypeName.get(ClassName.get(ModelKey.class), typeVariableName);

    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("switch(key.name())");
    for (KeyDescriptor key : definition.getKeys().values()) {
      if (key.dontAllowSet()) {
        // skip
      } else {
        if (key.isPrimitive()) {
          body.add("case $S: this.$L(($T)value); break; \n", key.name(), key.name(), key.boxedType());
        } else {
          body.add("case $S: this.$L(($T)value); break; \n", key.name(), key.name(), key.type());
        }
      }
    }
    body.add("default: throw new $T($S + key.name());\n", IllegalArgumentException.class, "No such field can be set: ");
    body.endControlFlow();

    MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("set")//
        .addModifiers(Modifier.PUBLIC)//
        .addParameter(ParameterSpec.builder(fieldKeyWildcardType, "key").build())//
        .addParameter(ParameterSpec.builder(typeVariableName, "value").build())//
        .addCode(body.build())//
        .addTypeVariable(typeVariableName);
    if (needsUnsafeCastAnnotation(definition)) {
      methodSpec.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)//
          .addMember("value", "$S", "unchecked").build());
    }
    return methodSpec;
  }

  public static MethodSpec.Builder createForInterface(ModelDescriptor definition) {
    MethodSpec.Builder builder = base(definition);
    builder.addModifiers(Modifier.DEFAULT);
    return builder;
  }

  private static String findTypeVariableName(ModelDescriptor definition) {
    String target = "T";
    boolean hasMatch = anyMatchingParams(definition, target);
    while (hasMatch) {
      target += "T";
      hasMatch = anyMatchingParams(definition, target);
    }
    return target;
  }

  private static boolean anyMatchingParams(ModelDescriptor definition, String target) {
    return definition.getTypeParameters().stream()//
        .anyMatch(x -> x.name.equals(target));
  }

  private static boolean needsUnsafeCastAnnotation(ModelDescriptor definition) {
    return definition.getKeys().values().stream().anyMatch(KeyDescriptor::needsSupressUncheckedForCast);
  }

}
