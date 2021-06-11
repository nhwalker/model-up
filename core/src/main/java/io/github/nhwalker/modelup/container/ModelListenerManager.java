package io.github.nhwalker.modelup.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import io.github.nhwalker.modelup.Model;
import io.github.nhwalker.modelup.ModelKey;
import io.github.nhwalker.modelup.container.ListenerSettings.ConcurrentListenerSettings;

// TODO Document
public class ModelListenerManager<T extends Model> implements ModelListener<T>, ModelListenerManagement<T> {

  private final ConcurrentHashMap<ModelKey<?>, Set<ListenerHolder<T>>> listeners = new ConcurrentHashMap<>();
  private final CopyOnWriteArraySet<ListenerHolder<T>> globalListeners = new CopyOnWriteArraySet<>();

  private final boolean concurrent;
  private final ConcurrentListenerSettings defaultConcurrentSettings;
  private final DelegatingModelListenerManagement<T> clientOnlyView;

  public ModelListenerManager(boolean concurrent, ConcurrentListenerSettings defaultConcurrentSettings) {
    this.concurrent = concurrent;
    this.defaultConcurrentSettings = defaultConcurrentSettings;
    this.clientOnlyView = new DelegatingModelListenerManagement<T>() {
      @Override
      protected ModelListenerManagement<T> delegate() {
        return ModelListenerManager.this;
      }
    };
  }

  public ModelListenerManagement<T> asModelListenerManagement() {
    return this.clientOnlyView;
  }

  private ListenerHolder<T> wrapForRemoval(ModelListener<? super T> listener) {
    return new ListenerHolder<>(listener);
  }

  private ListenerHolder<T> wrap(ModelListener<? super T> listener, ListenerSettings settings) {
    if (settings instanceof ConcurrentListenerSettings) {
      ConcurrentListenerSettings mainSettings = (ConcurrentListenerSettings) settings;
      return new ListenerHolderConcurrent<>(listener, mainSettings, defaultConcurrentSettings);
    } else if (this.concurrent) {
      return new ListenerHolderConcurrent<>(listener, defaultConcurrentSettings);
    } else {
      return new ListenerHolder<>(listener);
    }
  }

  protected T getCurrentState() {
    return null;
  }

  @Override
  public ListenerRegistration registerListener(ModelListener<? super T> listener) {
    return register(listener, null);
  }

  @Override
  public ListenerRegistration registerConcurrentListener(ConcurrentListenerSettings settings,
      ModelListener<? super T> listener) {
    return register(listener, settings);
  }

  @Override
  public ListenerRegistration registerListener(ListenerSettings settings, ModelListener<? super T> listener) {
    return register(listener, settings);
  }

  private ListenerRegistration register(ModelListener<? super T> listener, ListenerSettings settings) {
    ListenerRegistration reg;
    if (settings == null || settings.getIsGlobalListener()) {
      reg = registerGlobal(wrap(listener, settings));
    } else {
      reg = registerByKey(wrap(listener, settings), settings.getKeys());
    }

    if (settings.getCallImmediatly()) {
      T currentState = getCurrentState();
      if (currentState != null) {
        listener.onModelChanged(ChangeEvent.create(null, currentState));
      }
    }
    return reg;
  }

  private ListenerRegistration registerGlobal(ListenerHolder<T> listener) {
    globalListeners.add(listener);
    return () -> unregister(listener);
  }

  private ListenerRegistration registerByKey(ListenerHolder<T> listener, Collection<ModelKey<?>> keys) {
    for (ModelKey<?> key : keys) {
      listeners.computeIfAbsent(key, x -> new CopyOnWriteArraySet<>()).add(listener);
    }
    return () -> unregister(listener);
  }

  @Override
  public void unregisterListener(ModelListener<? super T> listener) {
    unregister(wrapForRemoval(listener));
  }

  public void clearListeners() {
    globalListeners.clear();
    listeners.clear();
  }

  private void unregister(ListenerHolder<T> listener) {
    globalListeners.remove(listener);

    for (ModelKey<?> key : listeners.keySet()) {
      listeners.computeIfPresent(key, (k, cur) -> {
        if (cur.remove(listener)) {
          return cur.isEmpty() ? null : cur;
        }
        return cur;
      });
    }
  }

  /**
   * Same as calling {@link #send(ChangeEvent)}
   * <p>
   * Included only so that a {@link ModelListenerManager} can also be a listener.
   * Prefer {@link #send(ChangeEvent)} when calling directly
   */
  @SuppressWarnings("unchecked")
  @Deprecated
  @Override
  public void onModelChanged(ChangeEvent<? extends T> event) {
    send((ChangeEvent<T>) event);
  }

  /**
   * Send event to all global registered listeners and all listeners that are
   * listening to specific keys changed by this event
   * 
   * @param event the event to send
   */
  public void send(ChangeEvent<T> event) {
    Set<ListenerHolder<T>> toNotify = listenersFor(event);

    List<Exception> error = null;
    for (ListenerHolder<T> listener : toNotify) {
      try {
        listener.send(event);
      } catch (Exception e) {
        if (error == null) {
          error = new ArrayList<>();
        }
        error.add(e);
      }
    }
    if (error != null) {
      if (error.size() == 1) {
        throw new ChangeDispatchException("Exception thrown by Listener", error.get(0));
      } else {
        ChangeDispatchException e = new ChangeDispatchException("Multiple Exceptions thrown by Listeners");
        error.forEach(err -> e.addSuppressed(err));
        throw e;
      }
    }

  }

  private Set<ListenerHolder<T>> listenersFor(ChangeEvent<T> event) {
    Set<ModelKey<?>> changedKeys = event.getChangedKeys();
    Set<ModelKey<?>> listenerKeys = listeners.keySet();

    Set<ListenerHolder<T>> matches;
    if (listenerKeys.isEmpty()) {
      matches = Collections.emptySet();
    } else if (changedKeys.size() <= listenerKeys.size()) {
      matches = event.getChangedKeys().stream()//
          .flatMap(field -> listeners.getOrDefault(field, Collections.emptySet()).stream())//
          .collect(Collectors.toCollection(LinkedHashSet::new));
    } else {
      matches = listeners.entrySet().stream()//
          .filter(e -> event.hasChanged(e.getKey())).flatMap(e -> e.getValue().stream())//
          .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    if (matches.isEmpty()) {
      return globalListeners;
    } else {
      matches.addAll(globalListeners);
      return matches;
    }

  }

}
