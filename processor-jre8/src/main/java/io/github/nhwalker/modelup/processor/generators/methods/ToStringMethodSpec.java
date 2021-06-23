package io.github.nhwalker.modelup.processor.generators.methods;

import java.util.Iterator;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import io.github.nhwalker.modelup.processor.TypeNameUtils;
import io.github.nhwalker.modelup.processor.descriptors.KeyDescriptor;
import io.github.nhwalker.modelup.processor.descriptors.ModelDescriptor;

public class ToStringMethodSpec {

  public static void addToRecordType(ModelDescriptor definition, TypeSpec.Builder type) {
    base(definition, type, definition.getRecordType().getRecordType(), true);
  }

  public static void addToArgsType(ModelDescriptor definition, TypeSpec.Builder type) {
    base(definition, type, definition.getArgsType().getArgsType(), false);
  }

  private static void base(ModelDescriptor definition, TypeSpec.Builder type, TypeName name, boolean mayMemorize) {

    MethodSpec.Builder toString = MethodSpec.methodBuilder("toString")//
        .addModifiers(Modifier.PUBLIC)//
        .returns(String.class)//
        .addAnnotation(Override.class);

    CodeBlock.Builder computeCode = CodeBlock.builder();
    computeCode.add("\"$L[\"\n", String.join(".", TypeNameUtils.rawType(name).simpleNames()));
    Iterator<KeyDescriptor> keyIter = definition.getKeys().values().iterator();
    if (keyIter.hasNext()) {
      KeyDescriptor key = keyIter.next();
      computeCode.add("+ \"$L=\" + $L", key.name(), key.name());
      while (keyIter.hasNext()) {
        key = keyIter.next();
        computeCode.add("\n+ \", $L=\" + $L", key.name(), key.name());
      }
    }
    computeCode.add("+ \"]\"");

    if (mayMemorize && definition.getRecordType().isMemorizeToString()) {
      type.addField(FieldSpec.builder(String.class, "toString", Modifier.PRIVATE).build());
      CodeBlock.Builder checkCode = CodeBlock.builder();
      checkCode.beginControlFlow("if (this.toString == null)");
      checkCode.add("$[this.toString = ");
      checkCode.add(computeCode.build());
      checkCode.add(";\n$]");
      checkCode.endControlFlow();
      toString.addCode(checkCode.build());
      toString.addStatement("return this.toString");
    } else {
      CodeBlock.Builder code = CodeBlock.builder();
      code.add("$[return ");
      code.add(computeCode.build());
      code.add(";\n$]");
      toString.addCode(code.build());
    }

    type.addMethod(toString.build());

  }
}
