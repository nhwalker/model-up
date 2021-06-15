package io.github.nhwalker.modelup.examples.jre8;

import io.github.nhwalker.modelup.ModelUp;

@ModelUp(argsExtension = "NamedArgs")
public interface Named {
  String firstName();

  String lastName();

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
