package io.github.nhwalker.modelup.examples.jre8;

import java.util.Objects;

import io.github.nhwalker.modelup.ModelUp;

@ModelUp(argsExtension = "NamedArgs")
public interface Named {
  String firstName();

  String lastName();

  @ModelUp.InitialArgs
  static void initialize(NamedArgs value) {
    value.firstName("");
    value.lastName("");
  }

  @ModelUp.Sanatize
  static void sanatize(NamedArgs value) {
    if (value.firstName().isEmpty()) {
      value.firstName("<unknown>");
    }
    if (value.lastName().isEmpty()) {
      value.lastName("<unknown>");
    }
  }

  @ModelUp.Validate
  static void validate(Named value) {
    Objects.requireNonNull(value.firstName());
    Objects.requireNonNull(value.lastName());
  }

  public interface NamedArgs extends NamedArgsBase {
    default void name(String firstName, String lastName) {
      firstName(firstName);
      lastName(lastName);
    }

    default String name() {
      return firstName() + " " + lastName();
    }
  }
}
