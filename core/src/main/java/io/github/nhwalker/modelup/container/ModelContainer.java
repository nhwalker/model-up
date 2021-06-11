package io.github.nhwalker.modelup.container;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import io.github.nhwalker.modelup.Model;
import io.github.nhwalker.modelup.container.ListenerSettings.ConcurrentListenerSettings;

// TODO
public abstract class ModelContainer<T extends Model> implements ModelListenerManagement<T> {

  public static <T extends Model> ModelContainer<T> create(Consumer<Parameters<T>> config) {
    Parameters<T> c = new Parameters<>();
    config.accept(c);
    return new ModelContainerBasic<>(c);
  }

  public static <T extends Model> ModelContainer<T> createSync(Consumer<ParametersSync<T>> config) {
    ParametersSync<T> c = new ParametersSync<>();
    config.accept(c);
    return new ModelContainerSync<>(c);
  }

  public static <T extends Model> ModelContainer<T> createAsync(Consumer<ParametersAsync<T>> config) {
    ParametersAsync<T> c = new ParametersAsync<>();
    config.accept(c);
    return new ModelContainerAsync<>(c);
  }

  /**
   * @return the current value
   */
  public abstract T getValue();

  /**
   * Change the current value
   * 
   * @param value the new value
   */
  public abstract void setValue(T value);

  /**
   * Atomically update the current value
   * 
   * @param update the update operation to apply
   * @return the updated value
   */
  public abstract T update(UnaryOperator<T> update);

  public T update(UnaryOperator<T> stage1, UnaryOperator<T> stage2) {
    return multiStageUpdate(Arrays.asList(stage1, stage2));
  }

  /**
   * Queue an atomic update of the current value
   * <p>
   * For {@link Builder#async() Asynchronous containers} will be queued to run on
   * the executure provided during {@link Builder#async(Executor)} or the
   * {@link ForkJoinPool#commonPool()} if {@link Builder#async()} was used.
   * <p>
   * For {@link Builder#synchronize() Synchronized containers} it will be queued
   * to run on the {@link ForkJoinPool#commonPool()}.
   * <p>
   * For simple containers which have no thread safety, it will executed
   * immediately and an already completed future will be returned. (With no thread
   * safety in a simple container,
   * 
   * 
   * 
   * @param update
   * @return
   */
  public abstract CompletableFuture<T> updateAsync(UnaryOperator<T> update);

  public CompletableFuture<T> updateAsync(UnaryOperator<T> stage1, UnaryOperator<T> stage2) {
    return multiStageUpdateAsync(Arrays.asList(stage1, stage2));
  }

  public abstract T multiStageUpdate(List<UnaryOperator<T>> stages);

  public abstract CompletableFuture<T> multiStageUpdateAsync(List<UnaryOperator<T>> stages);

  public void removeAllListeners() {
    listenerManager.clearListeners();
  }

  private final PostProcessing<T> postProcessing;
  private final ModelListenerManager<T> listenerManager;

  protected ModelContainer(Parameters<T> params) {
    PostProcessing<T> post = params.getPostProcessing();
    this.postProcessing = post == null ? PostProcessing.identity() : post;
    this.listenerManager = new ModelListenerManager<T>(params.getIsConcurrentListeners(),
        params.getDefaultConcurrentListenerSettings()) {
      protected T getCurrentState() {
        return ModelContainer.this.getValue();
      };
    };
  }

  @Override
  public ListenerRegistration registerListener(ModelListener<? super T> listener) {
    return this.listenerManager.registerListener(listener);
  }

  @Override
  public ListenerRegistration registerConcurrentListener(ConcurrentListenerSettings settings, ModelListener<? super T> listener) {
    return this.listenerManager.registerListener(settings, listener);
  }

  @Override
  public ListenerRegistration registerListener(ListenerSettings settings, ModelListener<? super T> listener) {
    return this.listenerManager.registerListener(settings, listener);
  }

  @Override
  public void unregisterListener(ModelListener<? super T> listener) {
    this.listenerManager.unregisterListener(listener);
  }

  protected T postProcessing(T value) {
    return postProcessing.apply(value);
  }

  protected void sendChange(T before, T after) {
    ChangeEvent<T> change = ChangeEvent.create(before, after);
    listenerManager.send(change);
  }

  public static class Parameters<T extends Model> {

    private PostProcessing<T> postProcessing = null;
    private boolean concurrentListeners = false;
    private ConcurrentListenerSettings defaultConcurrentListenerSettings = null;
    private T initialValue = null;

    public boolean getIsConcurrentListeners() {
      return concurrentListeners;
    }

    public ConcurrentListenerSettings getDefaultConcurrentListenerSettings() {
      return defaultConcurrentListenerSettings;
    }

    public PostProcessing<T> getPostProcessing() {
      return postProcessing;
    }

    public T getInitialValue() {
      return initialValue;
    }

    public void concurrentListeners() {
      this.concurrentListeners = true;
    }

    public void defaultConcurrentListenerSettings(ConcurrentListenerSettings settings) {
      this.defaultConcurrentListenerSettings = settings;
    }

    public void initialValue(T initialValue) {
      this.initialValue = initialValue;
    }

    public void addPostProcessing(PostProcessing<T> post) {
      this.postProcessing = this.postProcessing == null ? post : this.postProcessing.andThen(post);
    }

    public void requireNonNull() {
      addPostProcessing(PostProcessing.requireNonNull());
    }

  }

  public static class ParametersSync<T extends Model> extends Parameters<T> {

    private Object writeLock = null;

    public void writeLock(Object writeLock) {
      this.writeLock = writeLock;
    }

    public Object getWriteLock() {
      return writeLock;
    }
  }

  public static class ParametersAsync<T extends Model> extends Parameters<T> {
    private Executor asyncExecutor = null;

    public void asyncExecutor(Executor executor) {
      this.asyncExecutor = executor;
    }

    public Executor getAsyncExecutor() {
      return asyncExecutor;
    }

  }
}
