package io.github.nhwalker.modelup;

import java.util.Set;

/**
 * Describes a data model with enumerated keys
 */
public interface Model {

  /**
   * Check if this model has a field that matches this key
   * 
   * @param key the key check
   * @return {@code true} if there is a field in this model that matches the key's
   *         name
   */
  boolean hasField(ModelKey<?> key);

  /**
   * Get the value associated with the key
   * 
   * @param <T>
   * @param key the key whose associated value is to be returned
   * @return value associated with the key
   * @throws IllegalArgumentException if key is not present in this model
   *                                  ({@link #hasField(ModelKey)} is
   *                                  {@code false})
   */
  <T> T get(ModelKey<T> key);

  /**
   * @return the set of all keys associated with this model
   */
  Set<ModelKey<?>> fieldKeys();
}
