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
package consulo.ui.ex.impl.internal.popup;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.component.ComponentManager;
import consulo.dataContext.DataContext;
import consulo.ui.NotificationType;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.popup.*;
import consulo.ui.image.Image;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * The counterpart of {@code DesktopAWTPopupFactoryImpl} for the frontends which have no swing. Only the list popup
 * is answered so far - the rest of the factory is built around swing components, and each kind has to grow its own
 * unified form before it can be handed back here.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedPopupFactoryImpl extends JBPopupFactory {
    @Override
    public ListPopup createListPopup(ListPopupStep step) {
        return new UnifiedListPopupImpl(null, step);
    }

    @Override
    public ListPopup createListPopup(@Nullable ComponentManager project, ListPopupStep step) {
        return new UnifiedListPopupImpl(project, step);
    }

    @Override
    public ListPopup createListPopup(ListPopupStep step, int maxRowCount) {
        return new UnifiedListPopupImpl(null, step);
    }

    @Override
    public <T> IPopupChooserBuilder<T> createPopupChooserBuilder(List<? extends T> list) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListPopup createConfirmation(String title, Runnable onYes, int defaultOptionIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListPopup createConfirmation(String title, String yesText, String noText, Runnable onYes, int defaultOptionIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListPopup createConfirmation(
        String title,
        String yesText,
        String noText,
        Runnable onYes,
        Runnable onNo,
        int defaultOptionIndex
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PopupStep createActionsStep(
        ActionGroup actionGroup,
        DataContext dataContext,
        @Nullable String actionPlace,
        boolean showNumbers,
        boolean showDisabledActions,
        String title,
        Component component,
        boolean honorActionMnemonics,
        int defaultOptionIndex,
        boolean autoSelectionEnabled
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RelativePoint guessBestPopupLocation(JComponent component) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListPopup createActionGroupPopup(
        String title,
        ActionGroup actionGroup,
        DataContext dataContext,
        boolean showNumbers,
        boolean showDisabledActions,
        boolean honorActionMnemonics,
        @Nullable Runnable disposeCallback,
        int maxRowCount,
        @Nullable Predicate<? super AnAction> preselectActionCondition,
        boolean forceHeavyPopup
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListPopup createActionGroupPopup(
        @Nullable String title,
        ActionGroup actionGroup,
        DataContext dataContext,
        ActionSelectionAid aid,
        boolean showDisabledActions,
        @Nullable Runnable disposeCallback,
        int maxRowCount,
        @Nullable Predicate<? super AnAction> preselectActionCondition,
        @Nullable String actionPlace,
        BiPredicate<Object, Boolean> customFilter
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TreePopup createTree(JBPopup parent, TreePopupStep step, Object parentValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TreePopup createTree(TreePopupStep step) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ComponentPopupBuilder createComponentPopupBuilder(JComponent content, @Nullable JComponent preferableFocusComponent) {
        // a builder is configured far more often than a popup is created - a help tooltip does it on every
        // install - so refusal is deferred to createPopup, where the swing content actually matters
        return new UnifiedComponentPopupBuilder();
    }

    @Override
    public RelativePoint guessBestPopupLocation(DataContext dataContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Point getCenterOf(JComponent container, JComponent content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<JBPopup> getChildPopups(Component parent) {
        return List.of();
    }

    @Override
    public boolean isPopupActive() {
        return false;
    }

    @Override
    public BalloonBuilder createBalloonBuilder(JComponent content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BalloonBuilder createDialogBalloonBuilder(JComponent content, String title) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BalloonBuilder createHtmlTextBalloonBuilder(
        String htmlContent,
        @Nullable Image icon,
        Color textColor,
        Color fillColor,
        @Nullable HyperlinkListener listener
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BalloonBuilder createHtmlTextBalloonBuilder(
        String htmlContent,
        NotificationType messageType,
        @Nullable HyperlinkListener listener
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JBPopup createMessage(String text) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Balloon getParentBalloonFor(@Nullable Component c) {
        return null;
    }
}
