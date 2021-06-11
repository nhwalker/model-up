package io.github.nhwalker.modelup.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import io.github.nhwalker.modelup.Model;
import io.github.nhwalker.modelup.ModelKey;

// TODO Document
/**
 * Defines the settings to use to register an individual listener
 */
public class ListenerSettings {

  /**
   * Provides a new {@link Queue} to be used to order change events for concurrent
   * listeners
   */
  @FunctionalInterface
  public static interface ChangeEventQueueFactory {
    <T extends Model> Queue<ChangeEvent<T>> create();
  }

  /**
   * @return {@code true} if this listener should be provided the current model
   *         value once registered ({@link ChangeEvent#getBefore()} be
   *         {@code null}, and {@link ChangeEvent#getAfter()} will be the current
   *         value
   */
  public boolean getCallImmediatly() {
    return callImmediatly;
  }

  /**
   * @return the keys to register for, or {@code null} if this is a global
   *         listener
   */
  public Collection<ModelKey<?>> getKeys() {
    return keys;
  }

  public boolean getIsGlobalListener() {
    return keys == null;
  }

  public void callImmediatly() {
    this.callImmediatly = true;
  }

  public void keys(ModelKey<?>... keys) {
    this.keys = Collections.unmodifiableList(Arrays.asList(keys));
  }

  public void keys(Collection<ModelKey<?>> keys) {
    this.keys = Collections.unmodifiableList(new ArrayList<>(keys));
  }

  private boolean callImmediatly;
  private List<ModelKey<?>> keys;

  public static class ConcurrentListenerSettings extends ListenerSettings {

    public  ChangeEventQueueFactory getNewChangeEventQueue() {
      return queueFactory;
    }

    public Executor getExecutor() {
      return this.executor;
    }

    public void executor(Executor executor) {
      this.executor = executor;
    }

    public void queue(ChangeEventQueueFactory queueFactory) {
      this.queueFactory = queueFactory;
    }

    public void withQueueCapacity(int capacity) {
      if (capacity <= 0)
        throw new IllegalArgumentException();
      this.queueFactory = new ChangeEventQueueFactory() {

        @Override
        public <T extends Model> Queue<ChangeEvent<T>> create() {
          return new LinkedBlockingQueue<>(capacity);
        }
      };
    }

    public void foldEvents() {
      this.queueFactory = FoldingChangeEventQueue::new;
    }

    public void setDefaults(ConcurrentListenerSettings defaultConcurrentSettings) {
      if (defaultConcurrentSettings == null) {
        return;
      }
      if (this.executor == null) {
        this.executor = defaultConcurrentSettings.executor;
      }
      if (this.queueFactory == null) {
        this.queueFactory = defaultConcurrentSettings.queueFactory;
      }
    }

    private Executor executor;
    private ChangeEventQueueFactory queueFactory;

  }

}
