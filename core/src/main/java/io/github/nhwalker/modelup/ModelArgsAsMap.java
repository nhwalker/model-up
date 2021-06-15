package io.github.nhwalker.modelup;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Live map view of a {@link ModelArgs}.
 * <p>
 * This is a live view backed by the {@link ModelArgs} provided at construction,
 * so all edits made through this interface will be reflected in the provided
 * {@link ModelArgs}, and vice-versa.
 * <p>
 * Values associated with keys can be changed with
 * {@link #put(ModelKey, Object)}, {@link #putAll(Map)} and
 * {@link FieldEntry#setValue(Object)}. But new fields can not be added, and no
 * fields may be removed
 */
public class ModelArgsAsMap extends AbstractMap<ModelKey<?>, Object> {

  private final ModelArgs model;

  public ModelArgsAsMap(ModelArgs model) {
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

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public Object put(ModelKey<?> key, Object value) {
    Object cur = model.get(key);
    model.set((ModelKey) key, value);
    return cur;

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
          return new FieldEntry(key);
        }
      };
    }

    @Override
    public int size() {
      return model.fieldKeys().size();
    }

  }

  private class FieldEntry implements Map.Entry<ModelKey<?>, Object> {
    private final ModelKey<?> key;

    public FieldEntry(ModelKey<?> key) {
      this.key = Objects.requireNonNull(key);
    }

    @Override
    public ModelKey<?> getKey() {
      return key;
    }

    @Override
    public Object getValue() {
      return model.get(key);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object setValue(Object value) {
      Object cur = model.get(key);
      model.set((ModelKey) key, value);
      return cur;
    }

  }
}
