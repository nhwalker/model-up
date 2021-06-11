package io.github.nhwalker.modelup.container;

import io.github.nhwalker.modelup.Model;

/**
 * Describes a listener to a {@link ModelContainer}
 * 
 * @param <T> the model type
 */
@FunctionalInterface
public interface ModelListener<T extends Model> {

  /**
   * Called when the model changes
   * 
   * @param event the change that occured
   */
  void onModelChanged(ChangeEvent<? extends T> event);
}
