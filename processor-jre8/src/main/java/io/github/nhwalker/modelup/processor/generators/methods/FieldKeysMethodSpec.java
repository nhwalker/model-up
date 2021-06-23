package io.github.nhwalker.modelup.processor.generators.methods;

import java.util.Set;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.WildcardTypeName;

import io.github.nhwalker.modelup.ModelKey;
import io.github.nhwalker.modelup.processor.descriptors.KeysTypeDescriptor;

public class FieldKeysMethodSpec {

  private static MethodSpec.Builder base(KeysTypeDescriptor keysType) {
    ParameterizedTypeName fieldKeyWildcardType = //
        ParameterizedTypeName.get(ClassName.get(ModelKey.class), WildcardTypeName.subtypeOf(Object.class));
    ParameterizedTypeName fieldSetType = ParameterizedTypeName.get(ClassName.get(Set.class), fieldKeyWildcardType);

    return MethodSpec.methodBuilder("fieldKeys")//
        .addModifiers(Modifier.PUBLIC)//
        .returns(fieldSetType)//
        .addStatement("return $T.fields()", keysType.getKeysType());
  }

  public static MethodSpec.Builder createForInterface(KeysTypeDescriptor keysType) {
    MethodSpec.Builder builder = base(keysType);
    builder.addModifiers(Modifier.DEFAULT);
    return builder;
  }
  public static MethodSpec.Builder createForClass(KeysTypeDescriptor keysType) {
    MethodSpec.Builder builder = base(keysType);
    return builder;
  }

}
