package io.github.nhwalker.modelup;

import java.util.Set;

/**
 * Methods common to{@link Model} and {@link ModelArgs}
 */
public interface ModelOrArgs {

  /**
   * Check if this object has a field that matches the key
   * <p>
   * This does not make any claim about the value of the field, and the field may
   * be associated to a {@code null} value.
   * <p>
   * This should be the same as calling {@code fieldKeys().contains(key)}, but may
   * be more efficient.
   * 
   * @param key the key check
   * @return {@code true} if this object has the field
   */
  boolean hasField(ModelKey<?> key);

  /**
   * Get the value associated with the key
   * 
   * @param <T>
   * @param key the key whose associated value is to be returned
   * @return value the value associated with the key
   * @throws IllegalArgumentException if {@code key} does not belong to this
   *                                  object ({@link #hasField(ModelKey)} is
   *                                  {@code false})
   */
  <T> T get(ModelKey<T> key);

  /**
   * @return the set of all keys associated with this model
   */
  Set<ModelKey<?>> fieldKeys();

}
