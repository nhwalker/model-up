package io.github.nhwalker.modelup.examples.jre8;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.github.nhwalker.modelup.ModelUp;
import io.github.nhwalker.modelup.examples.jre8.other.PersonArgs;

@ModelUp(argsExtension = "io.github.nhwalker.modelup.examples.jre8.other.PersonArgs")
public interface Person extends Named,HasAddress{

  int age();

  List<String> alias();
  
  @Override
  String firstName();

  @ModelUp.InitialArgs
  static void initialize(PersonArgs value) {
    value.age(-999);
    value.alias(new ArrayList<>());
  }

  @ModelUp.Sanatize
  static void sanatize(PersonArgs value) {
    value.alias(Collections.unmodifiableList(new ArrayList<>(value.alias())));
  }

  @ModelUp.Validate
  static void validate(Person value) {
    value.alias().forEach(Objects::requireNonNull);
    if (value.age() < 0) {
      throw new IllegalArgumentException("Age must be >= 0");
    }
  }

}
