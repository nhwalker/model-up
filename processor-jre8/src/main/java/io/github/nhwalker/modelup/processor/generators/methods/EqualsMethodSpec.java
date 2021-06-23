package io.github.nhwalker.modelup.processor.generators.methods;

import java.util.Objects;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

import io.github.nhwalker.modelup.processor.TypeNameUtils;
import io.github.nhwalker.modelup.processor.descriptors.KeyDescriptor;
import io.github.nhwalker.modelup.processor.descriptors.ModelDescriptor;

public class EqualsMethodSpec {

  public static MethodSpec createForRecord(ModelDescriptor descriptor, TypeName name) {
    return equalsMethod(descriptor, name, true);
  }

  public static MethodSpec createForArgs(ModelDescriptor descriptor, TypeName name) {
    return equalsMethod(descriptor, name, false);
  }

  private static MethodSpec equalsMethod(ModelDescriptor descriptor, TypeName name, boolean mayMemorizeHash) {
    MethodSpec.Builder method = MethodSpec.methodBuilder("equals")//
        .addModifiers(Modifier.PUBLIC)//
        .returns(boolean.class)//
        .addParameter(Object.class, "obj")//
        .addAnnotation(Override.class);

    CodeBlock.Builder code = CodeBlock.builder();

    code.beginControlFlow("if (this == obj)");
    code.addStatement("return true");
    code.endControlFlow();

    code.beginControlFlow("if (obj == null)");
    code.addStatement("return false");
    code.endControlFlow();

    code.beginControlFlow("if (getClass() != obj.getClass())");
    code.addStatement("return false");
    code.endControlFlow();

    TypeName otherType = name;
    if (!descriptor.getTypeParameters().isEmpty()) {
      TypeName[] params = new TypeName[descriptor.getTypeParameters().size()];
      for (int i = 0; i < params.length; i++) {
        params[i] = WildcardTypeName.subtypeOf(Object.class);
      }
      otherType = ParameterizedTypeName.get(TypeNameUtils.rawType(otherType), params);
    }
    code.addStatement("$T other = ($T)obj", otherType, otherType);

    if (mayMemorizeHash && descriptor.getRecordType().isMemorizeHash()) {
      code.beginControlFlow(
          "if (this.hashCode !=null && other.hashCode !=null && !this.hashCode.equals(other.hashCode))");
      code.addStatement("return false");
      code.endControlFlow();
    }

    code.add("$[return ");
    if (descriptor.getKeys().isEmpty()) {
      code.add("true");
    } else {
      boolean delim = false;
      for (KeyDescriptor key : descriptor.getKeys().values()) {
        if (delim) {
          code.add("\n&& ");
        } else {
          delim = true;
        }
        if (key.isPrimitive()) {
          code.add("this.$L == other.$L", key.name(), key.name());
        } else {
          code.add("$T.equals(this.$L, other.$L)", Objects.class, key.name(), key.name());
        }
      }
    }
    code.add(";\n$]");

    method.addCode(code.build());

    return method.build();
  }

}
