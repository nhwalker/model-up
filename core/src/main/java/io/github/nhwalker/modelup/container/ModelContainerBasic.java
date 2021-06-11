package io.github.nhwalker.modelup.container;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

import io.github.nhwalker.modelup.Model;

class ModelContainerBasic<T extends Model> extends ModelContainer<T> {
  private T value;

  ModelContainerBasic(Parameters<T> params) {
    super(params);
    T init = postProcessing(params.getInitialValue());
    this.value = init;
  }

  @Override
  public T getValue() {
    return value;
  }

  @Override
  public void setValue(T value) {
    T before = this.value;
    T after = postProcessing(value);
    this.value = after;
    sendChange(before, after);
  }

  @Override
  public T update(UnaryOperator<T> update) {
    T before = this.value;
    T after = postProcessing(update.apply(before));
    this.value = after;
    sendChange(before, after);
    return after;
  }

  @Override
  public CompletableFuture<T> updateAsync(UnaryOperator<T> update) {
    CompletableFuture<T> future = new CompletableFuture<T>();
    try {
      future.complete(this.update(update));
    } catch (Exception e) {
      future.completeExceptionally(e);
    }

    return future;
  }

  @Override
  public T multiStageUpdate(List<UnaryOperator<T>> stages) {
    T after = this.value;
    for (UnaryOperator<T> stage : stages) {
      after = update(stage);
    }
    return after;
  }

  @Override
  public CompletableFuture<T> multiStageUpdateAsync(List<UnaryOperator<T>> stages) {
    CompletableFuture<T> future = new CompletableFuture<T>();
    try {
      future.complete(this.multiStageUpdate(stages));
    } catch (Exception e) {
      future.completeExceptionally(e);
    }
    return future;
  }
}