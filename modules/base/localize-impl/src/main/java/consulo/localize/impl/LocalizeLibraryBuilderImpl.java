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

import consulo.localize.LocalizeKeyAsValue;
import consulo.localize.LocalizeLibraryBuilder;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public class LocalizeLibraryBuilderImpl implements LocalizeLibraryBuilder {
  private final String myPluginId;
  private final String myId;
  private final Class<?> myBundleClass;

  private Map<String, LocalizeKeyAsValueImpl> myKeys = new HashMap<>();

  public LocalizeLibraryBuilderImpl(String pluginId, String id, Class<?> bundleClass) {
    myPluginId = pluginId;
    myId = id;
    myBundleClass = bundleClass;
  }

  @Nonnull
  @Override
  public LocalizeKeyAsValue define(@Nonnull String id) {
    return new LocalizeKeyAsValueImpl(id);
  }

  @Override
  public void finish() {
    Map<String, LocalizeKeyAsValueImpl> keys = myKeys;
    myKeys = null;

    LocalizeFileLoader loader = new LocalizeFileLoader("en", "/localize/" + myPluginId + "/" + myId + ".yaml", myBundleClass.getClassLoader());

    LocalizeLibraryImpl library = new LocalizeLibraryImpl(myPluginId, myId, keys);
    // TODO [VISTALL] register
  }
}
