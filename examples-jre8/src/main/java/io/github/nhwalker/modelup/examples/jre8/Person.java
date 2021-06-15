package io.github.nhwalker.modelup.examples.jre8;

import java.util.List;

import io.github.nhwalker.modelup.ModelUp;

@ModelUp(argsExtension = "io.github.nhwalker.modelup.examples.jre8.other.PersonArgs")
public interface Person extends Named {

  int age();

  List<String> alias();
  
}
