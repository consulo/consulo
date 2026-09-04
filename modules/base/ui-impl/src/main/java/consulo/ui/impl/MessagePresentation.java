/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ui.impl;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.MessageButtonRole;
import consulo.ui.MessageSeverity;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;

/**
 * Severity to image and role to label. Both live here because {@code consulo.ui.api} sits below the
 * icon and localize libraries in the module graph.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public final class MessagePresentation {
    public static @Nullable Image icon(MessageSeverity severity) {
        return switch (severity) {
            case NONE -> null;
            case INFO -> PlatformIconGroup.generalInformationdialog();
            case WARNING -> PlatformIconGroup.generalWarningdialog();
            case ERROR -> PlatformIconGroup.generalErrordialog();
            case QUESTION -> PlatformIconGroup.generalQuestiondialog();
        };
    }

    /**
     * The title a box carries, or the application's own name when it did not set one. A box shown
     * before there is an application - an early startup failure is exactly such a box - keeps an
     * empty title rather than failing to open.
     */
    public static LocalizeValue title(LocalizeValue title) {
        if (title.isNotEmpty()) {
            return title;
        }

        Application application = ApplicationManager.getApplication();
        return application != null ? application.getName() : LocalizeValue.empty();
    }

    public static LocalizeValue label(MessageButtonRole role) {
        return switch (role) {
            case OK -> CommonLocalize.buttonOk();
            case YES -> CommonLocalize.buttonYes();
            case NO -> CommonLocalize.buttonNo();
            case CANCEL -> CommonLocalize.buttonCancel();
            case CLOSE -> CommonLocalize.buttonClose();
            case RETRY -> CommonLocalize.buttonRetry();
            case YES_TO_ALL -> CommonLocalize.buttonYesForAll();
            case NO_TO_ALL -> CommonLocalize.buttonNoForAll();
            case HELP -> CommonLocalize.buttonHelp();
        };
    }

    /**
     * Whether pressing this role counts as accepting the box.
     */
    public static boolean isAccept(MessageButtonRole role) {
        return role == MessageButtonRole.OK || role == MessageButtonRole.YES || role == MessageButtonRole.YES_TO_ALL;
    }

    private MessagePresentation() {
    }
}
