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

    builder.addMethod(copyMethod(getDefinition().argsType()));
    builder.addMethod(copyMethod(getDefinition().modelType()));

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
}
