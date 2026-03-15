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
package consulo.ui.ex.awt.popup;

import consulo.annotation.DeprecationInfo;
import consulo.project.Project;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.ListPopupStep;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;

/**
 * Inteface for JBPopupFactory impl
 *
 * @author VISTALL
 * @since 21-Jul-22
 */
@Deprecated
@DeprecationInfo("Do not depend to Swing classes")
public interface AWTPopupFactory {
    @Deprecated
    <T> AWTPopupChooserBuilder<T> createListPopupBuilder(JList<T> list);

    AWTListPopup createListPopup(Project project,
                                 ListPopupStep step,
                                 Function<AWTListPopup, ListCellRenderer> rendererFactory);

    AWTListPopup createListPopup(Project project,
                                 ListPopupStep step,
                                 @Nullable AWTListPopup parentPopup,
                                 Function<AWTListPopup, ListCellRenderer> rendererFactory,
                                 AWTPopupSubFactory factory);

    int getPointerLength(Balloon.Position position, boolean dialogMode);

    <T> AWTPopupChooserBuilder<T> createPopupBuilder(JTree tree);

    <T> AWTPopupChooserBuilder<T> createPopupBuilder(JTable table);
}
