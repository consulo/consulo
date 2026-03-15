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
package consulo.desktop.awt.ui.popup;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ui.popup.PopupFactoryImpl;
import consulo.ide.impl.idea.ui.popup.WizardPopup;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.project.Project;
import consulo.ui.ex.awt.CollectionListModel;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.popup.AWTListPopup;
import consulo.ui.ex.awt.popup.AWTPopupChooserBuilder;
import consulo.ui.ex.awt.popup.AWTPopupFactory;
import consulo.ui.ex.awt.popup.AWTPopupSubFactory;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.popup.*;
import consulo.util.collection.Maps;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2025-05-03
 */
@Singleton
@ServiceImpl
public class DesktopAWTPopupFactoryImpl extends PopupFactoryImpl implements AWTPopupFactory {
    private final Map<Disposable, List<Balloon>> myStorage = Maps.newWeakHashMap();

    @Override
    public int getPointerLength(Balloon.Position position, boolean dialogMode) {
        return BalloonImpl.getPointerLength(position, dialogMode);
    }

    @Override
    public <T> AWTPopupChooserBuilder<T> createPopupBuilder(JTree tree) {
        return new PopupChooserBuilder<T>(tree);
    }

    @Override
    public <T> AWTPopupChooserBuilder<T> createPopupBuilder(JTable table) {
        return new PopupChooserBuilder<T>(table);
    }

    
    @Override
    public BalloonBuilder createBalloonBuilder(JComponent content) {
        return new BalloonPopupBuilderImpl(myStorage, content);
    }

    
    @Override
    public BalloonBuilder createDialogBalloonBuilder(JComponent content, String title) {
        BalloonPopupBuilderImpl builder = new BalloonPopupBuilderImpl(myStorage, content);
        Color bg = UIManager.getColor("Panel.background");
        Color borderOriginal = Color.darkGray;
        Color border = ColorUtil.toAlpha(borderOriginal, 75);
        builder.setDialogMode(true)
            .setTitle(title)
            .setAnimationCycle(200)
            .setFillColor(bg)
            .setHideOnClickOutside(false)
            .setHideOnKeyOutside(false)
            .setHideOnAction(false)
            .setCloseButtonEnabled(true)
            .setShadow(true);

        return builder;
    }

    @Override
    public AWTListPopup createListPopup(
        Project project,
        ListPopupStep step,
        Function<AWTListPopup, ListCellRenderer> rendererFactory
    ) {
        return new ListPopupImpl(step) {
            @Override
            protected ListCellRenderer getListElementRenderer() {
                return rendererFactory.apply(this);
            }
        };
    }

    @Override
    public AWTListPopup createListPopup(
        Project project,
        ListPopupStep step,
        @Nullable AWTListPopup parentPopup,
        Function<AWTListPopup, ListCellRenderer> rendererFactory,
        AWTPopupSubFactory factory
    ) {
        return new ListPopupImpl(project, (WizardPopup) parentPopup, step, null) {
            @Override
            protected ListCellRenderer getListElementRenderer() {
                return rendererFactory.apply(this);
            }

            @Override
            protected WizardPopup createPopup(WizardPopup parent, PopupStep step, Object parentValue) {
                ListPopupImpl popup = (ListPopupImpl) factory.create((AWTListPopup) parent, step);
                popup.setParentValue(parentValue);
                return popup;
            }
        };
    }

    @Override
    public <T> AWTPopupChooserBuilder<T> createListPopupBuilder(JList<T> list) {
        return new PopupChooserBuilder<>(list);
    }

    
    @Override
    public <T> IPopupChooserBuilder<T> createPopupChooserBuilder(List<? extends T> list) {
        return new PopupChooserBuilder<>(new JBList<>(new CollectionListModel<>(list)));
    }
}
