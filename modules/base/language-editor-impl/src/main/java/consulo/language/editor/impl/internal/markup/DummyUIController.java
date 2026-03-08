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
package consulo.language.editor.impl.internal.markup;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.GridBag;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-03-08
 */
public class DummyUIController implements UIController {
    public static final DummyUIController INSTANCE = new DummyUIController();

    @Override
    public boolean enableToolbar() {
        return false;
    }

    @Nonnull
    @Override
    public List<AnAction> getActions() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<InspectionsLevel> getAvailableLevels() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<LanguageHighlightLevel> getHighlightLevels() {
        return Collections.emptyList();
    }

    @Override
    public void setHighLightLevel(@Nonnull LanguageHighlightLevel newLevels) {

    }

    @Override
    public void fillHectorPanels(@Nonnull Container container, @Nonnull GridBag bag) {

    }

    @Override
    public boolean canClosePopup() {
        return false;
    }

    @Override
    public void onClosePopup() {

    }

    @Override
    public void openProblemsView() {

    }
}
