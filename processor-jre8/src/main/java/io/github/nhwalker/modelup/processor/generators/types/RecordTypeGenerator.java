package io.github.nhwalker.modelup.processor.generators.types;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import io.github.nhwalker.modelup.ModelOrArgs;
import io.github.nhwalker.modelup.processor.StaticMethodId;
import io.github.nhwalker.modelup.processor.TypeNameUtils;
import io.github.nhwalker.modelup.processor.descriptors.KeyDescriptor;
import io.github.nhwalker.modelup.processor.descriptors.ModelDescriptor;
import io.github.nhwalker.modelup.processor.descriptors.RecordTypeDescriptor;
import io.github.nhwalker.modelup.processor.generators.methods.AnyFieldGetterMethodSpec;
import io.github.nhwalker.modelup.processor.generators.methods.EqualsMethodSpec;
import io.github.nhwalker.modelup.processor.generators.methods.FieldGetterMethodSpec;
import io.github.nhwalker.modelup.processor.generators.methods.FieldKeysMethodSpec;
import io.github.nhwalker.modelup.processor.generators.methods.FieldSetterMethodSpec;
import io.github.nhwalker.modelup.processor.generators.methods.HasFieldMethodSpec;
import io.github.nhwalker.modelup.processor.generators.methods.HashCodeMethodSpec;
import io.github.nhwalker.modelup.processor.generators.methods.ToStringMethodSpec;

public class RecordTypeGenerator {

  public static JavaFile create(ModelDescriptor descriptor) {
    return JavaFile
        .builder(TypeNameUtils.packageName(descriptor.getRecordType().getRecordType()), createTypeSpec(descriptor))//
        .addFileComment(fileComment())//
        .build();
  }

  private static String fileComment() {
    return "Generated File - Do not edit";
  }

  private static TypeSpec createTypeSpec(ModelDescriptor descriptor) {
    ClassName argsTypeRawName = ClassName.get(TypeNameUtils.packageName(descriptor.getRecordType().getRecordType()),
        TypeNameUtils.rawType(descriptor.getRecordType().getRecordType()).simpleName(), "Args");
    TypeName argsTypeName = argsTypeRawName;
    if (!descriptor.getTypeParameters().isEmpty()) {
      argsTypeName = ParameterizedTypeName.get(argsTypeRawName,
          descriptor.getTypeParameters().toArray(new TypeName[0]));
    }

    TypeSpec.Builder builder = TypeSpec
        .classBuilder(TypeNameUtils.rawType(descriptor.getRecordType().getRecordType()));
    builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    descriptor.getTypeParameters().forEach(arg -> {
      builder.addTypeVariable(arg);
    });
    builder.addSuperinterface(descriptor.getModelType());

    for (KeyDescriptor key : descriptor.getKeys().values()) {
      FieldSpec recordField = FieldSpec//
          .builder(key.type(), key.name(), Modifier.PRIVATE, Modifier.FINAL)//
          .build();

      builder.addField(recordField);
      builder.addMethod(FieldGetterMethodSpec.createForClass(key).build());
    }

    builder.addMethod(FieldKeysMethodSpec.createForClass(descriptor.getKeysType()).build());
    builder.addMethod(AnyFieldGetterMethodSpec.createForClass(descriptor).build());
    builder.addMethod(HasFieldMethodSpec.createForClass(descriptor).build());

    ToStringMethodSpec.addToRecordType(descriptor, builder);
    HashCodeMethodSpec.addToRecordType(descriptor, builder);
    builder.addMethod(EqualsMethodSpec.createForRecord(descriptor, descriptor.getRecordType().getRecordType()));

    addConfigureMethod(descriptor, builder, argsTypeName);
    addRecordConstructors(argsTypeName, descriptor, builder);

    builder.addType(createArgsType(argsTypeRawName, argsTypeName, descriptor));

    return builder.build();
  }

