package io.github.nhwalker.modelup.container;

import java.util.AbstractQueue;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.github.nhwalker.modelup.Model;

/**
 * A queue for concurrent {@link ModelListener}s that folds unprocessed events
 * 
 * @param <T> the model type
 */
public class FoldingChangeEventQueue<T extends Model> extends AbstractQueue<ChangeEvent<T>> {

  private AtomicReference<ChangeEvent<T>> queued = new AtomicReference<>();

  @Override
  public boolean offer(ChangeEvent<T> e) {
    Objects.requireNonNull(e);
    queued.accumulateAndGet(e, FoldingChangeEventQueue::mergeOrNull);
    return true;
  }

  @Override
  public ChangeEvent<T> poll() {
    return queued.getAndSet(null);
  }

  @Override
  public ChangeEvent<T> peek() {
    return queued.get();
  }

  @Override
  public Iterator<ChangeEvent<T>> iterator() {
    ChangeEvent<T> e = peek();
    if (e == null) {
      return Collections.emptyIterator();
    } else {
      return Collections.singleton(e).iterator();
    }
  }

  @Override
  public int size() {
    return queued.get() != null ? 1 : 0;
  }

  private static <T extends Model> ChangeEvent<T> mergeOrNull(ChangeEvent<T> first, ChangeEvent<T> second) {
    final boolean acceptFirst = first != null && first.hasChanges();
    final boolean acceptSecond = second != null && second.hasChanges();
    if (acceptFirst && acceptSecond) {
      ChangeEvent<T> merged = ChangeEvent.merge(first, second);
      return merged.hasChanges() ? merged : null;
    } else if (acceptFirst) {
      return first;
    } else if (acceptSecond) {
      return second;
    }
    return null;
  }

}
