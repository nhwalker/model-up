package io.github.nhwalker.modelup.processor.descriptors;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import io.github.nhwalker.modelup.ModelUp;
import io.github.nhwalker.modelup.processor.StaticMethodId;
import io.github.nhwalker.modelup.processor.TypeNameUtils;

public class ArgsTypeDescriptor {

  public static ArgsTypeDescriptor create(TypeElement source, ModelUp ann, List<ModelDescriptor> parents,
      TypeName[] typeParameters, Elements elements) {
    String packageName = elements.getPackageOf(source).getQualifiedName().toString();
    String baseName = source.getSimpleName().toString();

    ClassName rawArgsName;
    ClassName rawArgsNameBase;
    if (ann.argsExtension().isEmpty()) {
      rawArgsName = getRawArgsName(packageName, baseName, ann, false);
      rawArgsNameBase = rawArgsName;
    } else {
      rawArgsNameBase = getRawArgsName(packageName, baseName, ann, true);
      rawArgsName = parseClassName(packageName, baseName, ann.argsExtension(), elements);
    }

    TypeName argsName;
    TypeName argsNameBase;
    if (typeParameters.length != 0) {
      argsName = ParameterizedTypeName.get(rawArgsName, typeParameters);
      argsNameBase = ParameterizedTypeName.get(rawArgsNameBase, typeParameters);
    } else {
      argsName = rawArgsName;
      argsNameBase = rawArgsNameBase;
    }

    Set<StaticMethodId> initMethods = ModelDescriptor.findStaticMethod(source, ModelUp.InitialArgs.class,
        ModelUp.InitialArgs::inherit);
    if (initMethods.stream().anyMatch(x -> !x.isInherit())) {
      // don't add parents
    } else {
      LinkedHashSet<StaticMethodId> allInitMethods = parents.stream()
          .flatMap(x -> x.hasArgsType() ? x.getArgsType().getInitializeArgsMethods().stream() : Stream.empty())
          .collect(Collectors.toCollection(LinkedHashSet::new));
      allInitMethods.addAll(initMethods);
      initMethods = allInitMethods;
    }

    return new ArgsTypeDescriptor(argsNameBase, argsName, Collections.unmodifiableSet(initMethods));
  }

  private static ClassName getRawArgsName(String basePackageName, String baseName, ModelUp modelUpAnn, boolean base) {
    String packageName = basePackageName;
    if (!modelUpAnn.argsPackageName().isEmpty()) {
      packageName = modelUpAnn.argsPackageName();
    }
    String name;
    if (!modelUpAnn.argsTypeName().isEmpty()) {
      name = modelUpAnn.argsTypeName();
    } else {
      name = baseName + "Args";
      if (base) {
        name += "Base";
      }
    }
    return ClassName.get(packageName, name);
  }

  private static ClassName parseClassName(String packageName, String baseName, String toParse, Elements elements) {
    TypeElement found;
    found = elements.getTypeElement(packageName + "." + baseName + "." + toParse);
    if (found == null) {
      found = elements.getTypeElement(packageName + "." + toParse);
      if (found == null) {
        found = elements.getTypeElement(toParse);
      }
    }
    Objects.requireNonNull(found);
    return TypeNameUtils.rawType(TypeName.get(found.asType()));

  }

  private final TypeName argsBaseType;
  private final TypeName argsType;
  private final Set<StaticMethodId> initializeArgsMethods;

  public ArgsTypeDescriptor(TypeName argsBaseType, TypeName argsType, Set<StaticMethodId> initializeArgsMethods) {
    this.argsBaseType = argsBaseType;
    this.argsType = argsType;
    this.initializeArgsMethods = initializeArgsMethods;
  }

  public TypeName getArgsBaseType() {
    return argsBaseType;
  }

  public TypeName getArgsType() {
    return argsType;
  }

  public Set<StaticMethodId> getInitializeArgsMethods() {
    return initializeArgsMethods;
  }
}