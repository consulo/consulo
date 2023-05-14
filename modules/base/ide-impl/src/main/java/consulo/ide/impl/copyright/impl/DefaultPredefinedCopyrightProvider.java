/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.copyright.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.copyright.PredefinedCopyrightProvider;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.io.FileUtil;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 28-Jun-22
 */
@ExtensionImpl
public class DefaultPredefinedCopyrightProvider implements PredefinedCopyrightProvider {
  private static final Logger LOG = Logger.getInstance(DefaultPredefinedCopyrightProvider.class);

  private Map<LocalizeValue, Supplier<String>> mySet = new LinkedHashMap<>();

  @Inject
  public DefaultPredefinedCopyrightProvider() {
    mySet.put(LocalizeValue.localizeTODO("Apache 2"), () -> getText("/copyright/Apache2.txt"));
    mySet.put(LocalizeValue.localizeTODO("MIT"), () -> getText("/copyright/MIT.txt"));
  }

  @Nonnull
  @Override
  public Map<LocalizeValue, String> getCopyrightTexts() {
    Map<LocalizeValue, String> set = new LinkedHashMap<>();
    for (Map.Entry<LocalizeValue, Supplier<String>> entry : mySet.entrySet()) {
      set.put(entry.getKey(), entry.getValue().get());
    }
    return set;
  }

  private static String getText(String url) {
    try {
      InputStream stream = DefaultPredefinedCopyrightProvider.class.getResourceAsStream(url);
      return FileUtil.loadTextAndClose(stream);
    }
    catch (IOException e) {
      LOG.warn(e);
    }
    return url;
  }
}
