package io.github.nhwalker.modelup.processor;

import java.util.Objects;

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

  @Override
  public int hashCode() {
    return Objects.hash(inherit, name, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    StaticMethodId other = (StaticMethodId) obj;
    return inherit == other.inherit && Objects.equals(name, other.name) && Objects.equals(type, other.type);
  }

}
