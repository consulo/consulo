/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.action;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.CheckBoxStyle;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public abstract class CheckboxAction extends ToggleAction implements CustomUIComponentAction {
    protected CheckboxAction() {
    }

    protected CheckboxAction(@Nonnull LocalizeValue text) {
        super(text);
    }

    protected CheckboxAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    protected CheckboxAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public CheckBox createCustomComponent(Presentation presentation, @Nonnull String place) {
        LocalizeValue textValue = presentation.getTextValue();

        CheckBox checkBox = CheckBox.create(textValue);
        checkBox.addStyle(CheckBoxStyle.TRANSPARENT_BACKGROUND);
        checkBox.setToolTipText(presentation.getDescriptionValue());

        checkBox.addClickListener(e -> {
            Component component = e.getComponent();

            DataContext context = DataManager.getInstance().getDataContext(component);

            ActionToolbar toolbar = context.getData(ActionToolbar.KEY);

            DataContext dataContext = toolbar != null ? toolbar.getToolbarDataContext() : context;

            CheckboxAction.this.actionPerformed(new AnActionEvent(
                null,
                dataContext,
                ActionPlaces.UNKNOWN,
                CheckboxAction.this.getTemplatePresentation(),
                ActionManager.getInstance(),
                0,
                false,
                toolbar != null,
                e.getInputDetails()
            ));
        });

        return checkBox;
    }

    @Nonnull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        Component property = e.getPresentation().getClientProperty(COMPONENT_KEY);
        if (property instanceof CheckBox checkBox) {
            checkBox.setValue(Boolean.TRUE.equals(e.getPresentation().getClientProperty(SELECTED_PROPERTY)));
            checkBox.setEnabled(e.getPresentation().isEnabled());
            checkBox.setVisible(e.getPresentation().isVisible());
        }
    }
}
