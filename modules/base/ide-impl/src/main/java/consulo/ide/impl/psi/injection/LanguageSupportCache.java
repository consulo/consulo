/*
 * Copyright 2013-2019 must-be.org
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

package consulo.ide.impl.psi.injection;

import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-10-23
 */
public class LanguageSupportCache {
  private static final LanguageSupportCache ourInstance = new LanguageSupportCache();

  public static LanguageSupportCache getInstance() {
    return ourInstance;
  }

  private static final ExtensionPointCacheKey<LanguageInjectionSupport, Map<String, LanguageInjectionSupport>> CACHE_KEY =
    ExtensionPointCacheKey.groupBy("LanguageInjectionSupportCache", LanguageInjectionSupport::getId);


  @Nonnull
  public Collection<LanguageInjectionSupport> getAllSupports() {
    return Application.get().getExtensionPoint(LanguageInjectionSupport.class).getOrBuildCache(CACHE_KEY).values();
  }

  @Nullable
  public LanguageInjectionSupport getSupport(String id) {
    return Application.get().getExtensionPoint(LanguageInjectionSupport.class).getOrBuildCache(CACHE_KEY).get(id);
  }

  @Nonnull
  public Set<String> getAllSupportIds() {
    return Application.get().getExtensionPoint(LanguageInjectionSupport.class).getOrBuildCache(CACHE_KEY).keySet();
  }
}
