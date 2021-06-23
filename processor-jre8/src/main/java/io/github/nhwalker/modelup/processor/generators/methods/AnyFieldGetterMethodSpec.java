package io.github.nhwalker.modelup.processor.generators.methods;

import javax.lang.model.element.Modifier;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeVariableName;

import io.github.nhwalker.modelup.ModelKey;
import io.github.nhwalker.modelup.processor.descriptors.KeyDescriptor;
import io.github.nhwalker.modelup.processor.descriptors.ModelDescriptor;

public class AnyFieldGetterMethodSpec {

  private static MethodSpec.Builder base(ModelDescriptor definition, boolean fieldAccess) {
    TypeVariableName typeVariableName = TypeVariableName.get(findTypeVariableName(definition));
    ParameterizedTypeName fieldKeyWildcardType = //
        ParameterizedTypeName.get(ClassName.get(ModelKey.class), typeVariableName);

    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("switch(key.name())");
    for (KeyDescriptor key : definition.getKeys().values()) {
      body.add("case $S: return key.type().cast(this.$L$L);\n", key.name(), key.name(), fieldAccess ? "" : "()");
    }
    body.add("default: throw new $T($S + key.name());\n", IllegalArgumentException.class, "No such field: ");
    body.endControlFlow();

    return MethodSpec.methodBuilder("get")//
        .addModifiers(Modifier.PUBLIC)//
        .returns(typeVariableName)//
        .addParameter(ParameterSpec.builder(fieldKeyWildcardType, "key").build())//
        .addCode(body.build())//
        .addTypeVariable(typeVariableName);
  }

  public static MethodSpec.Builder createForInterface(ModelDescriptor definition) {
    MethodSpec.Builder builder = base(definition, false);
    builder.addModifiers(Modifier.DEFAULT);
    return builder;
  }

  public static MethodSpec.Builder createForClass(ModelDescriptor definition) {
    MethodSpec.Builder builder = base(definition, true);
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

}
