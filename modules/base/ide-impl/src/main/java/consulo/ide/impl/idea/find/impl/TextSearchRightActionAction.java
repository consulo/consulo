// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.ide.impl.idea.find.impl;

import consulo.find.localize.FindLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.ui.image.Image;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Toggle actions for the right side of the search input field: Case Sensitive, Whole Words, Regex.
 */
public abstract class TextSearchRightActionAction extends DumbAwareToggleAction {
    private final AtomicBoolean myState;
    private final Runnable myCallback;

    protected TextSearchRightActionAction(
        LocalizeValue message,
        Image icon,
        Image hoveredIcon,
        Image selectedIcon,
        AtomicBoolean state,
        Consumer<AnAction> registerShortcut,
        Runnable callback
    ) {
        super(message, LocalizeValue.empty(), icon);
        myState = state;
        myCallback = callback;
        getTemplatePresentation().setHoveredIcon(hoveredIcon);
        getTemplatePresentation().setSelectedIcon(selectedIcon);
        registerShortcut.accept(this);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return myState.get();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean selected) {
        myState.set(selected);
        myCallback.run();
    }

    public boolean isSelected() {
        return myState.get();
    }

    public static class CaseSensitiveAction extends TextSearchRightActionAction {
        public CaseSensitiveAction(AtomicBoolean property, Consumer<AnAction> registerShortcut, Runnable onChanged) {
            super(
                FindLocalize.findPopupCaseSensitive(),
                PlatformIconGroup.actionsMatchcase(),
                PlatformIconGroup.actionsMatchcasehovered(),
                PlatformIconGroup.actionsMatchcaseselected(),
                property, registerShortcut, onChanged
            );
        }
    }

    public static class WordAction extends TextSearchRightActionAction {
        public WordAction(AtomicBoolean property, Consumer<AnAction> registerShortcut, Runnable onChanged) {
            super(
                FindLocalize.findWholeWords(),
                PlatformIconGroup.actionsWords(),
                PlatformIconGroup.actionsWordshovered(),
                PlatformIconGroup.actionsWordsselected(),
                property, registerShortcut, onChanged
            );
        }
    }

    public static class RegexpAction extends TextSearchRightActionAction {
        public RegexpAction(AtomicBoolean property, Consumer<AnAction> registerShortcut, Runnable onChanged) {
            super(
                FindLocalize.findRegex(),
                PlatformIconGroup.actionsRegex(),
                PlatformIconGroup.actionsRegexhovered(),
                PlatformIconGroup.actionsRegexselected(),
                property, registerShortcut, onChanged
            );
        }
    }
}
