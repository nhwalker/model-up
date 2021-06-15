package io.github.nhwalker.modelup.processor;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;

import io.github.nhwalker.modelup.Model;
import io.github.nhwalker.modelup.ModelArgs;
import io.github.nhwalker.modelup.ModelKey;
import io.github.nhwalker.modelup.ModelWithArgs;

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

    // TODO: Do I want to include this?
    if (getDefinition().isAModelWithArgs()) {
      recordSpec.addSuperinterface(
          ParameterizedTypeName.get(ClassName.get(ModelWithArgs.class), getDefinition().recordType(), argsTypeName));
    } else if (getDefinition().isAModel()) {
      recordSpec.addSuperinterface(ClassName.get(Model.class));
    }

    for (ModelKeyDefinition key : getKeys()) {
      FieldSpec recordField = FieldSpec//
          .builder(key.getEffectiveType(), key.getName(), Modifier.PRIVATE, Modifier.FINAL)//
          .build();

      recordSpec.addField(recordField);
      recordSpec.addMethod(createGetter(key));

    }

    recordSpec.addMethod(defaultRecordConstructor(argsTypeName));
    recordSpec.addMethod(configureMethod(argsTypeName));
    recordSpec.addMethod(configureConstructor(argsTypeName));
    recordSpec.addMethod(copyConstructor(argsTypeName));
    recordSpec.addMethod(recordCanonConstructor(argsTypeName));

    addHashCodeMethod(recordSpec, true);
    recordSpec.addMethod(equalsMethod(getDefinition().recordType(), true));
    addToStringMethod(getDefinition().recordType(), recordSpec, true);

    recordSpec.addType(createArgsType(argsTypeRawName, argsTypeName));

    if (getDefinition().isAModel()) {
      recordSpec.addMethod(fieldKeysMethodSpec());
      recordSpec.addMethod(getMethodSpec());
      recordSpec.addMethod(hasFieldMethodSpec());
    }
    if (getDefinition().isAModelWithArgs()) {
      recordSpec.addMethod(withUpdateMethod(argsTypeName));
    }

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
    if (getDefinition().isAModel()) {
      argsSpec.addSuperinterface(ClassName.get(ModelArgs.class));
    }

    for (ModelKeyDefinition key : getKeys()) {
      FieldSpec argsField = FieldSpec//
          .builder(key.getEffectiveType(), key.getName(), Modifier.PRIVATE)//
          .build();

      argsSpec.addField(argsField);
      argsSpec.addMethod(createGetter(key));
      argsSpec.addMethod(createSetter(key));
    }

    argsSpec.addMethod(defaultArgsConstructor());
    argsSpec.addMethod(copyConstructor(argsSpecName));
    argsSpec.addMethod(copyConstructor(getDefinition().modelType()));
    argsSpec.addMethod(argsCanonConstructor());

    addHashCodeMethod(argsSpec, false);
    argsSpec.addMethod(equalsMethod(argsSpecRawName, false));
    addToStringMethod(argsSpecRawName, argsSpec, false);

    if (getDefinition().isAModel()) {
      argsSpec.addMethod(fieldKeysMethodSpec());
      argsSpec.addMethod(getMethodSpec());
      argsSpec.addMethod(hasFieldMethodSpec());
      argsSpec.addMethod(setMethodSpec());
    }

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

  private MethodSpec recordCanonConstructor(TypeName argsType) {
    MethodSpec.Builder bldr = MethodSpec.constructorBuilder()//
        .addModifiers(Modifier.PUBLIC);

    if (!getDefinition().sanatizeArgsMethods().isEmpty()) {
      String copyCall = getKeys().stream()//
          .map(x -> x.getName())//
          .collect(Collectors.joining(",$W", "$T _toSanatize = new $T(", ")"));
      bldr.addStatement(copyCall, argsType, argsType);

      for (StaticMethodId method : getDefinition().sanatizeArgsMethods()) {
        bldr.addStatement("$T.$L(_toSanatize)", method.getType(), method.getName());
      }

      for (ModelKeyDefinition key : getKeys()) {
        bldr.addStatement("this.$N = _toSanatize.$N()", key.getName(), key.getName());
        bldr.addParameter(key.getEffectiveType(), key.getName());
      }
    } else {
      for (ModelKeyDefinition key : getKeys()) {
        bldr.addStatement("this.$N = $N", key.getName(), key.getName());
        bldr.addParameter(key.getEffectiveType(), key.getName());
      }
    }

    for (StaticMethodId method : getDefinition().validateMethods()) {
      bldr.addStatement("$T.$L(this)", method.getType(), method.getName());
    }

    return bldr.build();
  }

  private MethodSpec argsCanonConstructor() {
    MethodSpec.Builder cannonConstructorBldr = MethodSpec.constructorBuilder()//
        .addModifiers(Modifier.PUBLIC);

    for (ModelKeyDefinition key : getKeys()) {
      cannonConstructorBldr.addStatement("this.$N = $N", key.getName(), key.getName());
      cannonConstructorBldr.addParameter(key.getEffectiveType(), key.getName());
    }
    return cannonConstructorBldr.build();
  }

  private MethodSpec defaultRecordConstructor(TypeName argsTypeName) {
    MethodSpec.Builder b = MethodSpec.constructorBuilder()//
        .addModifiers(Modifier.PUBLIC);
    b.addStatement("this(new $T())", argsTypeName);
    return b.build();
  }

  private MethodSpec defaultArgsConstructor() {
    MethodSpec.Builder b = MethodSpec.constructorBuilder()//
        .addModifiers(Modifier.PUBLIC);
    if (!getDefinition().initializeArgsMethods().isEmpty()) {
      for (StaticMethodId method : getDefinition().initializeArgsMethods()) {
        b.addStatement("$T.$L(this)", method.getType(), method.getName());
      }
    }
    return b.build();

  }

  private MethodSpec configureConstructor(TypeName argsTypeName) {
    return MethodSpec.constructorBuilder()//
        .addParameter(configureType(argsTypeName), "configure")//
        .addStatement("this(applyConfig(new $T(), configure))", argsTypeName)//
        .addModifiers(Modifier.PUBLIC)//
        .build();
  }

  private MethodSpec withUpdateMethod(TypeName argsTypeName) {
    return MethodSpec.methodBuilder("withUpdate")//
        .addParameter(configureType(argsTypeName), "configure")//
        .addStatement("$T args = new $T(this)", argsTypeName, argsTypeName)//
        .addStatement("return new $T(applyConfig(args, configure))", getDefinition().recordType())//
        .returns(getDefinition().recordType()).addModifiers(Modifier.PUBLIC)//
        .build();
  }

  private TypeName configureType(TypeName argsTypeName) {
    return ParameterizedTypeName.get(ClassName.get(Consumer.class), WildcardTypeName.supertypeOf(argsTypeName));
  }

  private MethodSpec configureMethod(TypeName argsTypeName) {
    MethodSpec.Builder m = MethodSpec.methodBuilder("applyConfig")//
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)//
        .returns(argsTypeName)//
        .addParameter(argsTypeName, "args")//
        .addParameter(configureType(argsTypeName), "configure")//
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

  private void addHashCodeMethod(TypeSpec.Builder type, boolean mayMemorize) {
    MethodSpec.Builder hashCode = MethodSpec.methodBuilder("hashCode")//
        .addModifiers(Modifier.PUBLIC)//
        .returns(int.class)//
        .addAnnotation(Override.class);

    CodeBlock.Builder computeCode = CodeBlock.builder();
    computeCode.addStatement("final int prime = 31");
    computeCode.addStatement("int result = 1");
    for (ModelKeyDefinition key : getDefinition().keys()) {
      if (key.getValueType().isPrimitive()) {
        computeCode.addStatement(//
            "result = prime * result + $T.hashCode($L)", //
            key.getValueType().box(), key.getName());
      } else {
        computeCode.addStatement(//
            "result = prime * result + $T.hashCode($L)", //
            Objects.class, key.getName());
      }
    }

    if (mayMemorize && getDefinition().memorizeHash()) {
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

//  private MethodSpec hashCodeMethod() {
//    MethodSpec.Builder method = MethodSpec.methodBuilder("hashCode")//
//        .addModifiers(Modifier.PUBLIC)//
//        .returns(int.class)//
//        .addAnnotation(Override.class);
//
//    CodeBlock.Builder code = CodeBlock.builder();
//    code.addStatement("final int prime = 31");
//    code.addStatement("int result = 1");
//    for (ModelKeyDefinition key : getDefinition().keys()) {
//      if (key.getValueType().isPrimitive()) {
//        code.addStatement(//
//            "result = prime * result + $T.hashCode($L)", //
//            key.getValueType().box(), key.getName());
//      } else {
//        code.addStatement(//
//            "result = prime * result + $T.hashCode($L)", //
//            Objects.class, key.getName());
//      }
//    }
//    code.addStatement("return result");
//
//    method.addCode(code.build());
//
//    return method.build();
//  }

  private MethodSpec equalsMethod(TypeName name, boolean mayMemorizeHash) {
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

    if (mayMemorizeHash && getDefinition().memorizeHash()) {
      code.beginControlFlow(
          "if (this.hashCode !=null && other.hashCode !=null && !this.hashCode.equals(other.hashCode))");
      code.addStatement("return false");
      code.endControlFlow();
    }

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

  private void addToStringMethod(TypeName name, TypeSpec.Builder type, boolean mayMemorize) {
    MethodSpec.Builder toString = MethodSpec.methodBuilder("toString")//
        .addModifiers(Modifier.PUBLIC)//
        .returns(String.class)//
        .addAnnotation(Override.class);

    CodeBlock.Builder computeCode = CodeBlock.builder();
    computeCode.add("\"$L[\"\n", String.join(".", TypeNameUtils.rawType(name).simpleNames()));
    Iterator<ModelKeyDefinition> keyIter = getKeys().iterator();
    if (keyIter.hasNext()) {
      ModelKeyDefinition key = keyIter.next();
      computeCode.add("+ \"$L=\" + $L", key.getName(), key.getName());
      while (keyIter.hasNext()) {
        key = keyIter.next();
        computeCode.add("\n+ \", $L=\" + $L", key.getName(), key.getName());
      }
    }
    computeCode.add("+ \"]\"");

    if (mayMemorize && getDefinition().memorizeToString()) {
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

  private MethodSpec fieldKeysMethodSpec() {
    ParameterizedTypeName fieldKeyWildcardType = //
        ParameterizedTypeName.get(ClassName.get(ModelKey.class), WildcardTypeName.subtypeOf(Object.class));
    ParameterizedTypeName fieldSetType = ParameterizedTypeName.get(ClassName.get(Set.class), fieldKeyWildcardType);

    MethodSpec methodSpec = MethodSpec.methodBuilder("fieldKeys")//
        .addModifiers(Modifier.PUBLIC)//
        .returns(fieldSetType)//
        .addStatement("return $T.fields()", getDefinition().keysType())//
        .build();
    return methodSpec;
  }

  private MethodSpec getMethodSpec() {
    TypeVariableName typeVariableName = TypeVariableName.get(findTypeVariableName());
    ParameterizedTypeName fieldKeyWildcardType = //
        ParameterizedTypeName.get(ClassName.get(ModelKey.class), typeVariableName);

    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("switch(key.name())");
    for (ModelKeyDefinition key : getDefinition().keys()) {
      body.add("case $S: return key.type().cast(this.$L);\n", key.getName(), key.getName());
    }
    body.add("default: throw new $T($S + key.name());\n", IllegalArgumentException.class, "No such field: ");
    body.endControlFlow();

    MethodSpec methodSpec = MethodSpec.methodBuilder("get")//
        .addModifiers(Modifier.PUBLIC)//
        .returns(typeVariableName)//
        .addParameter(ParameterSpec.builder(fieldKeyWildcardType, "key").build())//
        .addCode(body.build())//
        .addTypeVariable(typeVariableName)//
        .build();
    return methodSpec;
  }

  private boolean anyMatchingParams(String target) {
    return getDefinition().typeParameters().stream()//
        .anyMatch(x -> x.name.equals(target));
  }

  private String findTypeVariableName() {
    String target = "T";
    boolean hasMatch = anyMatchingParams(target);
    while (hasMatch) {
      target += "T";
      hasMatch = anyMatchingParams(target);
    }
    return target;
  }

  private MethodSpec setMethodSpec() {

    TypeVariableName typeVariableName = TypeVariableName.get(findTypeVariableName());

    ParameterizedTypeName fieldKeyWildcardType = //
        ParameterizedTypeName.get(ClassName.get(ModelKey.class), typeVariableName);

    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("switch(key.name())");
    for (ModelKeyDefinition key : getDefinition().keys()) {
      if (key.getValueType().isPrimitive()) {
        body.add("case $S: this.$L = ($T)value; break; \n", key.getName(), key.getName(), key.getValueType().box());
      } else {
        body.add("case $S: this.$L = ($T)value; break; \n", key.getName(), key.getName(), key.getEffectiveType());
      }
    }
    body.add("default: throw new $T($S + key.name());\n", IllegalArgumentException.class, "No such field: ");
    body.endControlFlow();

    MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("set")//
        .addModifiers(Modifier.PUBLIC)//
        .addParameter(ParameterSpec.builder(fieldKeyWildcardType, "key").build())//
        .addParameter(ParameterSpec.builder(typeVariableName, "value").build())//
        .addCode(body.build())//
        .addTypeVariable(typeVariableName);
    if (needsUnsafeCastAnnotation()) {
      methodSpec.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)//
          .addMember("value", "$S", "unchecked").build());
    }
    return methodSpec.build();
  }

  private boolean needsUnsafeCastAnnotation() {
    return getDefinition().keys().stream().anyMatch(field -> {
      TypeName name = field.getEffectiveType();
      if (name instanceof TypeVariableName) {
        return true;
      } else if (name instanceof ParameterizedTypeName) {
        return true;
      }
      return false;
    });
  }

  private MethodSpec hasFieldMethodSpec() {
    ParameterizedTypeName fieldKeyWildcardType = //
        ParameterizedTypeName.get(ClassName.get(ModelKey.class), WildcardTypeName.subtypeOf(Object.class));

    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("switch(key.name())");
    for (ModelKeyDefinition key : getDefinition().keys()) {
      body.add("case $S: return true;\n", key.getName());
    }
    body.add("default: return false;\n");
    body.endControlFlow();

    MethodSpec methodSpec = MethodSpec.methodBuilder("hasField")//
        .addModifiers(Modifier.PUBLIC)//
        .returns(boolean.class)//
        .addParameter(ParameterSpec.builder(fieldKeyWildcardType, "key").build()).addCode(body.build()).build();
    return methodSpec;
  }
}
