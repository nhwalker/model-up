package io.github.nhwalker.modelup;

import java.util.function.Consumer;

public interface ModelWither<T extends Model, A> extends Model {

  T withChange(Consumer<? extends A> config);

}
