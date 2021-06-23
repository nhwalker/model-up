package io.github.nhwalker.modelup.processor.generators.methods;

import java.util.Objects;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import io.github.nhwalker.modelup.processor.descriptors.KeyDescriptor;
import io.github.nhwalker.modelup.processor.descriptors.ModelDescriptor;

public class HashCodeMethodSpec {

  public static void addToRecordType(ModelDescriptor definition, TypeSpec.Builder type) {
    base(definition, type, true);
  }

  public static void addToArgsType(ModelDescriptor definition, TypeSpec.Builder type) {
    base(definition, type, false);
  }

  private static void base(ModelDescriptor definition, TypeSpec.Builder type, boolean mayMemorize) {
    MethodSpec.Builder hashCode = MethodSpec.methodBuilder("hashCode")//
        .addModifiers(Modifier.PUBLIC)//
        .returns(int.class)//
        .addAnnotation(Override.class);

    CodeBlock.Builder computeCode = CodeBlock.builder();
    computeCode.addStatement("final int prime = 31");
    computeCode.addStatement("int result = 1");
    for (KeyDescriptor key : definition.getKeys().values()) {
      if (key.isPrimitive()) {
        computeCode.addStatement("result = prime * result + $T.hashCode($L)", //
            key.boxedType(), key.name());
      } else {
        computeCode.addStatement("result = prime * result + $T.hashCode($L)", //
            Objects.class, key.name());
      }
    }

    if (mayMemorize && definition.getRecordType().isMemorizeHash()) {
      type.addField(FieldSpec.builder(Integer.class, "hashCode", Modifier.PRIVATE).build());
      CodeBlock.Builder checkCode = CodeBlock.builder();
      checkCode.beginControlFlow("if (this.hashCode == null)");
      checkCode.add(computeCode.build());
      checkCode.addStatement("this.hashCode = result");
      checkCode.addStatement("return result");
      checkCode.endControlFlow();
      checkCode.addStatement("return this.hashCode");
      hashCode.addCode(checkCode.build());
    } else {
      hashCode.addCode(computeCode.build());
      hashCode.addStatement("return result");
    }

    type.addMethod(hashCode.build());
  }
}
