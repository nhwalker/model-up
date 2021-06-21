package io.github.nhwalker.modelup;

/**
 * Describes an mutable data model with enumerated keys
 * <p>
 * The set of keys for each object is immutable, usually matching the exact key
 * set of the model associated with these arguments
 */
public interface ModelArgs extends ModelOrArgs {

  void copy(ModelOrArgs toCopy);

  /**
   * Copies values from fields in {@toCopy} that are also in this
   * {@code ModelArgs}.
   * <p>
   * Fields not present in this {@code ModelArgs} according to
   * {@link #hasField(ModelKey)} will be ignored.
   * 
   * @param toCopy the model to copy
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  default void copyLienent(ModelOrArgs toCopy) {
    for (ModelKey<?> key : toCopy.fieldKeys()) {
      if (hasField(key)) {
        set((ModelKey) key, toCopy.get(key));
      }
    }
  }

  /**
   * Set the specified key to the specified value
   * 
   * @param <T>   the value type
   * @param key   key to set
   * @param value value to set
   * @throws IllegalArgumentException if {@code key} does not belong to this
   *                                  object ({@link #hasField(ModelKey)} is
   *                                  {@code false})
   */
  <T> void set(ModelKey<T> key, T value);
}
