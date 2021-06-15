package io.github.nhwalker.modelup.processor;

import java.util.List;
import java.util.stream.Collectors;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

public class ModelUpTypeDefinition {
  private List<TypeVariableName> typeParameters;
  private TypeName modelType;
  private List<TypeName> modelExtends;

  private TypeName argsBaseType;
  private TypeName argsType;
  private List<TypeName> argsTypeExtends;
  private boolean generateArgs;

  private TypeName keysType;
  private boolean generateKeys;
  private TypeName recordType;
  private boolean generateRecord;

  private boolean memorizeHash;
  private boolean memorizeToString;

  public void argsBaseType(TypeName argsBaseType) {
    this.argsBaseType = argsBaseType;
  }

  public TypeName argsBaseType() {
    return argsBaseType;
  }

  public void memorizeHash(boolean memorizeHash) {
    this.memorizeHash = memorizeHash;
  }

  public void memorizeToString(boolean memorizeToString) {
    this.memorizeToString = memorizeToString;
  }

  public boolean memorizeHash() {
    return memorizeHash;
  }

  public boolean memorizeToString() {
    return memorizeToString;
  }

  private List<ModelKeyDefinition> keys;

  public ModelUpTypeDefinition() {
  }

  public boolean generateArgs() {
    return generateArgs;
  }

  public void generateArgs(boolean generateArgs) {
    this.generateArgs = generateArgs;
  }

  public boolean generateKeys() {
    return generateKeys;
  }

  public void generateKeys(boolean generateKeys) {
    this.generateKeys = generateKeys;
  }

  public boolean generateRecord() {
    return generateRecord;
  }

  public void generateRecord(boolean generateRecord) {
    this.generateRecord = generateRecord;
  }

  public List<TypeName> argsTypeExtends() {
    return argsTypeExtends;
  }

  public void argsTypeExtends(List<TypeName> argsTypeExtends) {
    this.argsTypeExtends = argsTypeExtends;
  }

  public List<TypeName> modelExtends() {
    return modelExtends;
  }

  public void modelExtends(List<TypeName> modelExtends) {
    this.modelExtends = modelExtends;
  }

  public List<TypeVariableName> typeParameters() {
    return typeParameters;
  }

  public void typeParameters(List<TypeVariableName> typeParameters) {
    this.typeParameters = typeParameters;
  }

  public TypeName modelType() {
    return modelType;
  }

  public void modelType(TypeName modelType) {
    this.modelType = modelType;
  }

  public TypeName argsType() {
    return argsType;
  }

  public void argsType(TypeName argsType) {
    this.argsType = argsType;
  }

  public TypeName recordType() {
    return recordType;
  }

  public void recordType(TypeName recordType) {
    this.recordType = recordType;
  }

  public TypeName keysType() {
    return keysType;
  }

  public void keysType(TypeName keysType) {
    this.keysType = keysType;
  }

  public void keys(List<ModelKeyDefinition> keys) {
    this.keys = keys;
  }

  public List<ModelKeyDefinition> keys() {
    return keys;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    toString(builder, "");
    return builder.toString();
  }

  private void toString(StringBuilder builder, String indent) {
    String indent2 = indent + "  ";
    String nl = "\n" + indent2;
    builder.append(modelType);
    builder.append(nl).append("argsType: ").append(argsType);
    builder.append(nl).append("keysType: ").append(keysType);
    builder.append(nl).append("recordType: ").append(recordType);
    builder.append(nl).append("typeParameters: ")
        .append(typeParameters.stream().map(x -> x.name + "<" + x.bounds + ">").collect(Collectors.joining(",")));
    builder.append(nl).append("extends: ").append("\n");
  }

}
