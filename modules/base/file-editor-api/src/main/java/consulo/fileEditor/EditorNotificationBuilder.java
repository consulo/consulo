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
import consulo.ui.NotificationType;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.image.Image;

/**
 * @author VISTALL
 * @since 18-Jul-22
 */
public interface EditorNotificationBuilder {
    
    EditorNotificationBuilder withText(LocalizeValue text);

    
    EditorNotificationBuilder withIcon(Image image);

    
    EditorNotificationBuilder withType(NotificationType notificationType);

    
    default EditorNotificationBuilder withAction(LocalizeValue actionText,
                                                 ComponentEventListener<Component, ComponentEvent<Component>> action) {
        return withAction(actionText, LocalizeValue.empty(), action);
    }

    
    EditorNotificationBuilder withAction(LocalizeValue actionText,
                                         LocalizeValue actionTooltipText,
                                         ComponentEventListener<Component, ComponentEvent<Component>> action);

    
    EditorNotificationBuilder withAction(LocalizeValue actionText, String actionRefId);

    
    default EditorNotificationBuilder withGearAction(ComponentEventListener<Component, ComponentEvent<Component>> action) {
        return withGearAction(LocalizeValue.empty(), PlatformIconGroup.generalGearplain(), action);
    }

    
    default EditorNotificationBuilder withGearAction(LocalizeValue tooltipText,
                                                     ComponentEventListener<Component, ComponentEvent<Component>> action) {
        return withGearAction(tooltipText, PlatformIconGroup.generalGearplain(), action);
    }

    
    EditorNotificationBuilder withGearAction(LocalizeValue tooltipText,
                                             Image image,
                                             ComponentEventListener<Component, ComponentEvent<Component>> action);
}
