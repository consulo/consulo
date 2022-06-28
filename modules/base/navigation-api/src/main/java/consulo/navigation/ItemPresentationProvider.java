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
package consulo.navigation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.application.extension.ByClassGrouper;
import consulo.component.extension.ExtensionPointCacheKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ItemPresentationProvider<T extends NavigationItem> {
  ExtensionPointCacheKey<ItemPresentationProvider, Function<Class, ItemPresentationProvider>> KEY =
          ExtensionPointCacheKey.create("ItemPresentationProvider", ByClassGrouper.build(ItemPresentationProvider::getItemClass));

  @Nullable
  @SuppressWarnings("unchecked")
  public static <T extends NavigationItem> ItemPresentationProvider<T> getItemPresentationProvider(@Nonnull T element) {
    Function<Class, ItemPresentationProvider> call = Application.get().getExtensionPoint(ItemPresentationProvider.class).getOrBuildCache(KEY);
    return call.apply(element.getClass());
  }

  @Nullable
  public static ItemPresentation getItemPresentation(NavigationItem element) {
    final ItemPresentationProvider<NavigationItem> provider = getItemPresentationProvider(element);
    return provider != null ? provider.getPresentation(element) : null;
  }

  @Nonnull
  Class<T> getItemClass();

  @Nonnull
  ItemPresentation getPresentation(T item);
}
