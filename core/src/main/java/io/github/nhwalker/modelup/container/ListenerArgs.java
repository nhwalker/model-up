package io.github.nhwalker.modelup.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import io.github.nhwalker.modelup.Model;
import io.github.nhwalker.modelup.ModelKey;

/**
 * Defines the settings to use to register an individual listener
 */
// TODO Document
public class ListenerArgs {
  private boolean callImmediatly;
  private List<ModelKey<?>> keys;

  /**
   * Create listener. Initial state is a global listener with
   * {@code callImmediatly = false}
   */
  public ListenerArgs() {
  }

  /**
   * Create listener, copy state from argument
   */
  public ListenerArgs(ListenerArgs copy) {
    this.callImmediatly = copy.callImmediatly;
    this.keys = copy.keys;
  }

  /**
   * Set {@code callImmediatly = true}
   */
  public void callImmediatly() {
    callImmediatly(true);
  }

  /**
   * Set {@code callImmediatly}
   */
  public void callImmediatly(boolean callImmediatly) {
    this.callImmediatly = callImmediatly;
  }

  /**
   * Set to be a global listener
   */
  public void globalListener() {
    this.keys = null;
  }

  /**
   * Sets {@code keys}, so no longer a global listener
   */
  public void keys(ModelKey<?>... keys) {
    this.keys = Collections.unmodifiableList(Arrays.asList(keys));
  }

  /**
   * Sets {@code keys}, so no longer a global listener
   */
  public void keys(Collection<ModelKey<?>> keys) {
    this.keys = Collections.unmodifiableList(new ArrayList<>(keys));
  }

  /**
   * @return {@code true} if this listener should be provided the current model
   *         value once registered ({@link ChangeEvent#getBefore()} be
   *         {@code null}, and {@link ChangeEvent#getAfter()} will be the current
   *         value
   */
  public boolean isCallImmediatly() {
    return callImmediatly;
  }

  /**
   * @return the keys the listener is interested in receiving events for, or
   *         {@code Optional.empty()} if this is a global listener configuration
   */
  public Optional<List<ModelKey<?>>> keys() {
    return Optional.ofNullable(keys);
  }

  /**
   * Provides a new {@link Queue} to be used to order change events for concurrent
   * listeners
   */
  @FunctionalInterface
  public static interface ChangeEventQueueFactory {
    <T extends Model> Queue<ChangeEvent<T>> create();
  }

  public static class ConcurrentListenerArgs extends ListenerArgs {

    private Executor executor;
    private ChangeEventQueueFactory queueFactory;

    /**
     * Create listener. Initial state is a global listener with
     * {@code callImmediatly = false} and no preference for
     */
    public ConcurrentListenerArgs() {
    }

    public ConcurrentListenerArgs(ListenerArgs copy) {
      super(copy);
    }

    public ConcurrentListenerArgs(ConcurrentListenerArgs copy) {
      super(copy);
      this.executor = copy.executor;
      this.queueFactory = copy.queueFactory;
    }

    public void executor(Executor executor) {
      this.executor = executor;
    }

    public void queue(ChangeEventQueueFactory queueFactory) {
      this.queueFactory = queueFactory;
    }

    public void foldingQueue() {
      this.queueFactory = FoldingChangeEventQueue::new;
    }

    public void fixedCapacityQueue(int capacity) {
      if (capacity <= 0)
        throw new IllegalArgumentException();
      this.queueFactory = new ChangeEventQueueFactory() {

        @Override
        public <T extends Model> Queue<ChangeEvent<T>> create() {
          return new LinkedBlockingQueue<>(capacity);
        }
      };
    }

    public ChangeEventQueueFactory queue() {
      return queueFactory;
    }

    public Executor executor() {
      return this.executor;
    }

  }

}
