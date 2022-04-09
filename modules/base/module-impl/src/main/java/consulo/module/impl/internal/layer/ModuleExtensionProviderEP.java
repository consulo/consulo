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
package consulo.module.impl.internal.layer;

import consulo.component.extension.AbstractExtensionPointBean;
import consulo.logging.Logger;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageKey;
import consulo.ui.style.StandardColors;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.lazy.LazyValue;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.util.xml.serializer.annotation.Attribute;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 11:45/19.05.13
 */
public class ModuleExtensionProviderEP extends AbstractExtensionPointBean {
  private static final Logger LOG = Logger.getInstance(ModuleExtensionProviderEP.class);

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

  private Supplier<Image> myIconValue = LazyValue.notNull(() -> {
    if (StringUtil.isEmpty(icon)) {
      return PlatformIconGroup.actionsHelp();
    }

    ImageKey imageKey = ImageKey.fromString(icon, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
    if (imageKey != null) {
      return imageKey;
    }

    return ImageEffects.colorFilled(Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE, StandardColors.YELLOW);
  });

  private Supplier<Pair<Class<ModuleExtension>, Constructor<ModuleExtension>>> myImmutableValue = LazyValue.nullable(() -> this.<ModuleExtension>resolveFor(immutableClass));

  private Supplier<Pair<Class<MutableModuleExtension>, Constructor<MutableModuleExtension>>> myMutableValue = LazyValue.nullable(() -> this.<MutableModuleExtension>resolveFor(mutableClass));

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
      LOG.error(e);
    }
    return null;
  }

  @Nullable
  public ModuleExtension<?> createImmutable(@Nonnull ModuleRootLayer modifiableRootModel) {
    try {
      Pair<Class<ModuleExtension>, Constructor<ModuleExtension>> value = myImmutableValue.get();
      if (value != null) {
        return ReflectionUtil.createInstance(value.getSecond(), key, modifiableRootModel);
      }
    }
    catch (Error e) {
      LOG.error("Problem with module extension: " + key, e);
    }
    return null;
  }

  @Nullable
  public Class<ModuleExtension> getImmutableClass() {
    Pair<Class<ModuleExtension>, Constructor<ModuleExtension>> value = myImmutableValue.get();
    return value == null ? null : value.getFirst();
  }

  @Nullable
  public MutableModuleExtension<?> createMutable(@Nonnull ModuleRootLayer modifiableRootModel) {
    try {
      Pair<Class<MutableModuleExtension>, Constructor<MutableModuleExtension>> value = myMutableValue.get();
      if (value != null) {
        return ReflectionUtil.createInstance(value.getSecond(), key, modifiableRootModel);
      }
    }
    catch (Error e) {
      LOG.error("Problem with module extension: " + key, e);
    }
    return null;
  }

  @Nullable
  public Class<MutableModuleExtension> getMutableClass() {
    Pair<Class<MutableModuleExtension>, Constructor<MutableModuleExtension>> value = myMutableValue.get();
    return value == null ? null : value.getFirst();
  }

  @Nonnull
  public Image getIcon() {
    return myIconValue.get();
  }

  @Nonnull
  public String getName() {
    if (StringUtil.isEmpty(name)) {
      LOG.error("Name is empty for extension '" + key + "'. Capitalized 'key' used as name. Please define 'name' attribute for ep");
      name = StringUtil.capitalize(key);
    }
    return name;
  }

  @Nonnull
  public String getKey() {
    return key;
  }
}
