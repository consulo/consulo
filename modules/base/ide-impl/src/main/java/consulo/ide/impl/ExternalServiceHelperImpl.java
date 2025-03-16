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
package consulo.ide.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.internal.ExternalServiceHelper;
import consulo.ide.impl.idea.ide.ui.search.SearchUtil;
import consulo.ide.impl.idea.ide.util.TipUIUtil;
import consulo.localize.LocalizeValue;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2025-02-01
 */
@Singleton
@ServiceImpl
public class ExternalServiceHelperImpl implements ExternalServiceHelper {
    @Override
    public void openTipInBrowser(@Nonnull Pair<LocalizeValue, PluginDescriptor> tipInfo, Object browser) {
        TipUIUtil.openTipInBrowserByLocalize(tipInfo, (JEditorPane) browser);
    }

    @Override
    public String markup(@Nonnull String textToMarkup, @Nullable String filter) {
        return SearchUtil.markup(textToMarkup, filter);
    }
}
