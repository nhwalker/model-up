package io.github.nhwalker.modelup.processor.descriptors;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import io.github.nhwalker.modelup.ModelUp;
import io.github.nhwalker.modelup.processor.StaticMethodId;

public class RecordTypeDescriptor {
  private final TypeName recordType;
  private final boolean memorizeHash;
  private final boolean memorizeToString;
  private final Set<StaticMethodId> validateMethods;
  private final Set<StaticMethodId> sanatizeArgsMethods;
  private final boolean defaultConstructor;

  public RecordTypeDescriptor(TypeName recordType, boolean memorizeHash, boolean memorizeToString,
      Set<StaticMethodId> validateMethods, Set<StaticMethodId> sanatizeArgsMethods, boolean defaultConstructor) {
    this.recordType = recordType;
    this.memorizeHash = memorizeHash;
    this.memorizeToString = memorizeToString;
    this.validateMethods = validateMethods;
    this.sanatizeArgsMethods = sanatizeArgsMethods;
    this.defaultConstructor = defaultConstructor;
  }

  public static RecordTypeDescriptor create(TypeElement source, ModelUp ann, List<ModelDescriptor> parents,
      TypeName[] typeArgs, Types types, Elements elements) {
    boolean memorizeHash = ann.memorizeHash();
    boolean memorizeToString = ann.memorizeToString();
    boolean defaultConstructor = ann.defaultConstructor();

    String packageName = elements.getPackageOf(source).getQualifiedName().toString();
    String baseName = source.getSimpleName().toString();
    ClassName rawRecordName = getRawRecordName(packageName, baseName, ann);
    TypeName recordType;
    if (typeArgs.length != 0) {
      recordType = ParameterizedTypeName.get(rawRecordName, typeArgs);
    } else {
      recordType = rawRecordName;
    }

    Set<StaticMethodId> validateMethods = ModelDescriptor.findStaticMethod(source, ModelUp.Validate.class,
        ModelUp.Validate::inherit);
    if (validateMethods.stream().anyMatch(x -> !x.isInherit())) {
      // don't add parents
    } else {
      LinkedHashSet<StaticMethodId> allValidateMethods = parents.stream()
          .flatMap(x -> x.hasRecordType() ? x.getRecordType().getValidateMethods().stream() : Stream.empty())
          .collect(Collectors.toCollection(LinkedHashSet::new));
      allValidateMethods.addAll(validateMethods);
      validateMethods = allValidateMethods;
    }

    Set<StaticMethodId> sanatizeArgsMethods = ModelDescriptor.findStaticMethod(source, ModelUp.Sanatize.class,
        ModelUp.Sanatize::inherit);
    if (sanatizeArgsMethods.stream().anyMatch(x -> !x.isInherit())) {
      // don't add parents
    } else {
      LinkedHashSet<StaticMethodId> allSanatizeMethods = parents.stream()
          .flatMap(x -> x.hasRecordType() ? x.getRecordType().getSanatizeArgsMethods().stream() : Stream.empty())
          .collect(Collectors.toCollection(LinkedHashSet::new));
      allSanatizeMethods.addAll(sanatizeArgsMethods);
      sanatizeArgsMethods = allSanatizeMethods;
    }

    return new RecordTypeDescriptor(recordType, memorizeHash, memorizeToString, validateMethods, sanatizeArgsMethods,
        defaultConstructor);
  }

  private static ClassName getRawRecordName(String basePackageName, String baseName, ModelUp modelUpAnn) {
    String packageName = basePackageName;
    if (!modelUpAnn.recordPackageName().isEmpty()) {
      packageName = modelUpAnn.recordPackageName();
    }
    String name;
    if (!modelUpAnn.recordTypeName().isEmpty()) {
      name = modelUpAnn.recordTypeName();
    } else {
      name = baseName + "Record";
    }
    return ClassName.get(packageName, name);
  }

  public TypeName getRecordType() {
    return recordType;
  }

  public boolean isMemorizeHash() {
    return memorizeHash;
  }

  public boolean isMemorizeToString() {
    return memorizeToString;
  }

  public Set<StaticMethodId> getValidateMethods() {
    return validateMethods;
  }

  public Set<StaticMethodId> getSanatizeArgsMethods() {
    return sanatizeArgsMethods;
  }

  public boolean isDefaultConstructor() {
    return defaultConstructor;
  }

}