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

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import consulo.annotation.DeprecationInfo;
import consulo.logging.Logger;
import consulo.module.extension.impl.ModuleExtensionProviders;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.roots.ModuleRootLayer;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageKey;
import consulo.ui.style.StandardColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author VISTALL
 * @since 11:45/19.05.13
 */
public class ModuleExtensionProviderEP extends AbstractExtensionPointBean {
  public static final Logger LOGGER = Logger.getInstance(ModuleExtensionProviderEP.class);

  private static AtomicInteger ourCounter = new AtomicInteger();

  private final int myInternalIndex = ourCounter.getAndIncrement();

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
  public String icon;

  public int getInternalIndex() {
    return myInternalIndex;
  }

  private NotNullLazyValue<Image> myIconValue = new NotNullLazyValue<Image>() {
    @Nonnull
    @Override
    protected Image compute() {
      if (StringUtil.isEmpty(icon)) {
        return PlatformIconGroup.actionsHelp();
      }

      ImageKey imageKey = ImageKey.fromString(icon, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
      if (imageKey != null) {
        return imageKey;
      }

      return ImageEffects.colorFilled(Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE, StandardColors.YELLOW);
    }
  };

  private NullableLazyValue<Pair<Class<ModuleExtension>, Constructor<ModuleExtension>>> myImmutableValue = NullableLazyValue.of(() -> this.<ModuleExtension>resolveFor(immutableClass));

  private NullableLazyValue<Pair<Class<MutableModuleExtension>, Constructor<MutableModuleExtension>>> myMutableValue =
          NullableLazyValue.of(() -> this.<MutableModuleExtension>resolveFor(mutableClass));

  @Nullable
  private <E> Pair<Class<E>, Constructor<E>> resolveFor(String className) {
    Class<E> value = findClassNoExceptions(className);
    if (value == null) {
      return null;
    }
    try {
      return Pair.create(value, value.getConstructor(String.class, ModuleRootLayer.class));
    }
    catch (NoSuchMethodException e) {
      ModuleExtensionProviderEP.LOGGER.error(e);
    }
    return null;
  }

  @Nullable
  public ModuleExtension<?> createImmutable(@Nonnull ModuleRootLayer modifiableRootModel) {
    try {
      Pair<Class<ModuleExtension>, Constructor<ModuleExtension>> value = myImmutableValue.getValue();
      if (value != null) {
        return ReflectionUtil.createInstance(value.getSecond(), key, modifiableRootModel);
      }
    }
    catch (Error e) {
      ModuleExtensionProviderEP.LOGGER.error("Problem with module extension: " + key, e);
    }
    return null;
  }

  @Nullable
  public Class<ModuleExtension> getImmutableClass() {
    Pair<Class<ModuleExtension>, Constructor<ModuleExtension>> value = myImmutableValue.getValue();
    return value == null ? null : value.getFirst();
  }

  @Nullable
  public MutableModuleExtension<?> createMutable(@Nonnull ModuleRootLayer modifiableRootModel) {
    try {
      Pair<Class<MutableModuleExtension>, Constructor<MutableModuleExtension>> value = myMutableValue.getValue();
      if (value != null) {
        return ReflectionUtil.createInstance(value.getSecond(), key, modifiableRootModel);
      }
    }
    catch (Error e) {
      ModuleExtensionProviderEP.LOGGER.error("Problem with module extension: " + key, e);
    }
    return null;
  }

  @Nullable
  public Class<MutableModuleExtension> getMutableClass() {
    Pair<Class<MutableModuleExtension>, Constructor<MutableModuleExtension>> value = myMutableValue.getValue();
    return value == null ? null : value.getFirst();
  }

  @Nonnull
  public Image getIcon() {
    return myIconValue.getValue();
  }

  @Nonnull
  public String getName() {
    if (StringUtil.isEmpty(name)) {
      ModuleExtensionProviderEP.LOGGER.error("Name is empty for extension '" + key + "'. Capitalized 'key' used as name. Please define 'name' attribute for ep");
      name = StringUtil.capitalize(key);
    }
    return name;
  }

  @Nonnull
  public String getKey() {
    return key;
  }

  @Nullable
  @Deprecated
  @DeprecationInfo(value = "Use ModuleExtensionProviders#findProvider(String)", until = "2.0")
  public static ModuleExtensionProviderEP findProviderEP(@Nonnull String id) {
    return ModuleExtensionProviders.findProvider(id);
  }
}
