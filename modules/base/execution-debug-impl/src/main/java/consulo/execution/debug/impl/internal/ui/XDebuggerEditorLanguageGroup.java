/*
 * Copyright 2013-2025 consulo.io
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
package consulo.execution.debug.impl.internal.ui;

import consulo.application.ReadAction;
import consulo.application.dumb.DumbAware;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2025-01-04
 */
public class XDebuggerEditorLanguageGroup extends ActionGroup implements DumbAware {
    private final Supplier<Language> myLanguageGetter;
    private final Supplier<Collection<Language>> myLanguagesGetter;
    private final Consumer<Language> myLanguageConsumer;

    public XDebuggerEditorLanguageGroup(Supplier<Language> languageGetter,
                                        Supplier<Collection<Language>> languagesGetter,
                                        Consumer<Language> languageConsumer) {
        myLanguageGetter = languageGetter;
        myLanguagesGetter = languagesGetter;
        myLanguageConsumer = languageConsumer;
    }

    @Override
    public boolean isPopup() {
        return true;
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        Collection<Language> languages = ReadAction.compute(myLanguagesGetter::get);
        List<AnAction> result = new ArrayList<>(languages.size());
        for (Language language : languages) {
            Image icon = language.getAssociatedFileType() == null ? null : language.getAssociatedFileType().getIcon();
            if (icon == null) {
                icon = PlatformIconGroup.filetypesText();
            }

            result.add(new DumbAwareAction(language.getDisplayName(), null, icon) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    myLanguageConsumer.accept(language);
                }
            });
        }

        return result.toArray(EMPTY_ARRAY);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();

        presentation.setTextValue(XDebuggerLocalize.xdebuggerEvaluateLanguageHint());

        Language language = myLanguageGetter.get();
        LanguageFileType associatedFileType = language == null ? null : language.getAssociatedFileType();
        if (associatedFileType == null) {
            presentation.setIcon(PlatformIconGroup.filetypesText());
        } else {
            presentation.setIcon(associatedFileType.getIcon());
        }

        Collection<Language> languages = ReadAction.compute(myLanguagesGetter::get);

        presentation.setVisible(languages.size() > 1);
    }

    @Override
    public boolean showBelowArrow() {
        return false;
    }
}
