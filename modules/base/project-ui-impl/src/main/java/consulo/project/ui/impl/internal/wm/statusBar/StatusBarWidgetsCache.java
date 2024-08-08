/*
 * Copyright 2013-2024 consulo.io
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
package consulo.project.ui.impl.internal.wm.statusBar;

import consulo.component.extension.ExtensionPointCacheKey;
import consulo.logging.Logger;
import consulo.project.ui.wm.StatusBarWidgetFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author VISTALL
 * @since 04.05.2024
 */
public record StatusBarWidgetsCache(LinkedHashMap<String, StatusBarWidgetFactory> keyMap, List<String> order) {
  private static final Logger LOG = Logger.getInstance(StatusBarWidgetsCache.class);

  static final ExtensionPointCacheKey<StatusBarWidgetFactory, StatusBarWidgetsCache> CACHE_KEY =
    ExtensionPointCacheKey.create("StatusBarWidgetsCache", walker -> {
      LinkedHashMap<String, StatusBarWidgetFactory> map = new LinkedHashMap<>();
      List<String> order = new ArrayList<>();

      walker.walk(factory -> {
        StatusBarWidgetFactory putVal = map.putIfAbsent(factory.getId(), factory);
        if (putVal != null) {
          LOG.error("Duplicate status bar widget with id " + factory.getId() + ". Classes: " + putVal.getClass() + "/" + factory.getClass());
          return;
        }

        order.add(factory.getId());
      });

      // reverse order for weight sort
      Collections.reverse(order);
      return new StatusBarWidgetsCache(map, order);
    });
}
