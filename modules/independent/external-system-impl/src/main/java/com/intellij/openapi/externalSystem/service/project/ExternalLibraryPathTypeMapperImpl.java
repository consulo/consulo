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
package com.intellij.openapi.externalSystem.service.project;

import com.intellij.openapi.externalSystem.model.project.LibraryPathType;
import com.intellij.openapi.roots.OrderRootType;
import consulo.roots.types.BinariesOrderRootType;
import consulo.roots.types.DocumentationOrderRootType;
import consulo.roots.types.SourcesOrderRootType;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Denis Zhdanov
 * @since 1/17/13 3:55 PM
 */
@Singleton
public class ExternalLibraryPathTypeMapperImpl implements ExternalLibraryPathTypeMapper {

  private final Map<LibraryPathType, OrderRootType> myMapping = new EnumMap<LibraryPathType, OrderRootType>(LibraryPathType.class);

  @Inject
  ExternalLibraryPathTypeMapperImpl() {
    myMapping.put(LibraryPathType.BINARY, BinariesOrderRootType.getInstance());
    myMapping.put(LibraryPathType.SOURCE, SourcesOrderRootType.getInstance());
    myMapping.put(LibraryPathType.DOC, DocumentationOrderRootType.getInstance());
    assert LibraryPathType.values().length == myMapping.size();
  }

  @Nonnull
  @Override
  public OrderRootType map(@Nonnull LibraryPathType type) {
    return myMapping.get(type);
  }
}
