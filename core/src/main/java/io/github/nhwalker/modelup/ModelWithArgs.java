package io.github.nhwalker.modelup;

import java.util.function.Consumer;

public interface ModelWithArgs<M, A> extends Model {

  M withUpdate(Consumer<? super A> config);

}
