/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.configurable;

import consulo.annotation.DeprecationInfo;
import consulo.util.lang.reflect.ReflectionUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Supplier;

@Deprecated
@DeprecationInfo("Use SimpleConfigurable")
public final class IdeaSimpleConfigurable<UI extends IdeaConfigurableUi<S>, S> extends IdeaConfigurableBase<UI, S> {
  private final Class<UI> uiClass;
  private final Supplier<S> settingsGetter;

  private IdeaSimpleConfigurable(@Nonnull String id, @Nonnull String displayName, @Nullable String helpTopic, @Nonnull Class<UI> uiClass, @Nonnull Supplier<S> settingsGetter) {
    super(id, displayName, helpTopic);

    this.uiClass = uiClass;
    this.settingsGetter = settingsGetter;
  }

  public static <UI extends IdeaConfigurableUi<S>, S> IdeaSimpleConfigurable<UI, S> create(@Nonnull String id, @Nonnull String displayName, @Nullable String helpTopic, @Nonnull Class<UI> uiClass, @Nonnull Supplier<S> settingsGetter) {
    return new IdeaSimpleConfigurable<UI, S>(id, displayName, helpTopic, uiClass, settingsGetter);
  }

  public static <UI extends IdeaConfigurableUi<S>, S> IdeaSimpleConfigurable<UI, S> create(@Nonnull String id, @Nonnull String displayName, @Nonnull Class<UI> uiClass, @Nonnull Supplier<S> settingsGetter) {
    return create(id, displayName, id, uiClass, settingsGetter);
  }

  @Nonnull
  @Override
  protected S getSettings() {
    return settingsGetter.get();
  }

  @Override
  protected UI createUi() {
    return ReflectionUtil.newInstance(uiClass);
  }
}