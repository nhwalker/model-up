package io.github.nhwalker.modelup.examples.jre8;

public class Test {
  public static void main(String[] args) {
    DeliveryDriver<String> driver = new DeliveryDriverRecord<>(x->{
      x.age(12);
      x.firstName("Bob");
      x.lastName("Smith");
      x.id(12345L);
      x.payload("<this-is-what-i-have>");
      System.out.println("Args = "+x);
    });
    System.out.println(driver);
  }
}
