package io.github.nhwalker.modelup;

import java.util.function.Consumer;

public interface ModelWither<T extends Model, P> extends Model {

  T with(Consumer<? extends P> config);

}
