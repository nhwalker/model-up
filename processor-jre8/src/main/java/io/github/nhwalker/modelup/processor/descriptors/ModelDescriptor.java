package io.github.nhwalker.modelup.processor.descriptors;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import com.google.auto.common.MoreElements;
import com.google.common.base.Predicate;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import io.github.nhwalker.modelup.ModelUp;
import io.github.nhwalker.modelup.processor.StaticMethodId;
import io.github.nhwalker.modelup.processor.TypeNameUtils;

public class ModelDescriptor {

  public static ModelDescriptor resolve(Map<ClassName, ModelDescriptor> populate, TypeElement source, Types types,
      Elements elements) {
    TypeName self = TypeName.get(source.asType());
    ClassName rawSelf = TypeNameUtils.rawType(self);
    if (populate.containsKey(rawSelf)) {
      return populate.get(rawSelf);
    }

    List<? extends TypeMirror> supers = source.getInterfaces();

    List<ModelDescriptor> parents = new ArrayList<>();
    List<TypeName> parentTypes = new ArrayList<>();
    for (TypeMirror parent : supers) {
      TypeName parentType = TypeName.get(parent);
      parentTypes.add(parentType);
      ClassName rawParentName = TypeNameUtils.rawType(parentType);
      if (!populate.containsKey(rawParentName)) {
        TypeElement parentElement = MoreElements.asType(types.asElement(parent));
        ModelDescriptor parentDescriptor = resolve(populate, parentElement, types, elements);
        parents.add(parentDescriptor);
      } else {
        parents.add(populate.get(rawParentName));
      }
    }

    ModelDescriptor result = create(source, parents, parentTypes, types, elements);
    populate.put(rawSelf, result);
    return result;
  }

  private static ModelDescriptor create(TypeElement source, List<ModelDescriptor> parents, List<TypeName> parentTypes,
      Types types, Elements elements) {

    TypeName modelType = TypeName.get(source.asType());
    Map<String, KeyDescriptor> keys = KeyDescriptor.resolve(parents, source, types, elements);
    ModelUp ann = ModelUpCapture.resolveModelUp(source);
    List<TypeVariableName> typeParameters = resolveTypeArgs(modelType);
    TypeName[] typeArgs = typeParameters.toArray(new TypeName[typeParameters.size()]);

    System.out.println("ARGS: " + ann);
    ArgsTypeDescriptor argsType = !ann.generateArgs() ? null
        : ArgsTypeDescriptor.create(source, ann, parents, typeArgs, elements);
    KeysTypeDescriptor keysType = !ann.generateKeys() ? null : KeysTypeDescriptor.create(source, ann, elements);
    RecordTypeDescriptor recordType = !ann.generateRecord() ? null
        : RecordTypeDescriptor.create(source, ann, parents, typeArgs, types, elements);
    return new ModelDescriptor(modelType, parents, parentTypes, argsType, keysType, recordType, keys, typeParameters);
  }

  private static List<TypeVariableName> resolveTypeArgs(TypeName modelType) {
    if (modelType instanceof ParameterizedTypeName) {
      ParameterizedTypeName parameterized = (ParameterizedTypeName) modelType;
      List<TypeVariableName> completeParams = new ArrayList<>();
      parameterized.typeArguments.forEach(paramType -> {
        if (paramType instanceof TypeVariableName) {
          TypeVariableName param = (TypeVariableName) paramType;
          completeParams.add(param);
        } else {
          // TODO ERROR?
        }
      });

      return Collections.unmodifiableList(completeParams);
    }
    return Collections.emptyList();
  }

  private final TypeName modelType;
  private final List<ModelDescriptor> parents;
  private final List<TypeName> parentTypes;
  private final ArgsTypeDescriptor argsType;
  private final KeysTypeDescriptor keysType;
  private final RecordTypeDescriptor recordType;
  private final Map<String, KeyDescriptor> keys;
  private final List<TypeVariableName> typeParameters;

  private ModelDescriptor(TypeName modelType, List<ModelDescriptor> parents, List<TypeName> parentTypes,
      ArgsTypeDescriptor argsType, KeysTypeDescriptor keysType, RecordTypeDescriptor recordType,
      Map<String, KeyDescriptor> keys, List<TypeVariableName> typeParameters) {
    this.modelType = modelType;
    this.parents = parents;
    this.parentTypes = parentTypes;
    this.argsType = argsType;
    this.keysType = keysType;
    this.recordType = recordType;
    this.keys = keys;
    this.typeParameters = typeParameters;

  }

  public List<TypeVariableName> getTypeParameters() {
    return typeParameters;
  }

  public List<TypeName> getParentTypes() {
    return parentTypes;
  }

  public List<TypeName> getArgsParentTypes() {
    List<TypeName> list = new ArrayList<>();
    for (int i = 0; i < parentTypes.size(); i++) {
      TypeName parentType = parentTypes.get(i);
      ModelDescriptor parentDescriptor = parents.get(i);
      if (parentDescriptor.hasArgsType()) {
        ClassName rawParentArgsType = TypeNameUtils.rawType(parentDescriptor.getArgsType().getArgsType());
        if (parentType instanceof ParameterizedTypeName) {
          ParameterizedTypeName asParam = (ParameterizedTypeName) parentType;
          list.add(ParameterizedTypeName.get(rawParentArgsType, asParam.typeArguments.toArray(new TypeName[0])));
        } else {
          list.add(rawParentArgsType);
        }
      }
    }
    return list;
  }

  public Map<String, KeyDescriptor> getKeys() {
    return keys;
  }

  public TypeName getModelType() {
    return modelType;
  }

  public List<ModelDescriptor> getParents() {
    return parents;
  }

  public ArgsTypeDescriptor getArgsType() {
    return argsType;
  }

  public boolean hasArgsType() {
    return argsType != null;
  }

  public KeysTypeDescriptor getKeysType() {
    return keysType;
  }

  public boolean hasKeysType() {
    return argsType != null;
  }

  public RecordTypeDescriptor getRecordType() {
    return recordType;
  }

  public boolean hasRecordType() {
    return argsType != null;
  }

  static <A extends Annotation> Set<StaticMethodId> findStaticMethod(TypeElement e, Class<A> annotationType,
      Predicate<A> inherit) {
    LinkedHashSet<StaticMethodId> list = new LinkedHashSet<>();
    List<? extends Element> enclosed = e.getEnclosedElements();
    for (Element inner : enclosed) {
      if (MoreElements.isType(inner)) {
        TypeElement innerType = MoreElements.asType(inner);
        List<? extends Element> innerEnclosed = innerType.getEnclosedElements();
        for (Element innerInner : innerEnclosed) {
          if (innerInner.getKind() == ElementKind.METHOD) {
            ExecutableElement method = MoreElements.asExecutable(innerInner);
            A ann = method.getAnnotation(annotationType);
            if (ann != null) {
              list.add(new StaticMethodId(TypeNameUtils.rawType(TypeName.get(e.asType())),
                  method.getSimpleName().toString(), inherit.apply(ann)));
            }
          }
        }

      } else if (inner.getKind() == ElementKind.METHOD) {
        ExecutableElement method = MoreElements.asExecutable(inner);
        A ann = method.getAnnotation(annotationType);
        if (ann != null) {
          list.add(new StaticMethodId(TypeNameUtils.rawType(TypeName.get(e.asType())),
              method.getSimpleName().toString(), inherit.apply(ann)));
        }
      }
    }
    return list;
  }
}
