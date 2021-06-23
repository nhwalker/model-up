package io.github.nhwalker.modelup.processor.generators.methods;

import javax.lang.model.element.Modifier;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.WildcardTypeName;

import io.github.nhwalker.modelup.ModelKey;
import io.github.nhwalker.modelup.processor.descriptors.KeyDescriptor;
import io.github.nhwalker.modelup.processor.descriptors.ModelDescriptor;

public class HasFieldMethodSpec {

  private static MethodSpec.Builder base(ModelDescriptor definition) {
    ParameterizedTypeName fieldKeyWildcardType = //
        ParameterizedTypeName.get(ClassName.get(ModelKey.class), WildcardTypeName.subtypeOf(Object.class));

    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("switch(key.name())");
    for (KeyDescriptor key : definition.getKeys().values()) {
      body.add("case $S:\n", key.name());
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

  public static MethodSpec.Builder createForInterface(ModelDescriptor definition) {
    MethodSpec.Builder builder = base(definition);
    builder.addModifiers(Modifier.DEFAULT);
    return builder;
  }

  public static MethodSpec.Builder createForClass(ModelDescriptor definition) {
    MethodSpec.Builder builder = base(definition);
    return builder;
  }

}
