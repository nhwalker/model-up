package io.github.nhwalker.modelup.examples.jre8;

import io.github.nhwalker.modelup.ModelUp;

@ModelUp(memorizeHash = true, memorizeToString = true)
public interface DeliveryDriver<E>
    extends Person {

  E payload();

  long id();
}
