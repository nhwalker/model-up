package io.github.nhwalker.modelup.processor;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

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
    ClassName argsTypeRawName = ClassName.get(TypeNameUtils.packageName(getDefinition().recordType()),
        TypeNameUtils.rawType(getDefinition().recordType()).simpleName(), "Args");
    TypeName argsTypeName = argsTypeRawName;
    if (!getDefinition().typeParameters().isEmpty()) {
      argsTypeName = ParameterizedTypeName.get(argsTypeRawName,
          getDefinition().typeParameters().toArray(new TypeName[0]));
    }

    TypeSpec.Builder recordSpec = TypeSpec.classBuilder(TypeNameUtils.rawType(getDefinition().recordType()));
    recordSpec.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    getDefinition().typeParameters().forEach(arg -> {
      recordSpec.addTypeVariable(arg);
    });
    recordSpec.addSuperinterface(getDefinition().modelType());

    for (ModelKeyDefinition key : getKeys()) {
      FieldSpec recordField = FieldSpec//
          .builder(key.getEffectiveType(), key.getName(), Modifier.PRIVATE, Modifier.FINAL)//
          .build();

      recordSpec.addField(recordField);
      recordSpec.addMethod(createGetter(key));

    }

    recordSpec.addMethod(configureMethod(argsTypeName));
    recordSpec.addMethod(configureConstructor(argsTypeName));
    recordSpec.addMethod(copyConstructor(argsTypeName));
    recordSpec.addMethod(canonConstructor());

    recordSpec.addMethod(withChangeMethod(argsTypeName));
    recordSpec.addMethod(hashCodeMethod());
    recordSpec.addMethod(equalsMethod(getDefinition().recordType()));
    recordSpec.addMethod(toStringMethod(getDefinition().recordType()));

    recordSpec.addType(createArgsType(argsTypeRawName, argsTypeName));

    return recordSpec.build();
  }

  private TypeSpec createArgsType(ClassName argsSpecRawName, TypeName argsSpecName) {
    TypeSpec.Builder argsSpec = TypeSpec.classBuilder(argsSpecRawName);

    argsSpec.addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
    if (!getDefinition().typeParameters().isEmpty()) {
      getDefinition().typeParameters().forEach(arg -> {
        argsSpec.addTypeVariable(arg);
      });
    }

    argsSpec.addSuperinterface(getDefinition().argsType());

    for (ModelKeyDefinition key : getKeys()) {
      FieldSpec argsField = FieldSpec//
          .builder(key.getEffectiveType(), key.getName(), Modifier.PRIVATE)//
          .build();

      argsSpec.addField(argsField);
      argsSpec.addMethod(createGetter(key));
      argsSpec.addMethod(createSetter(key));
    }

    argsSpec.addMethod(defaultConstructor());
    argsSpec.addMethod(copyConstructor(argsSpecName));
    argsSpec.addMethod(copyConstructor(getDefinition().modelType()));
    argsSpec.addMethod(canonConstructor());

    argsSpec.addMethod(hashCodeMethod());
    argsSpec.addMethod(equalsMethod(argsSpecRawName));
    argsSpec.addMethod(toStringMethod(argsSpecRawName));

    return argsSpec.build();

  }

  private MethodSpec copyConstructor(TypeName argument) {
    MethodSpec.Builder copyConstructorBldr = MethodSpec.constructorBuilder()//
        .addModifiers(Modifier.PUBLIC)//
        .addParameter(argument, "args");

    String copyCall = getKeys().stream()//
        .map(x -> "args." + x.getName() + "()")//
        .collect(Collectors.joining(",$W", "this(", ")"));
    copyConstructorBldr.addStatement(copyCall);

    return copyConstructorBldr.build();
  }

  private MethodSpec canonConstructor() {
    MethodSpec.Builder cannonConstructorBldr = MethodSpec.constructorBuilder()//
        .addModifiers(Modifier.PUBLIC);

    for (ModelKeyDefinition key : getKeys()) {
      cannonConstructorBldr.addStatement("this.$N = $N", key.getName(), key.getName());
      cannonConstructorBldr.addParameter(key.getEffectiveType(), key.getName());
    }
    return cannonConstructorBldr.build();
  }

  private MethodSpec defaultConstructor() {
    MethodSpec.Builder b = MethodSpec.constructorBuilder()//
        .addModifiers(Modifier.PUBLIC);

    return b.build();

  }

  private MethodSpec configureConstructor(TypeName argsTypeName) {
    return MethodSpec.constructorBuilder()//
        .addParameter(
            ParameterizedTypeName.get(ClassName.get(Consumer.class), WildcardTypeName.supertypeOf(argsTypeName)),
            "configure")//
        .addStatement("this(applyConfig(new $T(), configure))", argsTypeName)//
        .addModifiers(Modifier.PUBLIC)//
        .build();
  }

  private MethodSpec withChangeMethod(TypeName argsTypeName) {
    return MethodSpec.methodBuilder("withChange")//
        .addParameter(
            ParameterizedTypeName.get(ClassName.get(Consumer.class), WildcardTypeName.supertypeOf(argsTypeName)),
            "configure")//
        .addStatement("$T args = new $T(this)", argsTypeName, argsTypeName)//
        .addStatement("return new $T(applyConfig(args, configure))", getDefinition().recordType())//
        .returns(getDefinition().recordType()).addModifiers(Modifier.PUBLIC)//
        .build();
  }

  private MethodSpec configureMethod(TypeName argsTypeName) {
    MethodSpec.Builder m = MethodSpec.methodBuilder("applyConfig")//
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)//
        .returns(argsTypeName)//
        .addParameter(argsTypeName, "args")//
        .addParameter(
            ParameterizedTypeName.get(ClassName.get(Consumer.class), WildcardTypeName.supertypeOf(argsTypeName)),
            "configure")//
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

  private MethodSpec createSetter(ModelKeyDefinition key) {
    MethodSpec.Builder b = MethodSpec.methodBuilder(key.getName())//
        .addModifiers(Modifier.PUBLIC)//
        .addParameter(key.getEffectiveType(), key.getName())//
        .addStatement("this.$N = $N", key.getName(), key.getName());
    return b.build();
  }

  private MethodSpec hashCodeMethod() {
    MethodSpec.Builder method = MethodSpec.methodBuilder("hashCode")//
        .addModifiers(Modifier.PUBLIC)//
        .returns(int.class)//
        .addAnnotation(Override.class);

    CodeBlock.Builder code = CodeBlock.builder();
    code.addStatement("final int prime = 31");
    code.addStatement("int result = 1");
    for (ModelKeyDefinition key : getDefinition().keys()) {
      if (key.getValueType().isPrimitive()) {
        code.addStatement(//
            "result = prime * result + $T.hashCode($L)", //
            key.getValueType().box(), key.getName());
      } else {
        code.addStatement(//
            "result = prime * result + $T.hashCode($L)", //
            Objects.class, key.getName());
      }
    }
    code.addStatement("return result");

    method.addCode(code.build());

    return method.build();
  }

  private MethodSpec equalsMethod(TypeName name) {
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
    if (!getDefinition().typeParameters().isEmpty()) {
      TypeName[] params = new TypeName[getDefinition().typeParameters().size()];
      for (int i = 0; i < params.length; i++) {
        params[i] = WildcardTypeName.subtypeOf(Object.class);
      }
      otherType = ParameterizedTypeName.get(TypeNameUtils.rawType(otherType), params);
    }
    code.addStatement("$T other = ($T)obj", otherType, otherType);

    code.add("$[return ");
    if (getDefinition().keys().isEmpty()) {
      code.add("true");
    } else {
      boolean delim = false;
      for (ModelKeyDefinition key : getDefinition().keys()) {
        if (delim) {
          code.add("\n&& ");
        } else {
          delim = true;
        }
        if (key.getValueType().isPrimitive()) {
          code.add("this.$L == other.$L", key.getName(), key.getName());
        } else {
          code.add("$T.equals(this.$L, other.$L)", Objects.class, key.getName(), key.getName());
        }
      }
    }
    code.add(";\n$]");

    method.addCode(code.build());

    return method.build();
  }

  private MethodSpec toStringMethod(TypeName name) {
    MethodSpec.Builder method = MethodSpec.methodBuilder("toString")//
        .addModifiers(Modifier.PUBLIC)//
        .returns(String.class)//
        .addAnnotation(Override.class);

    CodeBlock.Builder code = CodeBlock.builder();
    code.add("$[return \"$L[\"\n", String.join(".",TypeNameUtils.rawType(name).simpleNames()));
    Iterator<ModelKeyDefinition> keyIter = getKeys().iterator();
    if(keyIter.hasNext()) {
      ModelKeyDefinition key = keyIter.next();
      code.add("+ \"$L=\" + $L", key.getName(), key.getName());
      while(keyIter.hasNext()) {
        key = keyIter.next();
        code.add("\n+ \", $L=\" + $L", key.getName(), key.getName()); 
      }  
    }
    code.add("+ \"]\"");
    code.add(";\n$]");
    method.addCode(code.build());
    return method.build();
  }

}
