/*
 * Copyright 2013-2019 consulo.io
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
package consulo.localize.impl;

import consulo.localize.Localize;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeLibraryBuilder;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
class LocalizeLibraryBuilderImpl implements LocalizeLibraryBuilder {
  private final String myPluginId;
  private final String myId;
  private final Class<?> myBundleClass;

  private Map<String, LocalizeKeyImpl> myKeys = new HashMap<>();

  public LocalizeLibraryBuilderImpl(String pluginId, @Nonnull Localize localize) {
    myPluginId = pluginId;
    myId = localize.getClass().getSimpleName();
    myBundleClass = localize.getClass();
  }

  @Nonnull
  @Override
  public LocalizeKey define(@Nonnull String id) {
    if (myKeys.containsKey(id)) {
      throw new IllegalArgumentException("duplicate id " + id);
    }
    LocalizeKeyImpl key = new LocalizeKeyImpl(id);
    myKeys.put(id, key);
    return key;
  }

  @Override
  public void finish() {
    Map<String, LocalizeKeyImpl> keys = myKeys;
    myKeys = null;

    LocalizeLibraryImpl library = new LocalizeLibraryImpl(myPluginId, myId, myBundleClass, keys);

    library.loadLocale(LocalizeLibraryImpl.DEFAULT_LOCALE);

    // TODO [VISTALL] register
  }
}