  private static TypeSpec createArgsType(ClassName argsSpecRawName, TypeName argsSpecName, ModelDescriptor descriptor) {
    TypeSpec.Builder builder = TypeSpec.classBuilder(argsSpecRawName);
    builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
    if (!descriptor.getTypeParameters().isEmpty()) {
      descriptor.getTypeParameters().forEach(arg -> {
        builder.addTypeVariable(arg);
      });
    }

    builder.addSuperinterface(descriptor.getArgsType().getArgsType());

    for (KeyDescriptor key : descriptor.getKeys().values()) {
      FieldSpec argsField = FieldSpec//
          .builder(key.type(), key.name(), Modifier.PRIVATE)//
          .build();
      builder.addField(argsField);

      builder.addMethod(FieldGetterMethodSpec.createForClass(key).build());
      builder.addMethod(FieldSetterMethodSpec.createForClass(key).build());
    }

    ToStringMethodSpec.addToArgsType(descriptor, builder);
    HashCodeMethodSpec.addToArgsType(descriptor, builder);
    builder.addMethod(EqualsMethodSpec.createForArgs(descriptor, argsSpecName));

    addArgsConstructors(argsSpecName, descriptor, builder);

    return builder.build();
  }

  private static void addArgsConstructors(TypeName argsSpecName, ModelDescriptor descriptor, TypeSpec.Builder builder) {
    ArgsConstructors.addNoArgsConstructor(descriptor, builder);
    ArgsConstructors.addCanonConstructor(descriptor, builder);
    addCopyConstructor(descriptor, builder, descriptor.getArgsType().getArgsType());
    addCopyConstructor(descriptor, builder, descriptor.getModelType());
    ArgsConstructors.addGenericCopyConstructor(descriptor, builder);
  }

  private static void addRecordConstructors(TypeName argsSpecName, ModelDescriptor descriptor,
      TypeSpec.Builder builder) {
    RecordConstructors.addNoArgsConstructor(descriptor, builder, argsSpecName);
    RecordConstructors.addCanonConstructor(descriptor, builder, argsSpecName);
    addCopyConstructor(descriptor, builder, descriptor.getArgsType().getArgsType());
    addCopyConstructor(descriptor, builder, descriptor.getModelType());
    RecordConstructors.addConfigureConstructor(builder, argsSpecName);
    RecordConstructors.addGenericCopyConstructor(descriptor, builder, argsSpecName);
  }

  private static TypeName configureType(TypeName argsTypeName) {
    return ParameterizedTypeName.get(ClassName.get(Consumer.class), WildcardTypeName.supertypeOf(argsTypeName));
  }

