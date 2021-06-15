package io.github.nhwalker.modelup.container;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.nhwalker.modelup.Model;
import io.github.nhwalker.modelup.container.ListenerArgs.ChangeEventQueueFactory;
import io.github.nhwalker.modelup.container.ListenerArgs.ConcurrentListenerArgs;

/**
 * Helper class for managing a listener that is notified of events concurrently
 * 
 * @param <T> model type
 */
class ListenerHolderConcurrent<T extends Model> extends ListenerHolder<T> {
  private final Queue<ChangeEvent<T>> queue;
  private final Executor executor;
  private final AtomicBoolean queued = new AtomicBoolean();

  ListenerHolderConcurrent(ModelListener<? super T> listener, ConcurrentListenerArgs settings) {
    this(listener, settings, null);
  }

  ListenerHolderConcurrent(ModelListener<? super T> listener, ConcurrentListenerArgs settings,
      ConcurrentListenerArgs defaultSettings) {
    super(listener);
    Executor executor = settings.executor();
    if (executor == null && defaultSettings != null) {
      executor = defaultSettings.executor();
    }
    ChangeEventQueueFactory queueFactory = settings.queue();
    if (queueFactory == null && defaultSettings != null) {
      queueFactory = defaultSettings.queue();
    }
    this.executor = executor == null ? ForkJoinPool.commonPool() : executor;
    this.queue = queueFactory == null ? new LinkedBlockingQueue<>() : Objects.requireNonNull(queueFactory.create());
  }

  @Override
  public void send(ChangeEvent<T> event) {
    queue.add(event);
    if (!queued.getAndSet(true)) {
      executor.execute(this::run);
    }
  }

  private void run() {
    ChangeEvent<T> event = queue.poll();
    if (event == null) {
      queued.set(false);

      // Make sure that if a new event was added around
      // the same time we stopped processing events that
      // the a new task was queued to dispatch the event
      if (!queue.isEmpty() && !queued.getAndSet(true)) {
        executor.execute(this::run);
      }
    } else {
      try {
        dispatch(event);
      } finally {
        executor.execute(this::run);
      }
    }
  }

  private void dispatch(ChangeEvent<T> t) {
    try {
      super.send(t);
    } catch (Exception e) {
      // TODO - Log?
      e.printStackTrace();
    }
  }

}
