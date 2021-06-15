package io.github.nhwalker.modelup.examples.jre8.other;

import java.util.Arrays;

import io.github.nhwalker.modelup.examples.jre8.PersonArgsBase;

public interface PersonArgs extends PersonArgsBase {
  default void alias(String... alias) {
    alias(Arrays.asList(alias));

  }
}
