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

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
class LocalizeLibraryImpl {
  public static final String DEFAULT_LOCALE = "en";

  private final String myPluginId;
  private final String myId;
  private final Class<?> myLocalizeClass;

  private LocalizeFileLoader myLoader;

  public LocalizeLibraryImpl(String pluginId, String id, Class<?> localizeClass, Map<String, LocalizeKeyImpl> keys) {
    myPluginId = pluginId;
    myId = id;
    myLocalizeClass = localizeClass;

    for (LocalizeKeyImpl key : keys.values()) {
      key.setLibrary(this);
    }
  }

  public void loadLocale(String locale) {
    LocalizeFileLoader loader = new LocalizeFileLoader("/localize/" + myPluginId + "/" + myId + ".yaml", myLocalizeClass.getClassLoader());

    loader.parse();

    myLoader = loader;
  }

  @Nonnull
  public String getText(String id) {
    if (myLoader == null) {
      return id;
    }

    LocalizeFileLoader.LocalizeValueInstance valueInstance = myLoader.get(id);

    return valueInstance.getText();
  }

  @Nonnull
  @Override
  public String toString() {
    return myPluginId + ":" + myId;
  }
}
