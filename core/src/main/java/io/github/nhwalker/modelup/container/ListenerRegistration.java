package io.github.nhwalker.modelup.container;

/**
 * Handle that can be used to unregister it's associated listener
 */
@FunctionalInterface
public interface ListenerRegistration {

  /**
   * Unregister the associated listener
   */
  void unregister();
}