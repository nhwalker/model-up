package io.github.nhwalker.modelup.container;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import io.github.nhwalker.modelup.ChangedValue;
import io.github.nhwalker.modelup.Event;
import io.github.nhwalker.modelup.MapModel;
import io.github.nhwalker.modelup.Model;
import io.github.nhwalker.modelup.ModelKey;
import io.github.nhwalker.modelup.Models;

/**
 * Describes a change to a model
 *
 * @param <T> the model type
 */
public final class ChangeEvent<T extends Model> extends Event {

  /**
   * Create a new change
   * 
   * @param <T>    the model type
   * @param before the value before the change
   * @param after  the value after the change
   * @return
   */
  public static <T extends Model> ChangeEvent<T> create(T before, T after) {
    return new ChangeEvent<>(before, after);
  }

  /**
   * Merge two change events into a single change event
   * 
   * @param <T>    the model type
   * @param first  the first change
   * @param second the second change
   * @return
   */
  public static <T extends Model> ChangeEvent<T> merge(ChangeEvent<? extends T> first,
      ChangeEvent<? extends T> second) {
    return create(first.getBefore(), second.getAfter());
  }

  private final T before;
  private final T after;
  private final Map<ModelKey<?>, ChangedValue<?>> changes;
  private MapModel changedMap;

  private ChangeEvent(T before, T after) {
    this.before = before;
    this.after = after;
    this.changes = Models.differencesMap(before, after);
  }

  /**
   * @return {@code true} if this event has changes, {@code false} if nothing has
   *         changed
   */
  public boolean hasChanges() {
    return !this.changes.isEmpty();
  }

  /**
   * @return the value before the change
   */
  public T getBefore() {
    return before;
  }

  /**
   * @return the value after the change
   */
  public T getAfter() {
    return after;
  }

  /**
   * @return all keys that have changed
   */
  public Set<ModelKey<?>> getChangedKeys() {
    return changes.keySet();
  }

  /**
   * @return the changes that occurred
   */
  public Map<ModelKey<?>, ChangedValue<?>> getChanges() {
    return changes;
  }

  /**
   * @return a map of all changed-fields to their new values
   */
  public MapModel getChangedValues() {
    if (changedMap == null) {
      if (after == null) {
        this.changedMap = MapModel.EMPTY;
      } else {
        this.changedMap = MapModel.newBuilder(after).retainAll(changes.keySet()).build();
      }
    }
    return changedMap;
  }

  /**
   * Test if a field has changed
   * 
   * @param key the key to check if changed in this event
   * @return {@code true} if the value associated with the key has changed
   */
  public boolean hasChanged(ModelKey<?> key) {
    return changes.containsKey(key);
  }

  /**
   * Test if any of the provided keys have changed
   * 
   * @param keys the keys to check
   * @return {@code true} if any of {@code keys} have changed
   */
  public boolean hasAnyChanged(Collection<ModelKey<?>> keys) {
    for (ModelKey<?> k : keys) {
      if (hasChanged(k)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Test if all the provided keys have changed
   * 
   * @param keys the keys to check
   * @return {@code true} if all of the {@code keys} have changed
   */
  public boolean hasAllChanged(Collection<ModelKey<?>> keys) {
    return changes.keySet().containsAll(keys);
  }

  /**
   * Describes what changed in this event
   */
  @Override
  public String toString() {
    if (hasChanges()) {
      return "ChangeEvent" + changes;
    } else {
      return "ChangeEvent<no-change>";
    }
  }

}
