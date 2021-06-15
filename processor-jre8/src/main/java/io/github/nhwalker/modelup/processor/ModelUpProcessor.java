package io.github.nhwalker.modelup.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import io.github.nhwalker.modelup.ModelUp;

public class ModelUpProcessor extends AbstractProcessor {
  private static final Set<String> IGNORE = new HashSet<>(Arrays.asList("getClass", "hashCode", "toString", "clone"));

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton(ModelUp.class.getName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_8;
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(ModelUp.class);
    Map<ClassName, ModelUpTypeDefinition> cache = new LinkedHashMap<>();

    for (Element e : elements) {
      if (e instanceof TypeElement) {
        TypeElement typeElement = (TypeElement) e;
        ModelUpTypeDefinition typeDef = getOrCreate(cache, roundEnv, typeElement);
        runKeysGenerator(typeDef, roundEnv, typeElement);
        runArgsInterfaceGenerator(typeDef, roundEnv, typeElement);
        runRecordGenerator(typeDef, roundEnv, typeElement);
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

  private List<ModelKeyDefinition> findAllKeys(RoundEnvironment roundEnv, TypeElement e) {
    ImmutableSet<ExecutableElement> methods = MoreElements.getAllMethods(e, processingEnv.getTypeUtils(),
        processingEnv.getElementUtils());

    List<ModelKeyDefinition> fields = new ArrayList<>();
    for (Element member : methods) {
      if (member instanceof ExecutableElement) {
        ExecutableElement method = (ExecutableElement) member;
        String name = member.getSimpleName().toString();
        if (!method.getModifiers().contains(Modifier.STATIC) && !IGNORE.contains(name)
            && method.getParameters().isEmpty()) {
          TypeMirror returnType = method.getReturnType();
          if (returnType.getKind() != TypeKind.VOID) {
            String comment = processingEnv.getElementUtils().getDocComment(method);
            String doc = processDoc(comment);

            DeclaredType declaredType = MoreTypes.asDeclared(e.asType());

            TypeName returnTypeName = TypeName.get(returnType);
            TypeMirror methodMirror = processingEnv.getTypeUtils().asMemberOf(declaredType, method);
            TypeName effectiveType = TypeName.get(MoreTypes.asExecutable(methodMirror).getReturnType());
            TypeName effectiveKeyType = effectiveType(effectiveType);

            log("returnTypeName: " + returnTypeName);
            log("methodMirror: " + methodMirror);
            log("effectiveType: " + effectiveType);
            log("effectiveKeyType: " + effectiveKeyType);

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

  private ModelUpTypeDefinition getOrCreate(Map<ClassName, ModelUpTypeDefinition> cache, RoundEnvironment roundEnv,
      TypeElement type) {
    TypeName modelType = TypeName.get(type.asType());
    ClassName rawName = TypeNameUtils.rawType(modelType);
    ModelUpTypeDefinition def = cache.get(rawName);
    if (def == null) {
      def = createModelUpTypeDef(cache, roundEnv, type);
      cache.put(rawName, def);
    }
    return def;
  }

  private ModelUpTypeDefinition createModelUpTypeDef(Map<ClassName, ModelUpTypeDefinition> cache,
      RoundEnvironment roundEnv, TypeElement type) {
    ModelUpTypeDefinition def = new ModelUpTypeDefinition();
    String packageName = processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
    String baseName = type.getSimpleName().toString();
    TypeName modelType = TypeName.get(type.asType());
    def.modelType(modelType);

    ClassName rawArgsName = ClassName.get(packageName, baseName + "Args");
    ClassName rawKeysName = ClassName.get(packageName, baseName + "Keys");
    ClassName rawRecordName = ClassName.get(packageName, baseName + "Record");

    def.keysType(rawKeysName);

    log("::::::::::::::::::::::::::::::::");
    log(type);
    log("::::::::::::::::::::::::::::::::");
    if (modelType instanceof ParameterizedTypeName) {
      ParameterizedTypeName parameterized = (ParameterizedTypeName) modelType;
      List<TypeVariableName> params = new ArrayList<>();
      parameterized.typeArguments.forEach(paramType -> {
        if (paramType instanceof TypeVariableName) {
          TypeVariableName param = (TypeVariableName) paramType;
          log("PREFIX: ", paramType);
          params.add(param);
        } else {
          // TODO ERROR
        }
      });
      def.typeParameters(Collections.unmodifiableList(params));
      TypeName[] args = params.toArray(new TypeName[0]);
      def.argsType(ParameterizedTypeName.get(rawArgsName, args));
      def.recordType(ParameterizedTypeName.get(rawRecordName, args));

    } else {
      def.argsType(rawArgsName);
      def.recordType(rawRecordName);
      def.typeParameters(Collections.emptyList());
    }

    ArrayList<TypeName> modelExtends = new ArrayList<>();
    ArrayList<TypeName> argsExtends = new ArrayList<>();
    for (TypeMirror parent : type.getInterfaces()) {
      TypeName parentType = TypeName.get(parent);
      modelExtends.add(parentType);
      ClassName rawParentType = TypeNameUtils.rawType(parentType);
      ClassName rawParentArgsType = ClassName.get(rawParentType.packageName(), rawParentType.simpleName() + "Args");
      if (parentType instanceof ParameterizedTypeName) {
        ParameterizedTypeName asParam = (ParameterizedTypeName) parentType;
        argsExtends.add(ParameterizedTypeName.get(rawParentArgsType, asParam.typeArguments.toArray(new TypeName[0])));
      } else {
        argsExtends.add(rawParentArgsType);
      }
    }

    def.argsTypeExtends(argsExtends);
    def.modelExtends(modelExtends);
    def.keys(findAllKeys(roundEnv, type));

    log(def);
    return def;
  }

  private void log(Object obj) {
    processingEnv.getMessager().printMessage(Kind.WARNING, String.valueOf(obj));
  }

  private void log(String prefix, Object obj) {
    processingEnv.getMessager().printMessage(Kind.WARNING,
        prefix + ": " + (obj == null ? "null" : "[" + obj.getClass() + "] " + obj));
  }

}
