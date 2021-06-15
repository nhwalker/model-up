package io.github.nhwalker.modelup.processor;

import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

public class RecordGenerator extends AbstractModelKeyBasedGenerator {

  @Override
  protected JavaFile doCreate() {
    return JavaFile.builder(TypeNameUtils.packageName(getDefinition().recordType()), createTypeSpec())//
        .addFileComment(fileComment())//
        .build();
  }

  private String fileComment() {
    return "";// TODO
  }

  private TypeSpec createTypeSpec() {
    TypeSpec.Builder recordSpec = TypeSpec.classBuilder(TypeNameUtils.rawType(getDefinition().recordType()));
    recordSpec.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    getDefinition().typeParameters().forEach(arg -> {
      recordSpec.addTypeVariable(arg);
    });
    recordSpec.addSuperinterface(getDefinition().modelType());

    TypeSpec.Builder argsSpec = TypeSpec.classBuilder("Args");
    TypeName argsSpecName = ClassName.get(TypeNameUtils.packageName(getDefinition().recordType()),
        TypeNameUtils.rawType(getDefinition().recordType()).simpleName(), "Args");
    argsSpec.addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
    if (!getDefinition().typeParameters().isEmpty()) {
      getDefinition().typeParameters().forEach(arg -> {
        argsSpec.addTypeVariable(arg);
      });
      argsSpecName = ParameterizedTypeName.get((ClassName) argsSpecName,
          getDefinition().typeParameters().toArray(new TypeName[0]));
    }

    argsSpec.addSuperinterface(getDefinition().argsType());

    MethodSpec.Builder recordCopyConstructorBldr = MethodSpec.constructorBuilder()//
        .addModifiers(Modifier.PUBLIC)//
        .addParameter(argsSpecName, "args");
    MethodSpec.Builder argsCopyConstructorBldr = MethodSpec.constructorBuilder()//
        .addModifiers(Modifier.PUBLIC)//
        .addParameter(argsSpecName, "args");
    MethodSpec.Builder recordCannonConstructorBldr = MethodSpec.constructorBuilder()//
        .addModifiers(Modifier.PUBLIC);

    for (ModelKeyDefinition key : getKeys()) {
      FieldSpec recordField = FieldSpec//
          .builder(key.getEffectiveType(), key.getName(), Modifier.PRIVATE, Modifier.FINAL)//
          .build();
      FieldSpec argsField = FieldSpec//
          .builder(key.getEffectiveType(), key.getName(), Modifier.PRIVATE)//
          .build();

      recordSpec.addField(recordField);
      recordSpec.addMethod(createGetter(key));

      argsSpec.addField(argsField);
      argsSpec.addMethod(createGetter(key));
      argsSpec.addMethod(createSetter(key));

      recordCopyConstructorBldr.addStatement("this.$N = args.$N()", key.getName(), key.getName());
      argsCopyConstructorBldr.addStatement("this.$N = args.$N()", key.getName(), key.getName());
      recordCannonConstructorBldr.addStatement("this.$N = $N", key.getName(), key.getName());
      recordCannonConstructorBldr.addParameter(key.getEffectiveType(), key.getName());
    }

    recordSpec.addMethod(configureMethod(argsSpecName));
    recordSpec.addMethod(MethodSpec.constructorBuilder()//
        .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class), argsSpecName), "configure")//
        .addStatement("this(applyConfig(new $T(), configure))", argsSpecName)//
        .addModifiers(Modifier.PUBLIC)//
        .build());
    recordSpec.addMethod(recordCopyConstructorBldr.build());
    recordSpec.addMethod(recordCannonConstructorBldr.build());

    argsSpec.addMethod(argsCopyConstructorBldr.build());

    argsSpec.addMethod(MethodSpec.constructorBuilder()//
        .addModifiers(Modifier.PUBLIC)//
        .build());

    recordSpec.addType(argsSpec.build());

    return recordSpec.build();
  }

  private MethodSpec configureMethod(TypeName argsType) {
    MethodSpec.Builder m = MethodSpec.methodBuilder("applyConfig")//
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)//
        .returns(argsType)//
        .addParameter(argsType, "args")//
        .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class), argsType), "configure")//
        .addStatement("configure.accept(args)")//
        .addStatement("return args");
    getDefinition().typeParameters().forEach(arg -> {
      m.addTypeVariable(arg);
    });
    return m.build();
  }

  private MethodSpec createGetter(ModelKeyDefinition key) {
    MethodSpec.Builder b = MethodSpec.methodBuilder(key.getName())//
        .addModifiers(Modifier.PUBLIC)//
        .returns(key.getEffectiveType())//
        .addStatement("return this.$N", key.getName());
    return b.build();
  }

  public MethodSpec createSetter(ModelKeyDefinition key) {
    MethodSpec.Builder b = MethodSpec.methodBuilder(key.getName())//
        .addModifiers(Modifier.PUBLIC)//
        .addParameter(key.getEffectiveType(), key.getName())//
        .addStatement("this.$N = $N", key.getName(), key.getName());
    return b.build();
  }

  public MethodSpec createDefaultConstructor() {
    MethodSpec.Builder b = MethodSpec.constructorBuilder()//
        .addModifiers(Modifier.PUBLIC);

    return b.build();

  }

  public MethodSpec createCopyConstructor() {
    MethodSpec.Builder b = MethodSpec.constructorBuilder()//
        .addModifiers(Modifier.PUBLIC);

    return b.build();
  }
}
