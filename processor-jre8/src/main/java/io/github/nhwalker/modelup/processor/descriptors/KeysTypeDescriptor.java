package io.github.nhwalker.modelup.processor.descriptors;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import com.squareup.javapoet.ClassName;

import io.github.nhwalker.modelup.ModelUp;

public class KeysTypeDescriptor {
  private final ClassName keysType;

  public KeysTypeDescriptor(ClassName keysType) {
    super();
    this.keysType = keysType;
  }

  public static KeysTypeDescriptor create(TypeElement source, ModelUp ann, Elements elements) {
    String packageName = elements.getPackageOf(source).getQualifiedName().toString();
    String baseName = source.getSimpleName().toString();
    ClassName name = getRawKeysName(packageName, baseName, ann);

    return new KeysTypeDescriptor(name);
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

  public ClassName getKeysType() {
    return keysType;
  }
}