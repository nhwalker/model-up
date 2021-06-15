package io.github.nhwalker.modelup.processor;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import io.github.nhwalker.modelup.Model;
import io.github.nhwalker.modelup.ModelUp;
import io.github.nhwalker.modelup.ModelWithArgs;

public class ModelUpProcessor extends AbstractProcessor {
  private static final Set<String> IGNORE = new HashSet<>(Arrays.asList("getClass", "hashCode", "toString", "clone"));

  private static final Set<String> IGNORE_MODEL = new HashSet<>(Arrays.asList("fieldKeys"));

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton(ModelUp.class.getName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_8;
  }

  private boolean testIsInstance(TypeMirror candidate, Class<?> isA) {
    TypeElement interfaceType = processingEnv.getElementUtils().getTypeElement(isA.getCanonicalName());
    if (interfaceType != null) {
      return processingEnv.getTypeUtils().isAssignable(candidate, interfaceType.asType());
    }
    return false;
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(ModelUp.class);
    Map<ClassName, ModelUpTypeDefinition> cache = new LinkedHashMap<>();

    for (Element e : elements) {
      if (e instanceof TypeElement) {
        TypeElement typeElement = (TypeElement) e;
        ModelUpTypeDefinition typeDef = getOrCreate(cache, typeElement);
        if (typeDef.generateKeys()) {
          runKeysGenerator(typeDef, roundEnv, typeElement);
        }
        if (typeDef.generateArgs()) {
          runArgsInterfaceGenerator(typeDef, roundEnv, typeElement);
        }
        if (typeDef.generateRecord()) {
          runRecordGenerator(typeDef, roundEnv, typeElement);
        }
      }
    }
    return false;
  }

  private void runKeysGenerator(ModelUpTypeDefinition typeDef, RoundEnvironment roundEnv, Element e) {
    KeysGenerator gen = new KeysGenerator();
    gen.setDefinition(typeDef);
    JavaFile file = gen.create();
    try {
      file.writeTo(processingEnv.getFiler());
    } catch (IOException e1) {
      e1.printStackTrace();
      processingEnv.getMessager().printMessage(Kind.ERROR, "Failed to write keys file", e);
    }
  }

  private void runArgsInterfaceGenerator(ModelUpTypeDefinition typeDef, RoundEnvironment roundEnv, Element e) {
    ArgumentsInterfaceGenerator gen = new ArgumentsInterfaceGenerator();
    gen.setDefinition(typeDef);
    JavaFile file = gen.create();
    try {
      file.writeTo(processingEnv.getFiler());
    } catch (IOException e1) {
      e1.printStackTrace();
      processingEnv.getMessager().printMessage(Kind.ERROR, "Failed to write keys file");
    }
  }

  private void runRecordGenerator(ModelUpTypeDefinition typeDef, RoundEnvironment roundEnv, Element e) {
    RecordGenerator gen = new RecordGenerator();
    gen.setDefinition(typeDef);
    JavaFile file = gen.create();
    try {
      file.writeTo(processingEnv.getFiler());
    } catch (IOException e1) {
      e1.printStackTrace();
      processingEnv.getMessager().printMessage(Kind.ERROR, "Failed to write keys file");
    }
  }

  private List<ModelKeyDefinition> findAllKeys(TypeElement e, boolean isAModel) {
    ImmutableSet<ExecutableElement> methods = MoreElements.getAllMethods(e, processingEnv.getTypeUtils(),
        processingEnv.getElementUtils());

    List<ModelKeyDefinition> fields = new ArrayList<>();
    for (Element member : methods) {
      if (member instanceof ExecutableElement) {
        ExecutableElement method = (ExecutableElement) member;
        String name = member.getSimpleName().toString();
        if (!method.getModifiers().contains(Modifier.STATIC) && !IGNORE.contains(name)
            && method.getParameters().isEmpty() && (!isAModel || !IGNORE_MODEL.contains(name))) {
          TypeMirror returnType = method.getReturnType();
          if (returnType.getKind() != TypeKind.VOID) {
            String comment = processingEnv.getElementUtils().getDocComment(method);
            String doc = processDoc(comment);

            DeclaredType declaredType = MoreTypes.asDeclared(e.asType());

            TypeName returnTypeName = TypeName.get(returnType);
            TypeMirror methodMirror = processingEnv.getTypeUtils().asMemberOf(declaredType, method);
            TypeName effectiveType = TypeName.get(MoreTypes.asExecutable(methodMirror).getReturnType());
            TypeName effectiveKeyType = effectiveType(effectiveType);

            fields.add(new ModelKeyDefinition(name, returnTypeName, effectiveType, effectiveKeyType,
                TypeName.get(e.asType()), doc));
          }
        }
      }
    }
    return fields;
  }

