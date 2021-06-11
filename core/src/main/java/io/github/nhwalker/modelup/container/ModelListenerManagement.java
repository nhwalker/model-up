package io.github.nhwalker.modelup.container;

import java.util.function.Consumer;

import io.github.nhwalker.modelup.Model;
import io.github.nhwalker.modelup.container.ListenerSettings.ConcurrentListenerSettings;

// TODO Document
public interface ModelListenerManagement<T extends Model> {

  ListenerRegistration registerListener(ModelListener<? super T> listener);

  ListenerRegistration registerListener(ListenerSettings settings, ModelListener<? super T> listener);

  default ListenerRegistration registerListener(Consumer<ListenerSettings> settings,
      ModelListener<? super T> listener) {
    ListenerSettings x = new ListenerSettings();
    settings.accept(x);
    return registerListener(x, listener);
  }

  ListenerRegistration registerConcurrentListener(ConcurrentListenerSettings settings,
      ModelListener<? super T> listener);

  default ListenerRegistration registerConcurrentListener(Consumer<ConcurrentListenerSettings> settings,
      ModelListener<? super T> listener) {
    ConcurrentListenerSettings x = new ConcurrentListenerSettings();
    settings.accept(x);
    return registerConcurrentListener(x, listener);
  }

  void unregisterListener(ModelListener<? super T> listener);

  public static abstract class DelegatingModelListenerManagement<T extends Model>
      implements ModelListenerManagement<T> {
    protected abstract ModelListenerManagement<T> delegate();

    @Override
    public ListenerRegistration registerListener(ModelListener<? super T> listener) {
      return delegate().registerListener(listener);
    }

    @Override
    public ListenerRegistration registerListener(ListenerSettings settings, ModelListener<? super T> listener) {
      return delegate().registerListener(settings, listener);
    }

    @Override
    public ListenerRegistration registerConcurrentListener(ConcurrentListenerSettings settings,
        ModelListener<? super T> listener) {
      return delegate().registerConcurrentListener(settings, listener);
    }

    @Override
    public final ListenerRegistration registerConcurrentListener(Consumer<ConcurrentListenerSettings> settings,
        ModelListener<? super T> listener) {
      return ModelListenerManagement.super.registerConcurrentListener(settings, listener);
    }

    @Override
    public final ListenerRegistration registerListener(Consumer<ListenerSettings> settings,
        ModelListener<? super T> listener) {
      return ModelListenerManagement.super.registerListener(settings, listener);
    }

    @Override
    public void unregisterListener(ModelListener<? super T> listener) {
      delegate().unregisterListener(listener);
    }
  }
}
