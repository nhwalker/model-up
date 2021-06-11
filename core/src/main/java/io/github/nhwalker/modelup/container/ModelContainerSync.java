package io.github.nhwalker.modelup.container;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

import io.github.nhwalker.modelup.Model;
import io.github.nhwalker.modelup.container.ListenerSettings.ConcurrentListenerSettings;

class ModelContainerSync<T extends Model> extends ModelContainer<T> {
  private final Object writeLock;
  private volatile T value;

  ModelContainerSync(ParametersSync<T> builder) {
    super(builder);
    this.value = (builder.getInitialValue());
    Object lock = builder.getWriteLock();
    this.writeLock = lock == null ? this : lock;
  }

  @Override
  public T getValue() {
    return value;
  }

  @Override
  public void setValue(T value) {
    synchronized (writeLock) {
      T before = this.value;
      T after = postProcessing(value);
      this.value = after;
      sendChange(before, after);
    }
  }

  @Override
  public T update(UnaryOperator<T> update) {
    synchronized (writeLock) {
      T before = this.value;
      T after = postProcessing(update.apply(before));
      this.value = after;
      sendChange(before, after);
      return after;
    }
  }

  @Override
  public CompletableFuture<T> updateAsync(UnaryOperator<T> update) {
    return CompletableFuture.supplyAsync(() -> this.update(update));
  }

  @Override
  public T multiStageUpdate(List<UnaryOperator<T>> stages) {
    synchronized (writeLock) {
      T after = this.value;
      for (UnaryOperator<T> stage : stages) {
        after = update(stage);
      }
      return after;
    }
  }

  @Override
  public CompletableFuture<T> multiStageUpdateAsync(List<UnaryOperator<T>> stages) {
    return CompletableFuture.supplyAsync(() -> this.multiStageUpdate(stages));
  }

  @Override
  public ListenerRegistration registerConcurrentListener(ConcurrentListenerSettings settings, ModelListener<? super T> listener) {
    if (settings.getCallImmediatly()) {
      synchronized (writeLock) {
        return super.registerListener(settings, listener);
      }
    } else {
      return super.registerListener(settings, listener);
    }
  }

  @Override
  public ListenerRegistration registerListener(ListenerSettings settings, ModelListener<? super T> listener) {
    if (settings.getCallImmediatly()) {
      synchronized (writeLock) {
        return super.registerListener(settings, listener);
      }
    } else {
      return super.registerListener(settings, listener);
    }
  }
}