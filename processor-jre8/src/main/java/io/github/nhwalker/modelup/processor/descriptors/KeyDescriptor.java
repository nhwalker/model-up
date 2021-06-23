package io.github.nhwalker.modelup.processor.descriptors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import io.github.nhwalker.modelup.ModelUp;
import io.github.nhwalker.modelup.processor.TypeNameUtils;

public class KeyDescriptor {

  public static Map<String, KeyDescriptor> resolve(List<ModelDescriptor> parents,
      TypeElement source, Types types, Elements elements) {
    ListMultimap<String, KeyDescriptor> parentKeys = ArrayListMultimap.create();
    for(ModelDescriptor parent : parents) {
      parentKeys.putAll(Multimaps.forMap(parent.getKeys()));
    }
    
    Map<String, KeyDescriptor> myKeys = new LinkedHashMap<>();
    Set<ExecutableElement> methods = MoreElements.getAllMethods(source, types, elements);
    for (ExecutableElement method : methods) {
      if (isProperty(method)) {
        String name = method.getSimpleName().toString();
        KeyDescriptor key = create(source, method, types, elements, parentKeys.get(name));
        myKeys.put(name, key);
      }
    }
    Map<String, KeyDescriptor> immutableKeysView = Collections.unmodifiableMap(myKeys);
    return immutableKeysView;
  }

  private static final Set<String> IGNORE = new HashSet<>(Arrays.asList("getClass", "hashCode", "toString", "clone"));
  private static final Set<String> IGNORE_MODEL = new HashSet<>(Arrays.asList("fieldKeys"));

  private static boolean isProperty(ExecutableElement method) {
    String name = method.getSimpleName().toString();
    return !method.getModifiers().contains(Modifier.STATIC) //
        && !IGNORE.contains(name) //
        && method.getParameters().isEmpty()//
        && !IGNORE_MODEL.contains(name)//
        && method.getReturnType().getKind() != TypeKind.VOID;
  }

  private static KeyDescriptor create(TypeElement source, ExecutableElement method, Types types, Elements elements,
      List<KeyDescriptor> overrides) {
    String name = method.getSimpleName().toString();
    DeclaredType declaredType = MoreTypes.asDeclared(source.asType());
    TypeMirror returnTypeMirror = MoreTypes.asExecutable(types.asMemberOf(declaredType, method)).getReturnType();
    TypeName valueType = TypeName.get(returnTypeMirror);

    String description = processDoc(elements.getDocComment(method));
    if (description.isEmpty()) {
      description = overrides.stream()//
          .map(KeyDescriptor::description)//
          .filter(x -> !x.isEmpty())//
          .findFirst()//
          .orElse("");
    }
    boolean needsSurpressUncheckedForCast = MoreTypes.isConversionFromObjectUnchecked(returnTypeMirror);

    boolean defaulted = method.isDefault();
    boolean dontAllowSet = defaulted || dontAllowSet(method);

    return new KeyDescriptor(name, valueType, description, overrides, needsSurpressUncheckedForCast, defaulted,
        dontAllowSet);
  }

  private static boolean dontAllowSet(ExecutableElement e) {
    return e.getAnnotation(ModelUp.DontSet.class) != null;
  }

  private static String processDoc(String doc) {
    if (doc == null) {
      return "";
    }
    String generalDoc;
    String returnDoc;
    String splitDelim = "@return ";
    int splitIdx = doc.lastIndexOf("@return ");
    if (splitIdx == -1) {
      generalDoc = doc.trim();
      returnDoc = "";
    } else {
      generalDoc = doc.substring(0, splitIdx).trim();
      returnDoc = doc.substring(splitIdx + splitDelim.length()).trim();
    }
    if (!generalDoc.isEmpty()) {
      return generalDoc;
    } else if (!returnDoc.isEmpty()) {
      return returnDoc;
    }
    return "";
  }

  private final String name;
  private final TypeName type;
  private final String description;
  private final List<KeyDescriptor> overrides;
  private final boolean needsSupressUncheckedForCast;
  private final boolean defaulted;
  private final boolean dontAllowSet;
  private List<TypeVariableName> typeParams = null;

  public KeyDescriptor(String name, TypeName type, String description, List<KeyDescriptor> overrides,
      boolean needsSupressUncheckedForCast, boolean defaulted, boolean dontAllowSet) {
    this.name = name;
    this.type = type;
    this.description = description;
    this.overrides = overrides;
    this.needsSupressUncheckedForCast = needsSupressUncheckedForCast;

    this.defaulted = defaulted;
    this.dontAllowSet = dontAllowSet;
  }

  public String name() {
    return name;
  }

  public TypeName type() {
    return type;
  }

  public String description() {
    return description;
  }

  public List<KeyDescriptor> overrides() {
    return overrides;
  }
  public boolean overridesSet(){
    // TODO - This probably isn't quite accurate
    return overrides.stream().anyMatch(k->!k.dontAllowSet());
  }

  public ClassName rawType() {
    return TypeNameUtils.rawType(type);
  }

  public List<TypeVariableName> typeParameters() {
    if (typeParams == null) {
      List<TypeVariableName> vars = new ArrayList<>();
      if (type instanceof ParameterizedTypeName) {
        ParameterizedTypeName t = (ParameterizedTypeName) type;
        for (TypeName arg : t.typeArguments) {
          if (arg instanceof TypeVariableName) {
            vars.add((TypeVariableName) arg);
          }
        }
      }
      typeParams = Collections.unmodifiableList(vars);
    }
    return typeParams;
  }

  public boolean hasTypeArguments() {
    return type instanceof ParameterizedTypeName;
  }

  public boolean isPrimitive() {
    return type.isPrimitive();
  }

  public TypeName boxedType() {
    return isPrimitive() ? type.box() : type;
  }

  public TypeName keyLiteralType() {
    // Unbox Generic Types
    TypeName effectiveTypeName = type;
    if (type instanceof TypeVariableName) {
      TypeVariableName typeVar = (TypeVariableName) type;
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

  public boolean needsSupressUncheckedForCast() {
    return needsSupressUncheckedForCast;
  }

  public boolean isDefaulted() {
    return defaulted;
  }

  public boolean dontAllowSet() {
    return dontAllowSet;
  }
  
}
