package io.github.nhwalker.modelup;

import java.io.Serializable;
import java.util.Objects;

/**
 * A string based key indicating a field of a {@link Model}. Type information is
 * also included in the key to facilitate certain type-safe method calls, but it
 * is NOT used for key equality
 *
 * @param <T> the value type associated with this key
 */
public final class ModelKey<T> implements Serializable {
  private static final long serialVersionUID = -260166964904046331L;

  public static ModelKey<Object> of(String name) {
    return new ModelKey<>(name, Object.class);
  }

  public static <T> ModelKey<T> of(String name, Class<T> type) {
    return new ModelKey<>(name, type);
  }

  private final String name;
  private final Class<T> type;

  private ModelKey(String name, Class<T> type) {
    this.name = Objects.requireNonNull(name);
    this.type = Objects.requireNonNull(type);
  }

  public String name() {
    return name;
  }

  public Class<T> type() {
    return type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ModelKey<?> other = (ModelKey<?>) obj;
    return Objects.equals(name, other.name);
  }

  @Override
  public String toString() {
    return name;
  }
}
