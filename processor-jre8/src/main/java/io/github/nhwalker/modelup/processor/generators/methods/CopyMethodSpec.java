package io.github.nhwalker.modelup.processor.generators.methods;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import io.github.nhwalker.modelup.processor.descriptors.KeyDescriptor;
import io.github.nhwalker.modelup.processor.descriptors.ModelDescriptor;

public class CopyMethodSpec {

  private static MethodSpec.Builder base(ModelDescriptor descriptor, TypeName copySrcType) {
    MethodSpec.Builder b = MethodSpec.methodBuilder("copy")//
        .addParameter(copySrcType, "copy")
        .addModifiers(Modifier.PUBLIC);
    for (KeyDescriptor key : descriptor.getKeys().values()) {
      if (key.dontAllowSet()) {
        // skip
      } else {
        b.addStatement("this.$L(copy.$L())", key.name(), key.name());
      }
    }
    return b;
  }

  public static MethodSpec.Builder createForInterface(ModelDescriptor descriptor, TypeName copySrcType) {
    return base(descriptor, copySrcType)//
        .addModifiers(Modifier.DEFAULT);
  }

}
