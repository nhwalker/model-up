package io.github.nhwalker.modelup.processor;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.google.auto.common.MoreElements;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;

import io.github.nhwalker.modelup.ModelUp;
import io.github.nhwalker.modelup.processor.descriptors.ModelDescriptor;
import io.github.nhwalker.modelup.processor.generators.types.ArgumentsInterfaceGenerator;
import io.github.nhwalker.modelup.processor.generators.types.KeysTypeGenerator;
import io.github.nhwalker.modelup.processor.generators.types.RecordTypeGenerator;

public class ModelUpProcessor2 extends AbstractProcessor {

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
    Map<ClassName, ModelDescriptor> cache = new LinkedHashMap<>();

    for (Element e : elements) {
      if (MoreElements.isType(e)) {
        TypeElement typeElement = MoreElements.asType(e);
        ModelDescriptor descriptor = ModelDescriptor.resolve(cache, typeElement, processingEnv.getTypeUtils(),
            processingEnv.getElementUtils());
        JavaFile argFile = ArgumentsInterfaceGenerator.create(descriptor);
        JavaFile keysFile = KeysTypeGenerator.create(descriptor);
        JavaFile recordFile = RecordTypeGenerator.create(descriptor);

        try {
          argFile.writeTo(processingEnv.getFiler());
          keysFile.writeTo(processingEnv.getFiler());
          recordFile.writeTo(processingEnv.getFiler());
        } catch (IOException e1) {
          processingEnv.getMessager().printMessage(Kind.ERROR, "Failed to Write Files: " + e1, e);
          e1.printStackTrace();
        }
      }
    }
    return false;
  }
}