  private static void addConfigureMethod(ModelDescriptor descriptor, TypeSpec.Builder builder, TypeName argsType) {
    MethodSpec.Builder m = MethodSpec.methodBuilder("applyConfig")//
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)//
        .returns(argsType)//
        .addParameter(argsType, "args")//
        .addParameter(configureType(argsType), "configure")//
        .addStatement("configure.accept(args)")//
        .addStatement("return args");
    descriptor.getTypeParameters().forEach(arg -> {
      m.addTypeVariable(arg);
    });
    builder.addMethod(m.build());
  }

  private static void addCopyConstructor(ModelDescriptor descriptor, TypeSpec.Builder builder, TypeName toCopy) {
    MethodSpec.Builder copyConstructorBldr = MethodSpec.constructorBuilder()//
        .addModifiers(Modifier.PUBLIC)//
        .addParameter(toCopy, "copy");

    String copyCall = descriptor.getKeys().values().stream()//
        .map(x -> "copy." + x.name() + "()")//
        .collect(Collectors.joining(",$W", "this(", ")"));
    copyConstructorBldr.addStatement(copyCall);
    builder.addMethod(copyConstructorBldr.build());
  }

  private static class ArgsConstructors {

    static void addNoArgsConstructor(ModelDescriptor descriptor, TypeSpec.Builder builder) {

      MethodSpec.Builder b = MethodSpec.constructorBuilder()//
          .addModifiers(Modifier.PUBLIC);
      Set<StaticMethodId> initArgsMethods = descriptor.getArgsType().getInitializeArgsMethods();
      if (!initArgsMethods.isEmpty()) {
        for (StaticMethodId method : initArgsMethods) {
          b.addStatement("$T.$L(this)", method.getType(), method.getName());
        }
      }

      builder.addMethod(b.build());
    }

    static void addCanonConstructor(ModelDescriptor descriptor, TypeSpec.Builder builder) {
      MethodSpec.Builder cannonConstructorBldr = MethodSpec.constructorBuilder()//
          .addModifiers(Modifier.PUBLIC);

      for (KeyDescriptor key : descriptor.getKeys().values()) {
        cannonConstructorBldr.addStatement("this.$N = $N", key.name(), key.name());
        cannonConstructorBldr.addParameter(key.type(), key.name());
      }
      builder.addMethod(cannonConstructorBldr.build());
    }

    static void addGenericCopyConstructor(ModelDescriptor descriptor, TypeSpec.Builder builder) {
      MethodSpec.Builder bldr = MethodSpec.constructorBuilder()//
          .addModifiers(Modifier.PUBLIC).addParameter(ModelOrArgs.class, "copyFrom");
      bldr.addStatement("this()");
      bldr.addStatement("this.copy(copyFrom)");
      builder.addMethod(bldr.build());
    }

  }

  private static class RecordConstructors {
    static void addNoArgsConstructor(ModelDescriptor descriptor, TypeSpec.Builder builder, TypeName argsTypeName) {
      MethodSpec.Builder b = MethodSpec.constructorBuilder()//
          .addModifiers(Modifier.PUBLIC);
      b.addStatement("this(new $T())", argsTypeName);
      builder.addMethod(b.build());
    }

    static void addCanonConstructor(ModelDescriptor descriptor, TypeSpec.Builder builder, TypeName argsType) {
      MethodSpec.Builder bldr = MethodSpec.constructorBuilder()//
          .addModifiers(Modifier.PUBLIC);
      RecordTypeDescriptor recordDesc = descriptor.getRecordType();

      if (!recordDesc.getSanatizeArgsMethods().isEmpty()) {
        String copyCall = descriptor.getKeys().values().stream()//
            .map(x -> x.name())//
            .collect(Collectors.joining(",$W", "$T _toSanatize = new $T(", ")"));
        bldr.addStatement(copyCall, argsType, argsType);

        for (StaticMethodId method : recordDesc.getSanatizeArgsMethods()) {
          bldr.addStatement("$T.$L(_toSanatize)", method.getType(), method.getName());
        }

        for (KeyDescriptor key : descriptor.getKeys().values()) {
          bldr.addStatement("this.$N = _toSanatize.$N()", key.name(), key.name());
          bldr.addParameter(key.type(), key.name());
        }
      } else {
        for (KeyDescriptor key : descriptor.getKeys().values()) {
          bldr.addStatement("this.$N = $N", key.name(), key.name());
          bldr.addParameter(key.type(), key.name());
        }
      }

      for (StaticMethodId method : recordDesc.getValidateMethods()) {
        bldr.addStatement("$T.$L(this)", method.getType(), method.getName());
      }

      builder.addMethod(bldr.build());
    }

    static void addConfigureConstructor(TypeSpec.Builder builder, TypeName argsTypeName) {
      MethodSpec method = MethodSpec.constructorBuilder()//
          .addParameter(configureType(argsTypeName), "configure")//
          .addStatement("this(applyConfig(new $T(), configure))", argsTypeName)//
          .addModifiers(Modifier.PUBLIC)//
          .build();
      builder.addMethod(method);
    }

    static void addGenericCopyConstructor(ModelDescriptor descriptor, TypeSpec.Builder builder, TypeName argsTypeName) {
      MethodSpec.Builder bldr = MethodSpec.constructorBuilder()//
          .addModifiers(Modifier.PUBLIC).addParameter(ModelOrArgs.class, "copyFrom");
      bldr.addStatement("this(new $T(copyFrom))", argsTypeName);
      builder.addMethod(bldr.build());
    }
  }

}
