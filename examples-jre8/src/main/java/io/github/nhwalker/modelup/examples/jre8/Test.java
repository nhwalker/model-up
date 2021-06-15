package io.github.nhwalker.modelup.examples.jre8;

import java.util.Arrays;

public class Test {
  public static void main(String[] args) {
//    Consumer<NamedArgs> setJohn = x -> x.name("John", "Walker");
    PizzaDeliveryPerson driver = new PizzaDeliveryPersonRecord(x -> {
      x.age(12);
      x.name("Bob", "Smith");
      x.id(12345L);
      x.payload(new PizzaRecord(Arrays.asList("cheese", "bacon")));
      x.alias("A", "B", "C");
      System.out.println("Args = " + x);
    });
//    driver = driver.withUpdate(setJohn);

    System.out.println(driver);
  }
}
