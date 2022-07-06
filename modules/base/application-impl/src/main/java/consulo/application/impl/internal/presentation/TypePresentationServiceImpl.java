/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.application.impl.internal.presentation;

import consulo.annotation.component.ServiceImpl;
import consulo.application.presentation.TypePresentationProvider;
import consulo.application.presentation.TypePresentationService;
import consulo.ui.image.Image;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
@Singleton
@ServiceImpl
public class TypePresentationServiceImpl extends TypePresentationService {

  @Override
  @SuppressWarnings("unchecked")
  public Image getIcon(@Nonnull Object o) {
    TypePresentationProvider provider = TypePresentationProvider.getPresentationProvider(o);
    if (provider != null) {
      return provider.getIcon(o);
    }
    return null;
  }

  @Override
  @Nullable
  @SuppressWarnings("unchecked")
  public Image getTypeIcon(Class type) {
    TypePresentationProvider provider = TypePresentationProvider.getPresentationProvider(type);
    if (provider != null) {
      return provider.getIcon(null);
    }
    return null;
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public String getPresentableName(Object o) {
    TypePresentationProvider provider = TypePresentationProvider.getPresentationProvider(o);
    if (provider != null) {
      return provider.getName(o);
    }
    return null;
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public String getTypeName(Object o) {
    TypePresentationProvider provider = TypePresentationProvider.getPresentationProvider(o);
    if (provider != null) {
      return provider.getTypeName(o);
    }
    return null;
  }

  public TypePresentationServiceImpl() {
  }
}
