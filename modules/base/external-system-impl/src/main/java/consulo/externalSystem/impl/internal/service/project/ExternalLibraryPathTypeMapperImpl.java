/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.impl.internal.service.project;

import consulo.annotation.component.ServiceImpl;
import consulo.content.OrderRootType;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.DocumentationOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.externalSystem.model.project.LibraryPathType;
import consulo.externalSystem.service.project.ExternalLibraryPathTypeMapper;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author Denis Zhdanov
 * @since 1/17/13 3:55 PM
 */
@Singleton
@ServiceImpl
public class ExternalLibraryPathTypeMapperImpl implements ExternalLibraryPathTypeMapper {

  private static final Map<LibraryPathType, OrderRootType> MAPPINGS = new EnumMap<>(LibraryPathType.class);

  static {
    MAPPINGS.put(LibraryPathType.BINARY, BinariesOrderRootType.getInstance());
    MAPPINGS.put(LibraryPathType.SOURCE, SourcesOrderRootType.getInstance());
    MAPPINGS.put(LibraryPathType.DOC, DocumentationOrderRootType.getInstance());
    assert LibraryPathType.values().length == MAPPINGS.size();
  }

  @Nonnull
  @Override
  public OrderRootType map(@Nonnull LibraryPathType type) {
    return MAPPINGS.get(type);
  }
}
