package io.github.nhwalker.modelup.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import io.github.nhwalker.modelup.ModelKey;

public class KeysGenerator extends AbstractModelKeyBasedGenerator {

  @Override
  protected JavaFile doCreate() {
    return JavaFile.builder(TypeNameUtils.packageName(getDefinition().keysType()), createTypeSpec())//
        .addFileComment(fileComment())//
        .build();
  }

  private String fileComment() {
    return "";// TODO
  }

  private TypeSpec createTypeSpec() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(TypeNameUtils.rawType(getDefinition().keysType()));
    builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    List<FieldSpec> fieldSpecs = new ArrayList<>();
    for (ModelKeyDefinition key : getKeys()) {
      FieldSpec fieldSpec = createFieldSpec(key);
      fieldSpecs.add(fieldSpec);
      builder.addField(fieldSpec);
    }

    addFieldList(builder, fieldSpecs);

    builder.addMethod(hiddenConstructor());
    return builder.build();

  }

  private MethodSpec hiddenConstructor() {
    return MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE)
        .addStatement("throw new $T($S)", Error.class, "Never Instantiate").build();
  }

  protected FieldSpec createFieldSpec(ModelKeyDefinition field) {

    // Input
    String nameUpperCase = field.getNameUpperCaseFormat();
    String name = field.getName();
    TypeName effectiveTypeName = field.getEffectiveKeyType();
    String fieldDescription = field.getFieldDescription();
    TypeName fieldBelongsTo = field.getBelongsTo();

    // Field Definition
    ParameterizedTypeName fieldTypeName = ParameterizedTypeName.get(ClassName.get(ModelKey.class), effectiveTypeName);
    FieldSpec.Builder builder = FieldSpec.builder(fieldTypeName, nameUpperCase, //
        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

    // Field Initialization
    if (effectiveTypeName instanceof ParameterizedTypeName) {
      builder.initializer("$T.of($S, ($T)$T.class)", ModelKey.class, name, Class.class, rawType(effectiveTypeName));
      builder.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
          .addMember("value", "{ $S, $S }", "unchecked", "rawtypes").build());
    } else {
      builder.initializer("$T.of($S, $T.class)", ModelKey.class, name, effectiveTypeName);
    }

    // Field JavaDoc
    if (!fieldDescription.isEmpty()) {
      builder.addJavadoc(fieldDescription);
      builder.addJavadoc("\n\n");
    }
    builder.addJavadoc("@see $T#$L()", rawType(fieldBelongsTo), name);

    // Return
    return builder.build();

  }

  private void addFieldList(TypeSpec.Builder typeSpec, List<FieldSpec> fieldSpecs) {
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
