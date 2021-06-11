package io.github.nhwalker.modelup.container;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import io.github.nhwalker.modelup.Model;
import io.github.nhwalker.modelup.container.ListenerSettings.ConcurrentListenerSettings;

class ModelContainerAsync<T extends Model> extends ModelContainer<T> {
  private final LinkedBlockingQueue<AnUpdateEvent<?>> queue = new LinkedBlockingQueue<>();
  private final Executor executor;
  private final AtomicBoolean queued = new AtomicBoolean();
  private volatile T value;

  ModelContainerAsync(ParametersAsync<T> builder) {
    super(builder);
    this.value = postProcessing(builder.getInitialValue());
    Executor ex = builder.getAsyncExecutor();
    this.executor = ex == null ? ForkJoinPool.commonPool() : ex;
  }

  private <R> CompletableFuture<R> queue(AnUpdateEvent<R> update) {
    queue.add(update);
    if (!queued.getAndSet(true)) {
      executor.execute(this::run);
    }
    return update.result.thenApply(Function.identity());
  }

  private void run() {
    AnUpdateEvent<?> event = queue.poll();
    if (event == null) {
      queued.set(false);

      // Make sure that if a new event was added around
      // the same time we stopped processing events that
      // a new task was queued to dispatch the event
      if (!queue.isEmpty() && !queued.getAndSet(true)) {
        executor.execute(this::run);
      }
    } else {
      try {
        event.run();
      } finally {
        executor.execute(this::run);
      }
    }
  }

  @Override
  public T getValue() {
    return this.value;
  }

  @Override
  public void setValue(T value) {
    update(ignore -> value);
  }

  @Override
  public T update(UnaryOperator<T> update) {
    return updateAsync(update).join();
  }

  @Override
  public CompletableFuture<T> updateAsync(UnaryOperator<T> update) {
    return queue(new UpdateEvent(update));
  }

  @Override
  public T multiStageUpdate(List<UnaryOperator<T>> stages) {
    return multiStageUpdateAsync(stages).join();
  }

  @Override
  public CompletableFuture<T> multiStageUpdateAsync(List<UnaryOperator<T>> stages) {
    return queue(new UpdateEventMultiStage(new ArrayList<>(stages)));
  }

  @Override
  public ListenerRegistration registerConcurrentListener(ConcurrentListenerSettings settings, ModelListener<? super T> listener) {
    if (settings.getCallImmediatly()) {
      CompletableFuture<ListenerRegistration> future = queue(new RegisterListenerEvent(listener, settings));
      return () -> future.thenAccept(reg -> reg.unregister());
    } else {
      return super.registerListener(settings, listener);
    }
  }

  @Override
  public ListenerRegistration registerListener(ListenerSettings settings, ModelListener<? super T> listener) {
    if (settings.getCallImmediatly()) {
      CompletableFuture<ListenerRegistration> future = queue(new RegisterListenerEvent(listener, settings));
      return () -> future.thenAccept(reg -> reg.unregister());
    } else {
      return super.registerListener(settings, listener);
    }
  }

  private abstract class AnUpdateEvent<R> implements Runnable {
    final CompletableFuture<R> result = new CompletableFuture<>();
  }

  private class RegisterListenerEvent extends AnUpdateEvent<ListenerRegistration> {
    private final ModelListener<? super T> listener;
    private final ListenerSettings settings;

    public RegisterListenerEvent(ModelListener<? super T> listener, ListenerSettings settings) {
      this.listener = listener;
      this.settings = settings;
    }

    @Override
    public void run() {
      ModelContainerAsync.super.registerListener(settings, listener);
    }
  }

  private class UpdateEvent extends AnUpdateEvent<T> {
    private final UnaryOperator<T> update;

    public UpdateEvent(UnaryOperator<T> update) {
      this.update = update;
    }

    @Override
    public void run() {
      T before = ModelContainerAsync.this.value;
      T after = postProcessing(update.apply(before));
      ModelContainerAsync.this.value = after;
      try {
        sendChange(before, after);
        this.result.complete(after);
      } catch (Throwable t) {
        this.result.completeExceptionally(t);
      }
    }
  }

  private class UpdateEventMultiStage extends AnUpdateEvent<T> {
    private final List<UnaryOperator<T>> updates;

    public UpdateEventMultiStage(List<UnaryOperator<T>> updates) {
      this.updates = updates;
    }

    @Override
    public void run() {
      T after = ModelContainerAsync.this.value;
      T before;
      ModelContainerAsync.this.value = after;
      try {
        for (UnaryOperator<T> update : updates) {
          before = after;
          after = postProcessing(update.apply(before));
          ModelContainerAsync.this.value = after;
          sendChange(before, after);
        }
        this.result.complete(after);
      } catch (Throwable t) {
        this.result.completeExceptionally(t);
      }
    }
  }
}