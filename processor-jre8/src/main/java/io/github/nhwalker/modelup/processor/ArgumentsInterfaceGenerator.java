package io.github.nhwalker.modelup.processor;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import io.github.nhwalker.modelup.ModelOrArgs;

public class ArgumentsInterfaceGenerator extends AbstractModelKeyBasedGenerator {

  @Override
  protected JavaFile doCreate() {
    return JavaFile.builder(TypeNameUtils.packageName(getDefinition().argsBaseType()), createTypeSpec())//
        .addFileComment(fileComment())//
        .build();
  }

  private String fileComment() {
    return "";// TODO
  }

  private TypeSpec createTypeSpec() {
    TypeSpec.Builder builder = TypeSpec.interfaceBuilder(TypeNameUtils.rawType(getDefinition().argsBaseType()));
    builder.addModifiers(Modifier.PUBLIC);
    getDefinition().typeParameters().forEach(arg -> {
      builder.addTypeVariable(arg);
    });
    for (ModelKeyDefinition key : getKeys()) {
      builder.addMethod(createGetter(key));
      builder.addMethod(createSetter(key));
    }

    for (TypeName superType : getDefinition().argsTypeExtends()) {
      builder.addSuperinterface(superType);
    }

    builder.addMethod(copyMethod(getDefinition().argsType()));
    builder.addMethod(copyMethod(getDefinition().modelType()));
    builder.addMethod(copyGenericMethod());

    if (getDefinition().isAModel()) {
      builder.addMethod(ModelMethodFactory.fieldKeysMethodSpec(getDefinition())//
          .addModifiers(Modifier.DEFAULT).build());
      builder.addMethod(ModelMethodFactory.getMethodSpec(getDefinition(), false)//
          .addModifiers(Modifier.DEFAULT).build());
      builder.addMethod(ModelMethodFactory.hasFieldMethodSpec(getDefinition())//
          .addModifiers(Modifier.DEFAULT).build());
      builder.addMethod(ModelMethodFactory.setMethodSpec(getDefinition())//
          .addModifiers(Modifier.DEFAULT).build());
    }

    return builder.build();
  }

  private MethodSpec createGetter(ModelKeyDefinition key) {
    MethodSpec.Builder b = MethodSpec.methodBuilder(key.getName())//
        .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)//
        .returns(key.getEffectiveType());

    return b.build();
  }

  private MethodSpec createSetter(ModelKeyDefinition key) {
    MethodSpec.Builder b = MethodSpec.methodBuilder(key.getName())//
        .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)//
        .addParameter(key.getEffectiveType(), key.getName());
    return b.build();
  }

  private MethodSpec copyMethod(TypeName copySrcType) {
    MethodSpec.Builder b = MethodSpec.methodBuilder("copy")//
        .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)//
        .addParameter(copySrcType, "copy");
    for (ModelKeyDefinition key : getKeys()) {
      b.addStatement("this.$L(copy.$L())", key.getName(), key.getName());
    }
    return b.build();

  }

  private MethodSpec copyGenericMethod() {
    MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("copy")//
        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)//
        .addParameter(ModelOrArgs.class, "copyFrom")//
        .returns(void.class);
    if (!getDefinition().typeParameters().isEmpty()) {
      methodSpec.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)//
          .addMember("value", "$S", "unchecked").build());
    }
    TypeName rawTypeName = TypeNameUtils.rawType(getDefinition().modelType());
    TypeName rawArgsName = TypeNameUtils.rawType(getDefinition().argsType());

    CodeBlock.Builder code = CodeBlock.builder();
    code.beginControlFlow("if (copyFrom instanceof $T)", rawTypeName);
    /*  */code.addStatement("this.copy(($T)copyFrom)", getDefinition().modelType());
    code.nextControlFlow("else if (copyFrom instanceof $T)", rawArgsName);
    /*  */code.addStatement("this.copy(($T)copyFrom)", getDefinition().argsType());
    code.nextControlFlow("else");
    for (TypeName superType : getDefinition().argsTypeExtends()) {
      TypeName rawSuperType = TypeNameUtils.rawType(superType);
      code.addStatement("$T.super.copy(copyFrom)", rawSuperType);
    }
    code.endControlFlow();
    methodSpec.addCode(code.build());
    return methodSpec.build();
  }

}
