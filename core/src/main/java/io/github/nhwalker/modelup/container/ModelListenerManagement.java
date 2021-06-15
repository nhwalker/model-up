package io.github.nhwalker.modelup.container;

import java.util.function.Consumer;

import io.github.nhwalker.modelup.Model;
import io.github.nhwalker.modelup.container.ListenerArgs.ConcurrentListenerArgs;

// TODO Document
public interface ModelListenerManagement<T extends Model> {

  ListenerRegistration registerListener(ModelListener<? super T> listener);

  ListenerRegistration registerListener(ListenerArgs settings, ModelListener<? super T> listener);

  default ListenerRegistration registerListener(Consumer<ListenerArgs> settings,
      ModelListener<? super T> listener) {
    ListenerArgs x = new ListenerArgs();
    settings.accept(x);
    return registerListener(x, listener);
  }

  ListenerRegistration registerConcurrentListener(ConcurrentListenerArgs settings,
      ModelListener<? super T> listener);

  default ListenerRegistration registerConcurrentListener(Consumer<ConcurrentListenerArgs> settings,
      ModelListener<? super T> listener) {
    ConcurrentListenerArgs x = new ConcurrentListenerArgs();
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
    public ListenerRegistration registerListener(ListenerArgs settings, ModelListener<? super T> listener) {
      return delegate().registerListener(settings, listener);
    }

    @Override
    public ListenerRegistration registerConcurrentListener(ConcurrentListenerArgs settings,
        ModelListener<? super T> listener) {
      return delegate().registerConcurrentListener(settings, listener);
    }

    @Override
    public final ListenerRegistration registerConcurrentListener(Consumer<ConcurrentListenerArgs> settings,
        ModelListener<? super T> listener) {
      return ModelListenerManagement.super.registerConcurrentListener(settings, listener);
    }

    @Override
    public final ListenerRegistration registerListener(Consumer<ListenerArgs> settings,
        ModelListener<? super T> listener) {
      return ModelListenerManagement.super.registerListener(settings, listener);
    }

    @Override
    public void unregisterListener(ModelListener<? super T> listener) {
      delegate().unregisterListener(listener);
    }
  }
}
