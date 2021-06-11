package io.github.nhwalker.modelup;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class ModelAsMap extends AbstractMap<ModelKey<?>, Object> {

  private final Model model;

  public ModelAsMap() {
    this(Models.emptyModel());
  }

  public ModelAsMap(Model model) {
    this.model = Objects.requireNonNull(model);
  }

  private ModelAsEntrySet entrySet = null;

  @Override
  public Set<Entry<ModelKey<?>, Object>> entrySet() {
    return entrySet == null ? (entrySet = new ModelAsEntrySet()) : entrySet;
  }

  @Override
  public Set<ModelKey<?>> keySet() {
    return model.fieldKeys();
  }

  @Override
  public boolean containsKey(Object key) {
    return model.fieldKeys().contains(key);
  }

  @Override
  public Object get(Object key) {
    if (this.containsKey(key)) {
      return model.get((ModelKey<?>) key);
    }
    return null;
  }

  private class ModelAsEntrySet extends AbstractSet<Entry<ModelKey<?>, Object>> {

    @Override
    public Iterator<Entry<ModelKey<?>, Object>> iterator() {
      return new Iterator<Entry<ModelKey<?>, Object>>() {
        private final Iterator<ModelKey<?>> iter = model.fieldKeys().iterator();

        @Override
        public boolean hasNext() {
          return iter.hasNext();
        }

        @Override
        public Entry<ModelKey<?>, Object> next() {
          ModelKey<?> key = iter.next();
          return new AbstractMap.SimpleImmutableEntry<>(key, model.get(key));
        }
      };
    }

    @Override
    public int size() {
      return model.fieldKeys().size();
    }

  }

}
