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
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.content.OrderRootType;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.DocumentationOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.externalSystem.model.project.LibraryPathType;
import consulo.externalSystem.service.project.ExternalLibraryPathTypeMapper;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Denis Zhdanov
 * @since 1/17/13 3:55 PM
 */
@Singleton
@ServiceImpl
public class ExternalLibraryPathTypeMapperImpl implements ExternalLibraryPathTypeMapper {
    private final Map<LibraryPathType, OrderRootType> myMapping = new EnumMap<>(LibraryPathType.class);

    @Inject
    public ExternalLibraryPathTypeMapperImpl(@Nonnull Application application) {
        ExtensionPoint<OrderRootType> point = application.getExtensionPoint(OrderRootType.class);

        for (LibraryPathType type : LibraryPathType.values()) {
            // switch protects us for new rows
            Class<? extends OrderRootType> orderRootType = switch (type) {
                case BINARY -> BinariesOrderRootType.class;
                case SOURCE -> SourcesOrderRootType.class;
                case DOC -> DocumentationOrderRootType.class;
            };

            myMapping.put(type, point.findExtensionOrFail(orderRootType));
        }
    }

    @Override
    public OrderRootType map(LibraryPathType type) {
        return Objects.requireNonNull(myMapping.get(type));
    }
}
