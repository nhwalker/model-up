package io.github.nhwalker.modelup.processor.descriptors;

import javax.lang.model.element.Element;

import io.github.nhwalker.modelup.ModelUp;

@ModelUp(generateArgs = false, generateKeys = false, generateRecord = false)
public class ModelUpCapture {

  public static ModelUp resolveModelUp(Element source) {
    ModelUp ann = source.getAnnotation(ModelUp.class);
    if (ann == null) {
      ann = ModelUpCapture.class.getAnnotation(ModelUp.class);
    }
    return ann;
  }
}