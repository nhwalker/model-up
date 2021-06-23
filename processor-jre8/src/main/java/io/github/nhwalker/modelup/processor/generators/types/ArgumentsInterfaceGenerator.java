package io.github.nhwalker.modelup.processor.generators.types;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import io.github.nhwalker.modelup.processor.TypeNameUtils;
import io.github.nhwalker.modelup.processor.descriptors.KeyDescriptor;
import io.github.nhwalker.modelup.processor.descriptors.ModelDescriptor;
import io.github.nhwalker.modelup.processor.generators.methods.AnyFieldGetterMethodSpec;
import io.github.nhwalker.modelup.processor.generators.methods.AnyFieldSetterMethodSpec;
import io.github.nhwalker.modelup.processor.generators.methods.CopyMethodSpec;
import io.github.nhwalker.modelup.processor.generators.methods.FieldGetterMethodSpec;
import io.github.nhwalker.modelup.processor.generators.methods.FieldKeysMethodSpec;
import io.github.nhwalker.modelup.processor.generators.methods.FieldSetterMethodSpec;
import io.github.nhwalker.modelup.processor.generators.methods.GenericCopyMethodSpec;
import io.github.nhwalker.modelup.processor.generators.methods.HasFieldMethodSpec;

public class ArgumentsInterfaceGenerator {

  public static JavaFile create(ModelDescriptor descriptor) {
    return JavaFile
        .builder(TypeNameUtils.packageName(descriptor.getArgsType().getArgsBaseType()), createTypeSpec(descriptor))//
        .addFileComment(fileComment())//
        .build();
  }

  private static String fileComment() {
    return "Generated File - Do not edit";
  }

  private static TypeSpec createTypeSpec(ModelDescriptor descriptor) {

    TypeSpec.Builder builder = TypeSpec
        .interfaceBuilder(TypeNameUtils.rawType(descriptor.getArgsType().getArgsBaseType()));
    builder.addModifiers(Modifier.PUBLIC);
    descriptor.getTypeParameters().forEach(arg -> {
      builder.addTypeVariable(arg);
    });
    for (TypeName superType : descriptor.getArgsParentTypes()) {
      builder.addSuperinterface(superType);
    }

    for (KeyDescriptor key : descriptor.getKeys().values()) {
      builder.addMethod(FieldGetterMethodSpec.createForInterface(key).build());
      if (key.dontAllowSet()) {
        // no set
      } else {
        builder.addMethod(FieldSetterMethodSpec.createForInterface(key).build());
      }
    }
    builder.addMethod(CopyMethodSpec.createForInterface(//
        descriptor, descriptor.getArgsType().getArgsType()).build());

    builder.addMethod(CopyMethodSpec.createForInterface(//
        descriptor, descriptor.getModelType()).build());

    builder.addMethod(GenericCopyMethodSpec.createForInterface(//
        descriptor).build());

    builder.addMethod(FieldKeysMethodSpec.createForInterface(descriptor.getKeysType()).build());

    builder.addMethod(AnyFieldGetterMethodSpec.createForInterface(descriptor).build());

    builder.addMethod(HasFieldMethodSpec.createForInterface(descriptor).build());

    builder.addMethod(AnyFieldSetterMethodSpec.createForInterface(descriptor).build());

    return builder.build();

  }
}
