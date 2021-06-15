package io.github.nhwalker.modelup;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Map;

import java.util.Set;

public final class Models {
  private Models() {
    throw new Error("No create");
  }

  public static Model emptyModel() {
    return EMPTY;
  }

  public static ModelAsMap toMap(Model model) {
    return new ModelAsMap(model);
  }

  public static ModelArgsAsMap toMap(ModelArgs args) {
    return new ModelArgsAsMap(args);
  }

  public static String prettyToString(Model model) {
    if (model == null) {
      return "null";
    }
    StringBuilder b = new StringBuilder();
    Set<Model> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    prettyToString(b, visited, model, "");
    return b.toString();
  }

  private static void prettyToString(StringBuilder str, Set<Model> visited, Model model, String indent) {
    if (!visited.add(model)) {
      str.append("<cycle-detected>");
      return;
    } else if (model.fieldKeys().isEmpty()) {
      str.append("{}");
    }
    String nl = System.lineSeparator();
    String newIndent = indent + "  ";
    str.append("{");
    for (Entry<ModelKey<?>, Object> e : Models.toMap(model).entrySet()) {
      str.append(nl).append(newIndent);
      str.append(e.getKey()).append(": ");
      if (e.getValue() instanceof Model) {
        prettyToString(str, visited, (Model) e.getValue(), newIndent);
      } else {
        str.append(e.getValue());
      }
    }
    str.append(nl).append(indent).append("}");
  }

  public static Set<ModelKey<?>> differences(Model a, Model b) {
    return differences(DiffStyle.ALL_FIELDS, a, b);
  }

  public static Set<ModelKey<?>> differences(DiffStyle style, Model a, Model b) {
    return differences(style.keys(a, b), a, b);
  }

  public static Map<ModelKey<?>, ChangedValue<?>> differencesMap(Model a, Model b) {
    return differencesMap(DiffStyle.ALL_FIELDS, a, b);
  }

  public static Map<ModelKey<?>, ChangedValue<?>> differencesMap(DiffStyle style, Model a, Model b) {
    return differencesMap(style.keys(a, b), a, b);
  }

  public static Set<ModelKey<?>> differences(Set<ModelKey<?>> fields, Model a, Model b) {

    Model first = a == null ? emptyModel() : a;
    Model second = b == null ? emptyModel() : b;

    LinkedHashSet<ModelKey<?>> different = new LinkedHashSet<>();
    for (ModelKey<?> key : fields) {
      Object aValue = first.get(key);
      Object bValue = second.get(key);
      if (!Objects.equals(aValue, bValue)) {
        different.add(key);
      }
    }

    return Collections.unmodifiableSet(different);
  }

  public static Map<ModelKey<?>, ChangedValue<?>> differencesMap(Set<ModelKey<?>> fields, Model a, Model b) {

    Model first = a == null ? emptyModel() : a;
    Model second = b == null ? emptyModel() : b;

    LinkedHashMap<ModelKey<?>, ChangedValue<?>> different = new LinkedHashMap<>();
    for (ModelKey<?> key : fields) {
      Object aValue = first.get(key);
      Object bValue = second.get(key);
      if (!Objects.equals(aValue, bValue)) {
        different.put(key, new ChangedValue<>(aValue, bValue));
      }
    }

    return Collections.unmodifiableMap(different);
  }

  private static final Model EMPTY = new Model() {
    @Override
    public boolean hasField(ModelKey<?> key) {
      return false;
    }

    @Override
    public Set<ModelKey<?>> fieldKeys() {
      return Collections.emptySet();
    }

    public <T> T get(ModelKey<T> key) {
      throw new IllegalArgumentException("No such key " + key);
    }
  };

  public enum DiffStyle {
    ALL_FIELDS {
      @Override
      public Set<ModelKey<?>> keys(Model a, Model b) {
        Set<ModelKey<?>> keys = new LinkedHashSet<>();
        if (a != null) {
          keys.addAll(a.fieldKeys());
        }
        if (b != null) {
          keys.addAll(b.fieldKeys());
        }
        return keys;
      }
    },
    IGNORE_MISSING_FIELDS {
      @Override
      public Set<ModelKey<?>> keys(Model a, Model b) {
        Set<ModelKey<?>> keys = new LinkedHashSet<>();
        if (a == null || b == null) {
          return keys;
        }
        keys.addAll(a.fieldKeys());
        keys.retainAll(b.fieldKeys());
        return keys;
      }
    },
    FIRST_OBJECT_FIELDS {
      @Override
      public Set<ModelKey<?>> keys(Model a, Model b) {
        return a == null ? Collections.emptySet() : a.fieldKeys();
      }
    },
    SECOND_OBJECT_FIELDS {
      @Override
      public Set<ModelKey<?>> keys(Model a, Model b) {
        return b == null ? Collections.emptySet() : b.fieldKeys();
      }
    };

    public abstract Set<ModelKey<?>> keys(Model a, Model b);
  }
}
