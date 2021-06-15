package io.github.nhwalker.modelup.examples.jre8;

import io.github.nhwalker.modelup.ModelUp;

@ModelUp(memorizeHash = true, memorizeToString = true)
public interface DeliveryDriver<T> extends Person {

  T payload();

  long id();
}
