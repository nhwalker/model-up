package io.github.nhwalker.modelup;

/**
 * An event with a single value as a payload
 * <p>
 * Note that equality of events is identity based and not value based to reflect
 * that events may be fired with the same value more than once.
 */
public final class ValueEvent<T> extends Event {
  private final T value;

  public ValueEvent(T value) {
    this.value = value;
  }

  public T getValue() {
    return value;
  }

  @Override
  public String toString() {
    return super.toString() + "[" + value + "]";
  }
}
