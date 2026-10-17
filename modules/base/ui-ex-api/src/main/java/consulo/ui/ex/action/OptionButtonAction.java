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
package consulo.ui.ex.action;

import consulo.localize.LocalizeValue;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.OptionButton;
import consulo.ui.OptionButtonItem;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public abstract class OptionButtonAction extends DumbAwareAction implements CustomUIComponentAction {
    public static final Key<List<OptionButtonItem>> OPTIONS = Key.create("OptionButtonAction.options");

    private final boolean myPrimary;

    protected OptionButtonAction(LocalizeValue text, boolean primary) {
        super(text);
        myPrimary = primary;
    }

    protected abstract List<OptionButtonItem> getOptions();

    @RequiredUIAccess
    protected abstract void performMain();

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        performMain();
    }

    @RequiredUIAccess
    @Override
    public Component createCustomComponent(Presentation presentation, String place) {
        OptionButton button = OptionButton.create(presentation.getTextValue(), event -> performMain());
        if (myPrimary) {
            button.addStyle(ButtonStyle.PRIMARY);
        }
        List<OptionButtonItem> options = presentation.getClientProperty(OPTIONS);
        button.setOptions(options != null ? options : getOptions());
        button.setEnabled(presentation.isEnabled());
        presentation.addPropertyChangeListener(event -> {
            if (OPTIONS.toString().equals(event.getPropertyName())) {
                List<OptionButtonItem> changed = presentation.getClientProperty(OPTIONS);
                button.setOptions(changed != null ? changed : getOptions());
            }
            else if (Presentation.PROP_ENABLED.equals(event.getPropertyName())) {
                button.setEnabled(presentation.isEnabled());
            }
            else if (Presentation.PROP_TEXT.equals(event.getPropertyName())) {
                button.setText(presentation.getTextValue());
            }
        });
        return button;
    }
}
