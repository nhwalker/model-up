package io.github.nhwalker.modelup.processor.generators.methods;


import javax.lang.model.element.Modifier;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import io.github.nhwalker.modelup.ModelOrArgs;
import io.github.nhwalker.modelup.processor.TypeNameUtils;
import io.github.nhwalker.modelup.processor.descriptors.ModelDescriptor;

public class GenericCopyMethodSpec {

  public static MethodSpec.Builder createForInterface(ModelDescriptor descriptor) {
    return base(descriptor)//
        .addModifiers(Modifier.DEFAULT);
  }

  private static MethodSpec.Builder base(ModelDescriptor definition) {
    MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("copy")//
        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)//
        .addParameter(ModelOrArgs.class, "copyFrom")//
        .returns(void.class);
    if (!definition.getTypeParameters().isEmpty()) {
      methodSpec.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)//
          .addMember("value", "$S", "unchecked").build());
    }
    TypeName rawTypeName = TypeNameUtils.rawType(definition.getModelType());
    TypeName rawArgsName = TypeNameUtils.rawType(definition.getArgsType().getArgsType());

    CodeBlock.Builder code = CodeBlock.builder();
    code.beginControlFlow("if (copyFrom instanceof $T)", rawTypeName);
    /*  */code.addStatement("this.copy(($T)copyFrom)", definition.getModelType());
    code.nextControlFlow("else if (copyFrom instanceof $T)", rawArgsName);
    /*  */code.addStatement("this.copy(($T)copyFrom)", definition.getArgsType().getArgsType());
    code.nextControlFlow("else");
    
    for (TypeName superType : definition.getArgsParentTypes()) {
      TypeName rawSuperType = TypeNameUtils.rawType(superType);
      code.addStatement("$T.super.copy(copyFrom)", rawSuperType);
    }
    code.endControlFlow();
    methodSpec.addCode(code.build());
    return methodSpec;
  }

}
