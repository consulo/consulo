/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.content.library;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author nik
 */
public class LibraryKind {
  private final String myKindId;
  private static final Map<String, LibraryKind> ourAllKinds = new ConcurrentHashMap<>();

  /**
   * @param kindId must be unique among all {@link LibraryType} and {@link LibraryPresentationProvider} implementations
   */
  public LibraryKind(@Nonnull String kindId) {
    myKindId = kindId;
    if (ourAllKinds.containsKey(kindId)) {
      throw new IllegalArgumentException("Kind " + kindId + " is not unique");
    }
    ourAllKinds.put(kindId, this);
  }

  public final String getKindId() {
    return myKindId;
  }

  @Override
  public String toString() {
    return "LibraryKind:" + myKindId;
  }

  /**
   * @param kindId must be unique among all {@link LibraryType} and {@link LibraryPresentationProvider} implementations
   * @return new {@link LibraryKind} instance
   */
  @Nonnull
  public static LibraryKind create(@Nonnull String kindId) {
    return new LibraryKind(kindId);
  }

  @Nullable
  public static LibraryKind findById(@Nullable String kindId) {
    return kindId == null ? null : ourAllKinds.get(kindId);
  }
}
