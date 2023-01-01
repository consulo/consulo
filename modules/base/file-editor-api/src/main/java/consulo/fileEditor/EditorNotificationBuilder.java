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
package consulo.fileEditor;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.UIEvent;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 18-Jul-22
 */
public interface EditorNotificationBuilder {
  @Nonnull
  EditorNotificationBuilder withText(@Nonnull LocalizeValue text);

  @Nonnull
  EditorNotificationBuilder withIcon(@Nonnull Image image);

  @Nonnull
  EditorNotificationBuilder withBackgroundColor(@Nonnull ColorValue color);

  @Nonnull
  default EditorNotificationBuilder withAction(@Nonnull LocalizeValue actionText, @Nonnull @RequiredUIAccess Consumer<UIEvent<Component>> actionHandler) {
    return withAction(actionText, LocalizeValue.of(), actionHandler);
  }

  @Nonnull
  EditorNotificationBuilder withAction(@Nonnull LocalizeValue actionText, @Nonnull LocalizeValue actionTooltipText, @Nonnull @RequiredUIAccess Consumer<UIEvent<Component>> actionHandler);

  @Nonnull
  EditorNotificationBuilder withAction(@Nonnull LocalizeValue actionText, @Nonnull String actionRefId);

  @Nonnull
  default EditorNotificationBuilder withGearAction(@Nonnull @RequiredUIAccess Consumer<UIEvent<Component>> action) {
    return withGearAction(LocalizeValue.of(), PlatformIconGroup.generalGearplain(), action);
  }

  @Nonnull
  default EditorNotificationBuilder withGearAction(@Nonnull LocalizeValue tooltipText, @Nonnull @RequiredUIAccess Consumer<UIEvent<Component>> action) {
    return withGearAction(tooltipText, PlatformIconGroup.generalGearplain(), action);
  }

  @Nonnull
  EditorNotificationBuilder withGearAction(@Nonnull LocalizeValue tooltipText, @Nonnull Image image, @Nonnull @RequiredUIAccess Consumer<UIEvent<Component>> action);
}
