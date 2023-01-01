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
package consulo.application.presentation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ByClassGrouper;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * @author Dmitry Avdeev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class TypePresentationProvider<T> {
  private static final ExtensionPointCacheKey<TypePresentationProvider, Function<Class, TypePresentationProvider>> KEY =
          ExtensionPointCacheKey.create("TypePresentationProvider", ByClassGrouper.build(TypePresentationProvider::getItemClass));

  @Nullable
  @SuppressWarnings("unchecked")
  public static <T> TypePresentationProvider<T> getPresentationProvider(@Nonnull T element) {
    return getPresentationProvider(element.getClass());
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public static <T> TypePresentationProvider<T> getPresentationProvider(@Nonnull Class typeClass) {
    Function<Class, TypePresentationProvider> call = Application.get().getExtensionPoint(TypePresentationProvider.class).getOrBuildCache(KEY);
    return call.apply(typeClass);
  }

  @Nonnull
  public abstract Class<T> getItemClass();

  @Nullable
  public String getName(T t) {
    return t.toString();
  }

  @Nullable
  public String getTypeName(T t) {
    return t.toString();
  }

  @Nullable
  public abstract Image getIcon(T t);
}
