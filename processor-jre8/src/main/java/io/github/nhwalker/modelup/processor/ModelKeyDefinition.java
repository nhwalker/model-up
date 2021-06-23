package io.github.nhwalker.modelup.processor;

import java.util.Objects;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.TypeName;

public class ModelKeyDefinition {

  private final String name;

  private final TypeName valueType;
  private final TypeName effectiveType;
  private final TypeName effectiveKeyType;

  private final TypeName belongsTo;
  private final String fieldDescription;

  private final boolean overrides;

  public ModelKeyDefinition(String name, TypeName valueType, TypeName effectiveType, TypeName effectiveKeyType,
      TypeName belongsTo, String fieldDescription, boolean overrides) {
    this.name = name;
    this.valueType = valueType;
    this.effectiveType = effectiveType;
    this.effectiveKeyType = effectiveKeyType;
    this.belongsTo = belongsTo;
    this.fieldDescription = fieldDescription;
    this.overrides = overrides;
  }

  public boolean overrides() {
    return overrides;
  }

  public String getName() {
    return name;
  }

  public String getFieldDescription() {
    return fieldDescription;
  }

  public TypeName getBelongsTo() {
    return belongsTo;
  }

  public TypeName getValueType() {
    return valueType;
  }

  public String getNameUpperCaseFormat() {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, getName());
  }

  public TypeName getEffectiveKeyType() {
    return effectiveKeyType;
  }

  public TypeName getEffectiveType() {
    return effectiveType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(belongsTo, fieldDescription, name, valueType);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ModelKeyDefinition other = (ModelKeyDefinition) obj;
    return Objects.equals(belongsTo, other.belongsTo) && Objects.equals(fieldDescription, other.fieldDescription)
        && Objects.equals(name, other.name) && Objects.equals(valueType, other.valueType);
  }

  @Override
  public String toString() {
    return "ModelKeyDefinition [" + name + ": " + valueType + "]";
  }

}
