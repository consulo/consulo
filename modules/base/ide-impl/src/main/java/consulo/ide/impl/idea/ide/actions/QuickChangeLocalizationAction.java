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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.ui.action.NewQuickSwitchSchemeAction;
import jakarta.annotation.Nonnull;

import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2025-08-13
 */
@ActionImpl(id = "ChangeLocalization")
public class QuickChangeLocalizationAction extends NewQuickSwitchSchemeAction<Locale> {
    public QuickChangeLocalizationAction() {
        super(ActionLocalize.actionChangelocalizationText());
    }

    @Override
    public void fill(@Nonnull BiConsumer<LocalizeValue, Locale> itemsAcceptor) {
        itemsAcceptor.accept(LocalizeValue.localizeTODO("<default>"), Locale.ROOT);
        LocalizeManager localizeManager = LocalizeManager.get();

        for (Locale locale : localizeManager.getAvailableLocales()) {
            itemsAcceptor.accept(LocalizeValue.ofNullable(locale.getDisplayName()), locale);
        }
    }

    @Nonnull
    @Override
    public Locale getCurrentValue() {
        LocalizeManager localizeManager = LocalizeManager.get();
        return localizeManager.isDefaultLocale() ? Locale.ROOT : localizeManager.getLocale();
    }

    @Override
    public void changeSchemeTo(@Nonnull Locale value) {
        LocalizeManager.get().setLocale(value == Locale.ROOT ? null : value);
    }
}
