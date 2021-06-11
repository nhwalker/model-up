package io.github.nhwalker.modelup.examples.jre16;

import io.github.nhwalker.modelup.MapModel;
import io.github.nhwalker.modelup.Model;
import io.github.nhwalker.modelup.ModelKey;
import io.github.nhwalker.modelup.container.ListenerRegistration;
import io.github.nhwalker.modelup.container.ModelContainer;

public class TestMain {

  public static void main(String[] args) {
    System.out.println(Model.class);
    ModelContainer<MapModel> ref = ModelContainer.create(x -> {
      x.concurrentListeners();
      x.initialValue(MapModel.EMPTY);
      x.requireNonNull();
    });
    ref.registerListener(change -> System.out.println(change));

    // Subscribe
    ListenerRegistration reg = ref.registerConcurrentListener($ -> {
      $.callImmediatly();
      $.keys(ModelKey.of("A"), ModelKey.of("B"));
      $.foldEvents();
    }, change -> {
      System.out.println(change);
    });

    reg.unregister();
  }

}
