package io.github.nhwalker.modelup.container;

import java.util.Objects;
import java.util.function.UnaryOperator;

import io.github.nhwalker.modelup.Model;

/**
 * Describes an operation that performs either verifies or sanitizes a change
 * being made to a {@link ModelContainer}.
 * 
 * @param <T> the model type
 */
@FunctionalInterface
public interface PostProcessing<T extends Model> extends UnaryOperator<T> {

  public static PostProcessing<?> IDENTITY = x -> x;

  public static final PostProcessing<?> NON_NULL_POST_PROCESSING = Objects::requireNonNull;

  /**
   * A {@link PostProcessing} that throws a {@link NullPointerException} if the
   * model is {@code null}
   * 
   * @param <T> the model type
   * @return the described {@link PostProcessing}
   */
  @SuppressWarnings("unchecked")
  public static <T extends Model> PostProcessing<T> requireNonNull() {
    return (PostProcessing<T>) NON_NULL_POST_PROCESSING;
  }

  /**
   * A {@link PostProcessing} that does nothing and returns the input.
   * 
   * @param <T> the model type
   * @return the described {@link PostProcessing}
   */
  @SuppressWarnings("unchecked")
  public static <T extends Model> PostProcessing<T> identity() {
    return (PostProcessing<T>) IDENTITY;
  }

  /**
   * Create a new {@link PostProcessing} that does {@code this} then {@code after}
   * 
   * @param after the next step to execute
   * @return the described {@link PostProcessing}
   */
  default PostProcessing<T> andThen(PostProcessing<T> after) {
    if (after == IDENTITY) {
      return this;
    } else if (this == IDENTITY) {
      return after;
    }
    return x -> after.apply(apply(x));
  }
}