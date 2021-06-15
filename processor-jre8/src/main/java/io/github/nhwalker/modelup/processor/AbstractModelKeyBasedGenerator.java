package io.github.nhwalker.modelup.processor;

import java.util.List;
import java.util.Objects;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

public abstract class AbstractModelKeyBasedGenerator {
  private ModelUpTypeDefinition definition;

  public void setDefinition(ModelUpTypeDefinition definition) {
    this.definition = definition;
  }
  
  protected static TypeName rawType(TypeName type) {
    if (type instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) type).rawType;
    }
    return type;
  }

  public ModelUpTypeDefinition getDefinition() {
    return definition;
  }

  public List<ModelKeyDefinition> getKeys() {
    return getDefinition().keys();
  }


  public JavaFile create() {
    Objects.requireNonNull(definition);
    return doCreate();
  }

  protected abstract JavaFile doCreate();

}
