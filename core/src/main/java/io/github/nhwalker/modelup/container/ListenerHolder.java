package io.github.nhwalker.modelup.container;

import java.util.Objects;

import io.github.nhwalker.modelup.Model;

/**
 * Helper class for managing a listener
 * 
 * @param <T> model type
 */
class ListenerHolder<T extends Model> {

  private final ModelListener<? super T> listener;

  ListenerHolder(ModelListener<? super T> listener) {
    this.listener = Objects.requireNonNull(listener);
  }

  void send(ChangeEvent<T> event) {
    this.listener.onModelChanged(event);
  }

  @Override
  public final int hashCode() {
    return listener.hashCode();
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ListenerHolder<?> other = (ListenerHolder<?>) obj;
    return listener == other.listener;
  }

  @Override
  public String toString() {
    return listener.toString();
  }

}
