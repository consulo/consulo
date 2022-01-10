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
package consulo.actionSystem.impl;

import com.intellij.AbstractBundle;
import com.intellij.CommonBundle;
import consulo.annotation.DeprecationInfo;
import consulo.container.plugin.PluginDescriptor;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import java.util.ResourceBundle;

/**
 * @author VISTALL
 * @since 2020-06-01
 */
public interface LocalizeHelper {
  static LocalizeHelper build(PluginDescriptor plugin) {
    String localize = plugin.getLocalize();
    if (!StringUtil.isEmpty(localize)) {
      return new DefaultLocalizeHelper(localize);
    }

    String resourceBundleBaseName = plugin.getResourceBundleBaseName();
    if (!StringUtil.isEmpty(resourceBundleBaseName)) {
      return new ResourceBundleLocalizeHelper(resourceBundleBaseName, plugin.getPluginClassLoader());
    }

    return FallbackLocalizeHelper.INSTANCE;
  }

  public static class DefaultLocalizeHelper implements LocalizeHelper {
    private final String myLocalize;

    public DefaultLocalizeHelper(String localize) {
      myLocalize = localize;
    }

    @Nonnull
    @Override
    public LocalizeValue getValue(@Nonnull String key) {
      return LocalizeKey.of(myLocalize, key).getValue();
    }
  }

  public static class ResourceBundleLocalizeHelper implements LocalizeHelper {
    private final String myResourceBundleBaseName;
    private final ClassLoader myPluginClassLoader;

    private ResourceBundle myResourceBundle;

    public ResourceBundleLocalizeHelper(String resourceBundleBaseName, ClassLoader pluginClassLoader) {
      myResourceBundleBaseName = resourceBundleBaseName;
      myPluginClassLoader = pluginClassLoader;
    }

    @Nonnull
    @Override
    public String getText(String key) {
      if (myResourceBundle == null) {
        myResourceBundle = AbstractBundle.getResourceBundle(myResourceBundleBaseName, myPluginClassLoader);
      }

      return CommonBundle.message(myResourceBundle, key);
    }

    @Nonnull
    @Override
    public LocalizeValue getValue(@Nonnull String key) {
      return LocalizeValue.of(getText(key));
    }
  }

  public static class FallbackLocalizeHelper extends DefaultLocalizeHelper {
    private static final FallbackLocalizeHelper INSTANCE = new FallbackLocalizeHelper();

    public FallbackLocalizeHelper() {
      super("consulo.platform.base.ActionLocalize");
    }
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use #getValue()")
  default String getText(String key) {
    return getValue(key).getValue();
  }

  @Nonnull
  LocalizeValue getValue(@Nonnull String key);
}
