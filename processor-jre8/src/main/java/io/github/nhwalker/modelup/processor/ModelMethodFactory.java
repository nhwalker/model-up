package io.github.nhwalker.modelup.processor;

import java.util.Set;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;

import io.github.nhwalker.modelup.ModelKey;

public class ModelMethodFactory {

  public static MethodSpec.Builder fieldKeysMethodSpec(ModelUpTypeDefinition definition) {
    ParameterizedTypeName fieldKeyWildcardType = //
        ParameterizedTypeName.get(ClassName.get(ModelKey.class), WildcardTypeName.subtypeOf(Object.class));
    ParameterizedTypeName fieldSetType = ParameterizedTypeName.get(ClassName.get(Set.class), fieldKeyWildcardType);

    return MethodSpec.methodBuilder("fieldKeys")//
        .addModifiers(Modifier.PUBLIC)//
        .returns(fieldSetType)//
        .addStatement("return $T.fields()", definition.keysType());
  }

  public static MethodSpec.Builder getMethodSpec(ModelUpTypeDefinition definition, boolean fieldAccess) {
    TypeVariableName typeVariableName = TypeVariableName.get(findTypeVariableName(definition));
    ParameterizedTypeName fieldKeyWildcardType = //
        ParameterizedTypeName.get(ClassName.get(ModelKey.class), typeVariableName);

    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("switch(key.name())");
    for (ModelKeyDefinition key : definition.keys()) {
      body.add("case $S: return key.type().cast(this.$L$L);\n", key.getName(), key.getName(), fieldAccess ? "" : "()");
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

  public static MethodSpec.Builder hasFieldMethodSpec(ModelUpTypeDefinition definition) {
    ParameterizedTypeName fieldKeyWildcardType = //
        ParameterizedTypeName.get(ClassName.get(ModelKey.class), WildcardTypeName.subtypeOf(Object.class));

    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("switch(key.name())");
    for (ModelKeyDefinition key : definition.keys()) {
      body.add("case $S:\n", key.getName());
    }
    body.indent().add("return true;").unindent().add("\n");
    body.add("default: return false;\n");
    body.endControlFlow();

    return MethodSpec.methodBuilder("hasField")//
        .addModifiers(Modifier.PUBLIC)//
        .returns(boolean.class)//
        .addParameter(ParameterSpec.builder(fieldKeyWildcardType, "key").build())//
        .addCode(body.build());
  }

  public static MethodSpec.Builder setMethodSpec(ModelUpTypeDefinition definition) {

    TypeVariableName typeVariableName = TypeVariableName.get(findTypeVariableName(definition));

    ParameterizedTypeName fieldKeyWildcardType = //
        ParameterizedTypeName.get(ClassName.get(ModelKey.class), typeVariableName);

    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("switch(key.name())");
    for (ModelKeyDefinition key : definition.keys()) {
      if (key.getValueType().isPrimitive()) {
        body.add("case $S: this.$L(($T)value); break; \n", key.getName(), key.getName(), key.getValueType().box());
      } else {
        body.add("case $S: this.$L(($T)value); break; \n", key.getName(), key.getName(), key.getEffectiveType());
      }
    }
    body.add("default: throw new $T($S + key.name());\n", IllegalArgumentException.class, "No such field: ");
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

  private static String findTypeVariableName(ModelUpTypeDefinition definition) {
    String target = "T";
    boolean hasMatch = anyMatchingParams(definition, target);
    while (hasMatch) {
      target += "T";
      hasMatch = anyMatchingParams(definition, target);
    }
    return target;
  }

  private static boolean anyMatchingParams(ModelUpTypeDefinition definition, String target) {
    return definition.typeParameters().stream()//
        .anyMatch(x -> x.name.equals(target));
  }

  private static boolean needsUnsafeCastAnnotation(ModelUpTypeDefinition definition) {
    return definition.keys().stream().anyMatch(field -> {
      TypeName name = field.getEffectiveType();
      if (name instanceof TypeVariableName) {
        return true;
      } else if (name instanceof ParameterizedTypeName) {
        return true;
      }
      return false;
    });
  }

}
