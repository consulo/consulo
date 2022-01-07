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

package consulo.psi.injection;

import com.intellij.openapi.components.ServiceManager;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-10-23
 */
@Singleton
public class LanguageSupportCache {
  public static LanguageSupportCache getInstance() {
    return ServiceManager.getService(LanguageSupportCache.class);
  }

  private Map<String, LanguageInjectionSupport> myInjectors = new HashMap<>();

  public LanguageSupportCache() {
    for (LanguageInjectionSupport support : LanguageInjectionSupport.EP_NAME.getExtensionList()) {
      myInjectors.put(support.getId(), support);
    }
  }

  @Nonnull
  public Collection<LanguageInjectionSupport> getAllSupports() {
    return myInjectors.values();
  }

  @Nullable
  public LanguageInjectionSupport getSupport(String id) {
    return myInjectors.get(id);
  }

  @Nonnull
  public Set<String> getAllSupportIds() {
    return myInjectors.keySet();
  }
}
