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
import consulo.component.extension.ByClassGrouper;
import consulo.component.extension.ExtensionPointCacheKey;

import org.jspecify.annotations.Nullable;
import java.util.function.Function;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ItemPresentationProvider<T extends NavigationItem> {
  ExtensionPointCacheKey<ItemPresentationProvider, Function<Class, ItemPresentationProvider>> KEY =
          ExtensionPointCacheKey.create("ItemPresentationProvider", ByClassGrouper.build(ItemPresentationProvider::getItemClass));

  @SuppressWarnings("unchecked")
  public static <T extends NavigationItem> @Nullable ItemPresentationProvider<T> getItemPresentationProvider(T element) {
    Function<Class, ItemPresentationProvider> call = Application.get().getExtensionPoint(ItemPresentationProvider.class).getOrBuildCache(KEY);
    return call.apply(element.getClass());
  }

  public static @Nullable ItemPresentation getItemPresentation(NavigationItem element) {
    ItemPresentationProvider<NavigationItem> provider = getItemPresentationProvider(element);
    return provider != null ? provider.getPresentation(element) : null;
  }

  
  Class<T> getItemClass();

  
  ItemPresentation getPresentation(T item);
}
