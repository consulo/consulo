/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.pratt;

import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.ast.IElementType;
import consulo.util.collection.SmartList;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * @author peter
 */
public class PathPattern {
  private final List<ElementPattern> myPath = new SmartList<ElementPattern>();

  private PathPattern() {
  }

  public static PathPattern path() {
    return new PathPattern();
  }

  public PathPattern up() {
    myPath.add(null);
    return this;
  }

  public PathPattern left() {
    return left(PlatformPatterns.elementType());
  }

  public PathPattern left(@Nonnull IElementType pattern) {
    return left(PlatformPatterns.elementType().equalTo(pattern));
  }

  public PathPattern left(@Nonnull ElementPattern pattern) {
    myPath.add(pattern);
    return this;
  }

  @NonNls
  public String toString() {
    return Arrays.toString(myPath.toArray()).replaceAll("null", "UP");
  }

  public boolean accepts(PrattBuilder builder) {
    ListIterator<IElementType> iterator = null;
    for (ElementPattern pattern : myPath) {
      if (builder == null) return false;

      if (iterator == null) {
        iterator = builder.getBackResultIterator();
      }

      if (pattern == null) {
        if (iterator.hasPrevious()) return false;
        builder = builder.getParent();
        iterator = null;
      } else {
        if (!iterator.hasPrevious()) return false;
        if (!pattern.accepts(iterator.previous())) return false;
      }
    }

    return true;
  }

}
