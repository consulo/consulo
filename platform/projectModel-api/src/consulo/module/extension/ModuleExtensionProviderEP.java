/*
 * Copyright 2013-2016 consulo.io
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
package consulo.module.extension;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import consulo.roots.ModuleRootLayer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.HashMap;
import com.intellij.util.xmlb.annotations.Attribute;
import consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11:45/19.05.13
 */
@Logger
public class ModuleExtensionProviderEP extends AbstractExtensionPointBean {
  public static final ExtensionPointName<ModuleExtensionProviderEP> EP_NAME = ExtensionPointName.create("com.intellij.moduleExtensionProvider");

  @Attribute("key")
  public String key;

  @Attribute("parentKey")
  public String parentKey;

  @Attribute("allowMixin")
  public boolean allowMixin;

  @Attribute("systemOnly")
  public boolean systemOnly;

  @Attribute("name")
  public String name;

  @Attribute("mutableClass")
  public String mutableClass;

  @Attribute("immutableClass")
  public String immutableClass;

  @Attribute("icon")
  @NotNull
  public String icon;

  private static NotNullLazyValue<Map<String, ModuleExtensionProviderEP>> ourAllValue = new NotNullLazyValue<Map<String, ModuleExtensionProviderEP>>() {
    @NotNull
    @Override
    protected Map<String, ModuleExtensionProviderEP> compute() {
      Map<String, ModuleExtensionProviderEP> map = new HashMap<String, ModuleExtensionProviderEP>();
      for (ModuleExtensionProviderEP ep : EP_NAME.getExtensions()) {
        map.put(ep.key, ep);
      }
      return map;
    }
  };

  private NotNullLazyValue<Icon> myIconValue = new NotNullLazyValue<Icon>() {
    @NotNull
    @Override
    protected Icon compute() {
      if (StringUtil.isEmpty(icon)) {
        return AllIcons.Toolbar.Unknown;
      }
      Icon icon1 = IconLoader.findIcon(icon, getLoaderForClass());
      return icon1 == null ? AllIcons.Toolbar.Unknown : icon1;
    }
  };

  private NullableLazyValue<Constructor<ModuleExtension>> myImmutableConstructorValue = new NullableLazyValue<Constructor<ModuleExtension>>() {
    @Override
    protected Constructor<ModuleExtension> compute() {
      Class<ModuleExtension> value = findClassNoExceptions(immutableClass);
      if(value == null) {
        return null;
      }
      try {
        return value.getConstructor(String.class, ModuleRootLayer.class);
      }
      catch (NoSuchMethodException e) {
         ModuleExtensionProviderEP.LOGGER.error(e);
      }
      return null;
    }
  };

  private NullableLazyValue<Constructor<MutableModuleExtension>> myMutableConstructorValue = new NullableLazyValue<Constructor<MutableModuleExtension>>() {
    @Override
    protected Constructor<MutableModuleExtension> compute() {
      Class<MutableModuleExtension> value = findClassNoExceptions(mutableClass);
      if(value == null) {
        return null;
      }
      try {
        return value.getConstructor(String.class, ModuleRootLayer.class);
      }
      catch (NoSuchMethodException e) {
        ModuleExtensionProviderEP.LOGGER.error(e);
      }
      return null;
    }
  };

  @Nullable
  public ModuleExtension<?> createImmutable(@NotNull ModuleRootLayer modifiableRootModel) {
    try {
      Constructor<ModuleExtension> value = myImmutableConstructorValue.getValue();
      if(value != null) {
        return ReflectionUtil.createInstance(value, key, modifiableRootModel);
      }
    }
    catch (Error e) {
      ModuleExtensionProviderEP.LOGGER.error("Problem with module extension: " + key, e);
    }
    return null;
  }

  @Nullable
  public MutableModuleExtension<?> createMutable(@NotNull ModuleRootLayer modifiableRootModel) {
    try {
      Constructor<MutableModuleExtension> value = myMutableConstructorValue.getValue();
      if(value != null) {
        return ReflectionUtil.createInstance(value, key, modifiableRootModel);
      }
    }
    catch (Error e) {
      ModuleExtensionProviderEP.LOGGER.error("Problem with module extension: " + key, e);
    }
    return null;
  }

  @NotNull
  public Icon getIcon() {
    return myIconValue.getValue();
  }

  @NotNull
  public String getName() {
    if(StringUtil.isEmpty(name)) {
      ModuleExtensionProviderEP.LOGGER.error("Name is empty for extension '" + key + "'. Capitalized 'key' used as name. Please define 'name' attribute for ep");
      name = StringUtil.capitalize(key);
    }
    return name;
  }

  @NotNull
  public String getKey() {
    return key;
  }

  @Nullable
  public static ModuleExtensionProviderEP findProviderEP(@NotNull String id) {
    return ourAllValue.getValue().get(id);
  }
}
