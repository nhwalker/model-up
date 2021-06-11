package io.github.nhwalker.modelup;

/**
 * An event indicating something has happened.
 * <p>
 * Useful for adding an "event bus" to a model where you want to inform
 * listeners to the model's container that an event happened, but are not really
 * concerned with storing any particular state in the model associated with the
 * event.
 * <p>
 * {@link #equals(Object)} and {@link #hashCode()} are identify based for all
 * events since events that are the same in quality may be occur repeatedly and
 * should be considered discrete events.
 */
public class Event {

  public Event() {
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public String toString() {
    return getClass().getName();
  }
}
