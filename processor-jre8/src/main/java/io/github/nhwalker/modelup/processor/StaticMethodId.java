package io.github.nhwalker.modelup.processor;

import com.squareup.javapoet.ClassName;

public class StaticMethodId {
  private final ClassName type;
  private final String name;
  private final boolean inherit;

  public StaticMethodId(ClassName type, String name, boolean inherit) {
    super();
    this.type = type;
    this.name = name;
    this.inherit = inherit;
  }

  public String getName() {
    return name;
  }

  public ClassName getType() {
    return type;
  }

  public boolean isInherit() {
    return inherit;
  }

}
