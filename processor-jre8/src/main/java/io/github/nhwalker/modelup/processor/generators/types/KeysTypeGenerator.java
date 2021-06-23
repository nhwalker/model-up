package io.github.nhwalker.modelup.processor.generators.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;

import io.github.nhwalker.modelup.ModelKey;
import io.github.nhwalker.modelup.processor.TypeNameUtils;
import io.github.nhwalker.modelup.processor.descriptors.KeyDescriptor;
import io.github.nhwalker.modelup.processor.descriptors.ModelDescriptor;

public class KeysTypeGenerator {

  public static JavaFile create(ModelDescriptor descriptor) {

    return JavaFile.builder(TypeNameUtils.packageName(descriptor.getKeysType().getKeysType()), //
        createTypeSpec(descriptor))//
        .addFileComment(fileComment())//
        .build();

  }

  private static String fileComment() {
    return "Generated File - Do not edit";
  }

  private static TypeSpec createTypeSpec(ModelDescriptor descriptor) {
    TypeSpec.Builder builder = TypeSpec.classBuilder(TypeNameUtils.rawType(descriptor.getKeysType().getKeysType()));
    builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    List<FieldSpec> fieldSpecs = new ArrayList<>();
    for (KeyDescriptor key : descriptor.getKeys().values()) {
      FieldSpec fieldSpec = createFieldSpec(descriptor, key);
      fieldSpecs.add(fieldSpec);
      builder.addField(fieldSpec);
    }

    addFieldList(builder, fieldSpecs);

    builder.addMethod(hiddenConstructor());
    builder.addJavadoc("Defines all property keys of {@link $T}", descriptor.getModelType());
    return builder.build();
  }

  private static MethodSpec hiddenConstructor() {
    return MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE)
        .addStatement("throw new $T($S)", Error.class, "Never Instantiate").build();
  }

  private static FieldSpec createFieldSpec(ModelDescriptor descriptor, KeyDescriptor field) {

    // Input
    String nameUpperCase = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, field.name());
    String name = field.name();
    TypeName unwrappedValueType = unwrappedValueType(field.type());
    String fieldDescription = field.description();
    TypeName fieldBelongsTo = descriptor.getModelType();

    // Field Definition
    ParameterizedTypeName fieldTypeName = ParameterizedTypeName.get(ClassName.get(ModelKey.class), unwrappedValueType);
    FieldSpec.Builder builder = FieldSpec.builder(fieldTypeName, nameUpperCase, //
        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

    // Field Initialization
    if (unwrappedValueType instanceof ParameterizedTypeName) {
      builder.initializer("$T.of($S, ($T)$T.class)", ModelKey.class, name, Class.class,
          TypeNameUtils.rawType(unwrappedValueType));
      builder.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
          .addMember("value", "{ $S, $S }", "unchecked", "rawtypes").build());
    } else {
      builder.initializer("$T.of($S, $T.class)", ModelKey.class, name, unwrappedValueType);
    }

    // Field JavaDoc
    if (!fieldDescription.isEmpty()) {
      builder.addJavadoc(fieldDescription);
      builder.addJavadoc("\n\n");
    }
    builder.addJavadoc("@see $T#$L()", TypeNameUtils.rawType(fieldBelongsTo), name);

    // Return
    return builder.build();

  }

  private static TypeName unwrappedValueType(TypeName valueType) {
    // Unbox Generic Types
    TypeName effectiveTypeName = valueType;
    if (valueType instanceof TypeVariableName) {
      TypeVariableName typeVar = (TypeVariableName) valueType;
      if (typeVar.bounds.size() == 1) {
        effectiveTypeName = typeVar.bounds.get(0);
      } else {
        effectiveTypeName = ClassName.get(Object.class);
      }
    }

    // Box primitives
    effectiveTypeName = effectiveTypeName.box();
    return effectiveTypeName;
  }

  private static void addFieldList(TypeSpec.Builder typeSpec, List<FieldSpec> fieldSpecs) {
    ParameterizedTypeName fieldKeyWildcardType = //
        ParameterizedTypeName.get(ClassName.get(ModelKey.class), WildcardTypeName.subtypeOf(Object.class));
    ParameterizedTypeName fieldSetType = ParameterizedTypeName.get(ClassName.get(Set.class), fieldKeyWildcardType);
    String name = "ALL_FIELDS";

    CodeBlock.Builder initBuilder = CodeBlock.builder();
    initBuilder.add("$[$T.unmodifiableSet(new $T<>($T.asList(", Collections.class, LinkedHashSet.class, Arrays.class);
    boolean notFirst = false;
    for (FieldSpec spec : fieldSpecs) {
      if (notFirst) {
        initBuilder.add(",$W");
      } else {
        notFirst = true;
      }
      initBuilder.add("$N", spec);
    }
    initBuilder.add(")))$]");

    FieldSpec fieldSpec = FieldSpec.builder(fieldSetType, name, //
        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)//
        .initializer(initBuilder.build())//
        .build();

    MethodSpec methodSpec = MethodSpec.methodBuilder("fields")//
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)//
        .returns(fieldSetType)//
        .addStatement("return $N", fieldSpec)//
        .addJavadoc("@return immutable set of all fields in this field list (iterates in definition order)")//
        .build();

    typeSpec.addField(fieldSpec);
    typeSpec.addMethod(methodSpec);
  }
}