  private static TypeName effectiveType(TypeName valueType) {
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

  private ModelUpTypeDefinition getOrCreate(Map<ClassName, ModelUpTypeDefinition> cache, TypeElement type) {
    TypeName modelType = TypeName.get(type.asType());
    ClassName rawName = TypeNameUtils.rawType(modelType);
    ModelUpTypeDefinition def = cache.get(rawName);
    if (def == null) {
      def = createModelUpTypeDef(cache, type);
      cache.put(rawName, def);
    }
    return def;
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

  private static ClassName getRawKeysName(String basePackageName, String baseName, ModelUp modelUpAnn) {
    String packageName = basePackageName;
    if (!modelUpAnn.keysPackageName().isEmpty()) {
      packageName = modelUpAnn.keysPackageName();
    }
    String name;
    if (!modelUpAnn.keysTypeName().isEmpty()) {
      name = modelUpAnn.keysTypeName();
    } else {
      name = baseName + "Keys";
    }
    return ClassName.get(packageName, name);
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

  private ClassName parseClassName(String packageName, String baseName, String toParse) {
    TypeElement found;
    found = processingEnv.getElementUtils().getTypeElement(packageName + "." + baseName + "." + toParse);
    if (found == null) {
      found = processingEnv.getElementUtils().getTypeElement(packageName + "." + toParse);
      if (found == null) {
        found = processingEnv.getElementUtils().getTypeElement(toParse);
      }
    }
    Objects.requireNonNull(found);
    return TypeNameUtils.rawType(TypeName.get(found.asType()));

  }

  private ModelUpTypeDefinition createModelUpTypeDef(Map<ClassName, ModelUpTypeDefinition> cache, TypeElement type) {
    ModelUp modelUpAnn = Objects.requireNonNull(type.getAnnotation(ModelUp.class));

    ModelUpTypeDefinition def = new ModelUpTypeDefinition();
    String packageName = processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
    String baseName = type.getSimpleName().toString();
    TypeName modelType = TypeName.get(type.asType());
    def.modelType(modelType);

    ClassName rawArgsName;
    ClassName rawArgsNameBase;
    if (modelUpAnn.argsExtension().isEmpty()) {
      rawArgsName = getRawArgsName(packageName, baseName, modelUpAnn, false);
      rawArgsNameBase = rawArgsName;
    } else {
      rawArgsNameBase = getRawArgsName(packageName, baseName, modelUpAnn, true);
      rawArgsName = parseClassName(packageName, baseName, modelUpAnn.argsExtension());
    }

    ClassName rawKeysName = getRawKeysName(packageName, baseName, modelUpAnn);
    ClassName rawRecordName = getRawRecordName(packageName, baseName, modelUpAnn);
    def.generateArgs(modelUpAnn.generateArgs());
    def.generateKeys(modelUpAnn.generateKeys());
    def.generateRecord(modelUpAnn.generateRecord());
    def.memorizeHash(modelUpAnn.memorizeHash());
    def.memorizeToString(modelUpAnn.memorizeToString());
    def.keysType(rawKeysName);

    if (modelType instanceof ParameterizedTypeName) {
      ParameterizedTypeName parameterized = (ParameterizedTypeName) modelType;
      List<TypeVariableName> completeParams = new ArrayList<>();
      parameterized.typeArguments.forEach(paramType -> {
        if (paramType instanceof TypeVariableName) {
          TypeVariableName param = (TypeVariableName) paramType;
          completeParams.add(param);
        } else {
          // TODO ERROR
        }
      });
      def.typeParameters(Collections.unmodifiableList(completeParams));

      TypeName[] args = completeParams.toArray(new TypeName[0]);
      def.argsType(ParameterizedTypeName.get(rawArgsName, args));
      def.argsBaseType(ParameterizedTypeName.get(rawArgsNameBase, args));
      def.recordType(ParameterizedTypeName.get(rawRecordName, args));

    } else {
      def.argsType(rawArgsName);
      def.argsBaseType(rawArgsNameBase);
      def.recordType(rawRecordName);
      def.typeParameters(Collections.emptyList());
    }

    ArrayList<StaticMethodId> initializeArgsMethods = new ArrayList<>();
    ArrayList<StaticMethodId> sanatizeArgsMethods = new ArrayList<>();
    ArrayList<StaticMethodId> validateMethods = new ArrayList<>();

    ArrayList<TypeName> modelExtends = new ArrayList<>();
    ArrayList<TypeName> argsExtends = new ArrayList<>();
    for (TypeMirror parent : type.getInterfaces()) {
      ModelUpTypeDefinition parentDefinition = findModelUpDef(cache, parent);
      if (parentDefinition != null) {
        TypeName parentType = TypeName.get(parent);
        modelExtends.add(parentType);
        ClassName rawParentArgsType = TypeNameUtils.rawType(parentDefinition.argsType());
        if (parentType instanceof ParameterizedTypeName) {
          ParameterizedTypeName asParam = (ParameterizedTypeName) parentType;
          argsExtends.add(ParameterizedTypeName.get(rawParentArgsType, asParam.typeArguments.toArray(new TypeName[0])));
        } else {
          argsExtends.add(rawParentArgsType);
        }

        initializeArgsMethods.addAll(parentDefinition.initializeArgsMethods());
        sanatizeArgsMethods.addAll(parentDefinition.sanatizeArgsMethods());
        validateMethods.addAll(parentDefinition.validateMethods());
      }
    }

    StaticMethodId initMethod = findStaticMethod(type, ModelUp.InitialArgs.class, ModelUp.InitialArgs::inherit);
    if (initMethod != null) {
      if (!initMethod.isInherit()) {
        initializeArgsMethods.clear();
      }
      initializeArgsMethods.add(initMethod);
    }

    StaticMethodId sanatizeMethod = findStaticMethod(type, ModelUp.Sanatize.class, ModelUp.Sanatize::inherit);
    if (sanatizeMethod != null) {
      if (!sanatizeMethod.isInherit()) {
        sanatizeArgsMethods.clear();
      }
      sanatizeArgsMethods.add(sanatizeMethod);
    }

    StaticMethodId validateMethod = findStaticMethod(type, ModelUp.Validate.class, ModelUp.Validate::inherit);
    if (validateMethod != null) {
      if (!validateMethod.isInherit()) {
        validateMethods.clear();
      }
      validateMethods.add(validateMethod);
    }

    def.setAModel(testIsInstance(type.asType(), Model.class));
    def.setAModelWithArgs(testIsInstance(type.asType(), ModelWithArgs.class));

    def.argsTypeExtends(argsExtends);
    def.modelExtends(modelExtends);
    def.keys(findAllKeys(type, def.isAModel()));
    def.initializeArgsMethods(initializeArgsMethods);
    def.sanatizeArgsMethods(sanatizeArgsMethods);
    def.validateMethods(validateMethods);
    def.defaultConstructor(modelUpAnn.defaultConstructor());

    return def;
  }

  private <A extends Annotation> StaticMethodId findStaticMethod(TypeElement e, Class<A> annotationType,
      Predicate<A> inherit) {
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
              return new StaticMethodId(TypeNameUtils.rawType(TypeName.get(e.asType())),
                  method.getSimpleName().toString(), inherit.apply(ann));
            }
          }
        }

      } else if (inner.getKind() == ElementKind.METHOD) {
        ExecutableElement method = MoreElements.asExecutable(inner);
        A ann = method.getAnnotation(annotationType);
        if (ann != null) {
          return new StaticMethodId(TypeNameUtils.rawType(TypeName.get(e.asType())), method.getSimpleName().toString(),
              inherit.apply(ann));
        }
      }
    }
    return null;
  }

  private ModelUpTypeDefinition findModelUpDef(Map<ClassName, ModelUpTypeDefinition> cache, TypeMirror parent) {
    Element parentElement = processingEnv.getTypeUtils().asElement(parent);
    if (parentElement instanceof TypeElement) {
      ModelUp modelUpAnn = parentElement.getAnnotation(ModelUp.class);
      if (modelUpAnn != null) {
        return getOrCreate(cache, (TypeElement) parentElement);
      }
    }
    return null;
  }

//  private ArrayList<TypeVariableName> removeModelWithArgsParams(List<TypeVariableName> parameters, TypeElement type) {
//    LinkedHashMap<String, TypeVariableName> paramMap = new LinkedHashMap<>();
//    parameters.forEach(x -> paramMap.put(x.name, x));
//    for (TypeMirror superType : type.getInterfaces()) {
//      if (MoreTypes.isTypeOf(ModelWithArgs.class, superType)) {
//        TypeName superTypeName = TypeName.get(superType);
//        if (superTypeName instanceof ParameterizedTypeName) {
//          for (TypeName superTypeVars : ((ParameterizedTypeName) superTypeName).typeArguments) {
//            if (superTypeVars instanceof TypeVariableName) {
//              paramMap.remove(((TypeVariableName) superTypeVars).name);
//            }
//          }
//        }
//      }
//    }
//    return new ArrayList<>(paramMap.values());
//  }

}
