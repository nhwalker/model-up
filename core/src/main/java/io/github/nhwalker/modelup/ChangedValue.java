package io.github.nhwalker.modelup;

import java.util.Objects;

public class ChangedValue<T> {
  private final T before;
  private final T after;

  public ChangedValue(T before, T after) {
    this.before = before;
    this.after = after;
  }

  public T getBefore() {
    return before;
  }

  public T getAfter() {
    return after;
  }


  @Override
  public String toString() {
    return "ChangedValue [" + before + " => " + after + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hash(after, before);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ChangedValue<?> other = (ChangedValue<?>) obj;
    return Objects.equals(after, other.after) && Objects.equals(before, other.before);
  }
}
