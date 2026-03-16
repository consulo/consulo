package consulo.ide.impl.idea.model.search.impl;


import java.util.Collection;
import java.util.List;

// from kotlin
final class IdTransformation implements XTransformation {
  public static IdTransformation INSTANCE = new IdTransformation();

  private IdTransformation() {
  }

  
  public Collection<XResult<Object>> apply(Object e) {
    return List.of(new ValueResult<>(e));
  }

  
  public String toString() {
    return "ID";
  }
}
