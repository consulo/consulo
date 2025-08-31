/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.language.ast.IElementType;
import consulo.util.collection.MultiMap;
import consulo.util.lang.Trinity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;

/**
 * @author peter
 */
public class PrattRegistry {
  private final MultiMap<IElementType, Trinity<Integer, PathPattern, TokenParser>> myMap = new MultiMap<IElementType, Trinity<Integer, PathPattern, TokenParser>>();

  public void registerParser(@Nonnull IElementType type, int priority, TokenParser parser) {
    registerParser(type, priority, PathPattern.path(), parser);
  }

  public void registerParser(@Nonnull IElementType type, int priority, PathPattern pattern, TokenParser parser) {
    myMap.putValue(type, new Trinity<Integer, PathPattern, TokenParser>(priority, pattern, parser));
  }

  @Nonnull
  public Collection<Trinity<Integer, PathPattern, TokenParser>> getParsers(@Nullable IElementType type) {
    return myMap.get(type);
  }
}
