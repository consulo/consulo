/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.keymap.impl;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMUtil;
import consulo.logging.Logger;
import consulo.platform.Platform;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Belyaev
 */
@Singleton
public class DefaultKeymap {
  private static final Logger LOG = Logger.getInstance(DefaultKeymap.class);

  private static final String KEY_MAP = "keymap";
  private static final String NAME_ATTRIBUTE = "name";

  private final List<Keymap> myKeymaps = new ArrayList<>();

  public static DefaultKeymap getInstance() {
    return ServiceManager.getService(DefaultKeymap.class);
  }

  public DefaultKeymap() {
    for (BundledKeymapEP bundledKeymapEP : BundledKeymapEP.EP_NAME.getExtensionList()) {
      try {
        String file = bundledKeymapEP.file;
        if (!file.endsWith(".xml")) {
          file += ".xml";
        }

        InputStream inputStream = bundledKeymapEP.getLoaderForClass().getResourceAsStream(file);
        if (inputStream == null) {
          LOG.warn("Keymap: " + file + " not found in " + bundledKeymapEP.getPluginDescriptor().getPluginId().getIdString());
          continue;
        }

        loadKeymapsFromElement(JDOMUtil.load(inputStream));
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }
  }


  private void loadKeymapsFromElement(final Element element) throws InvalidDataException {
    if(!element.getName().equals(KEY_MAP)) {
      throw new IllegalArgumentException("Expecting tag: " + KEY_MAP);
    }

    String keymapName = element.getAttributeValue(NAME_ATTRIBUTE);
    DefaultKeymapImpl keymap = keymapName.startsWith(KeymapManager.MAC_OS_X_KEYMAP) ? new MacOSDefaultKeymap() : new DefaultKeymapImpl();
    keymap.readExternal(element, myKeymaps.toArray(new Keymap[myKeymaps.size()]));
    keymap.setName(keymapName);
    myKeymaps.add(keymap);
  }

  public List<Keymap> getKeymaps() {
    return myKeymaps;
  }

  @Nonnull
  public String getDefaultKeymapName() {
    Platform.OperatingSystem os = Platform.current().os();

    if (os.isMac()) {
      return KeymapManager.MAC_OS_X_10_5_PLUS_KEYMAP;
    }
    else if (os.isGNOME()) {
      return KeymapManager.GNOME_KEYMAP;
    }
    else if (os.isKDE()) {
      return KeymapManager.KDE_KEYMAP;
    }
    else if (os.isXWindow()) {
      return KeymapManager.X_WINDOW_KEYMAP;
    }
    else {
      return KeymapManager.DEFAULT_IDEA_KEYMAP;
    }
  }

  public String getKeymapPresentableName(KeymapImpl keymap) {
    String name = keymap.getName();
    return KeymapManager.DEFAULT_IDEA_KEYMAP.equals(name) ? "Default" : name;
  }
}
