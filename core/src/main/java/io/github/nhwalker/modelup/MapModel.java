package io.github.nhwalker.modelup;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MapModel implements Model {
  public static final MapModel EMPTY = newBuilder().build();

  public static final Builder newBuilder() {
    return new Builder();
  }

  public static final Builder newBuilder(Model copy) {
    return newBuilder().putAll(copy);
  }

  public static final MapModel copy(Model copy) {
    return new MapModel(Models.toMap(copy));
  }

  private final Map<ModelKey<?>, Object> values;

  private MapModel(Map<ModelKey<?>, Object> values) {
    this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
  }

  public Map<ModelKey<?>, Object> asMap() {
    return values;
  }

  @Override
  public Set<ModelKey<?>> fieldKeys() {
    return values.keySet();
  }

  @Override
  public <T> T get(ModelKey<T> key) {
    return key.type().cast(values.get(key));
  }

  @Override
  public boolean hasField(ModelKey<?> key) {
    return values.containsKey(key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(values);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MapModel other = (MapModel) obj;
    return Objects.equals(values, other.values);
  }

  @Override
  public String toString() {
    return values.toString();
  }

  public static class Builder {
    private final LinkedHashMap<ModelKey<?>, Object> values = new LinkedHashMap<>();

    public <T> Builder put(ModelKey<T> key, T value) {
      values.put(key, value);
      return this;
    }

    public <T> Builder putAll(MapModel map) {
      values.putAll(map.values);
      return this;
    }

    public <T> Builder putAll(Model fields) {
      if (fields instanceof MapModel) {
        putAll((MapModel) fields);
      } else {
        values.putAll(Models.toMap(fields));
      }
      return this;
    }

    public <T> Builder remove(ModelKey<?> key) {
      values.remove(key);
      return this;
    }

    public <T> Builder removeAll(Collection<ModelKey<?>> keys) {
      values.keySet().removeAll(keys);
      return this;
    }

    public <T> Builder retainAll(Collection<ModelKey<?>> keys) {
      values.keySet().retainAll(keys);
      return this;
    }

    public MapModel build() {
      return new MapModel(values);
    }

  }

}
