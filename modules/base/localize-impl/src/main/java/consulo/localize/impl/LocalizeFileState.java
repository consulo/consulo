/*
 * Copyright 2013-2020 consulo.io
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

import consulo.localize.LocalizeKey;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2020-05-20
 */
class LocalizeFileState {
  private static final Logger LOG = Logger.getInstance(LocalizeFileState.class);

  private final String myId;
  private URL myFileUrl;

  private volatile Map<String, LocalizeKeyText> myTexts;

  public LocalizeFileState(String id, URL fileUrl) {
    myId = id;
    myFileUrl = fileUrl;
  }

  @Nullable
  public String getValue(LocalizeKey key) {
    Map<String, LocalizeKeyText> texts = myTexts;

    if (texts == null) {
      texts = loadTexts(myFileUrl);
      myTexts = texts;
    }

    LocalizeKeyText text = texts.get(key.getKey());
    return text == null ? null : text.getText();
  }

  @Nonnull
  private Map<String, LocalizeKeyText> loadTexts(URL fileUrl) {
    Map<String, LocalizeKeyText> map = new HashMap<>();

    long time = System.currentTimeMillis();

    Yaml yaml = new Yaml();
    try (InputStream stream = fileUrl.openStream()) {
      Map<String, Map<String, String>> o = yaml.load(stream);

      for (Map.Entry<String, Map<String, String>> entry : o.entrySet()) {
        String key = entry.getKey();
        Map<String, String> value = entry.getValue();

        LocalizeKeyText instance = new LocalizeKeyText(StringUtil.notNullize(value.get("text")));

        map.put(key, instance);
      }
    }
    catch (Exception e) {
      LOG.error(e);
    }

    LOG.info(myId + " parsed in " + (System.currentTimeMillis() - time) + " ms. Size: " + map.size());
    return map;
  }
}
