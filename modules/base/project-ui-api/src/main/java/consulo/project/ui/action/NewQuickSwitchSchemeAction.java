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
package consulo.project.ui.action;

import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.SimpleListPopupStepBuilder;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2025-08-13
 */
public abstract class NewQuickSwitchSchemeAction<T> extends AnAction implements DumbAware {
    private record Item<K>(LocalizeValue actionText, K value) {
    }

    public NewQuickSwitchSchemeAction(@Nonnull LocalizeValue actionText) {
        super(actionText, actionText);
    }

    public abstract void fill(@Nonnull BiConsumer<LocalizeValue, T> itemsAcceptor);

    @Nonnull
    public abstract T getCurrentValue();

    public abstract void changeSchemeTo(@Nonnull T value);

    @Override
    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public final void actionPerformed(@Nonnull AnActionEvent e) {
        T oldValue = getCurrentValue();

        Project project = e.getRequiredData(Project.KEY);

        SimpleReference<Item<T>> defaultValue = new SimpleReference<>();

        List<Item<T>> items = new ArrayList<>();
        fill((s, t) -> {
            Item<T> item = new Item<>(s, t);

            items.add(item);

            if (Objects.equals(oldValue, t)) {
                defaultValue.set(item);
            }
        });

        SimpleListPopupStepBuilder<Item<T>> builder = SimpleListPopupStepBuilder.newBuilder(items);
        builder.withTextBuilder(tItem -> tItem.actionText().get());
        builder.withTitle(getPopupTitle(e));
        builder.withFinishAction(tItem -> changeSchemeTo(tItem.value()));

        if (!defaultValue.isNull()) {
            builder.withDefaultValue(defaultValue.get());
        }

        ListPopup listPopup = JBPopupFactory.getInstance().createListPopup(project, builder.build());
        listPopup.setHandleAutoSelectionBeforeShow(true);
        listPopup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@Nonnull LightweightWindowEvent event) {
                if (!event.isOk()) {
                    changeSchemeTo(oldValue);
                }
            }
        });

        showPopup(e, listPopup);

        // add after showing - handle preselect
        listPopup.addSelectionListener(o -> changeSchemeTo(((Item<T>) o).value()));
    }

    protected LocalizeValue getPopupTitle(AnActionEvent e) {
        return e.getPresentation().getTextValue();
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(e.hasData(Project.KEY) && isEnabled());
    }

    protected boolean isEnabled() {
        return true;
    }

    protected void showPopup(AnActionEvent e, ListPopup popup) {
        popup.showCenteredInCurrentWindow(e.getRequiredData(Project.KEY));
    }
}
