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
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public class LocalizeLibraryImpl {
  private final Map<String, LocalizeKeyAsValueImpl> myKeys = new HashMap<>();

  private final String myPluginId;
  private final String myId;

  public LocalizeLibraryImpl(String pluginId, String id, Map<String, LocalizeKeyAsValueImpl> keys) {
    myPluginId = pluginId;
    myId = id;
  }

  @Nonnull
  @Override
  public String toString() {
    return myPluginId + ":" + myId;
  }
}
