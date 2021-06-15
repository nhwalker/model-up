package io.github.nhwalker.modelup.processor;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

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
}
