package io.github.nhwalker.modelup.examples.jre8;

import java.util.List;

import io.github.nhwalker.modelup.ModelUp;

@ModelUp
public interface Pizza {
  List<String> toppings();
}
