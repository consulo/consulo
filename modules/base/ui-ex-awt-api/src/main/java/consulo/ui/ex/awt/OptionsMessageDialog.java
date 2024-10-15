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
package consulo.ui.ex.awt;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.awt.internal.laf.MultiLineLabelUI;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

public abstract class OptionsMessageDialog extends OptionsDialog {
    private final LocalizeValue myMessage;
    private final Image myIcon;

    protected OptionsMessageDialog(Project project, @Nonnull LocalizeValue message, @Nonnull LocalizeValue title, final Image icon) {
        super(project);
        myMessage = message;
        myIcon = icon;
        setTitle(title);
        setButtonsAlignment(SwingUtilities.CENTER);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    protected OptionsMessageDialog(Project project, final String message, String title, final Image icon) {
        this(project, LocalizeValue.ofNullable(message), LocalizeValue.ofNullable(title), icon);
    }

    @Nonnull
    //TODO: rename to getOkActionName() after deprecation removal and make abstract
    protected LocalizeValue getOkActionValue() {
        return LocalizeValue.ofNullable(getOkActionName());
    }

    @Deprecated
    @DeprecationInfo("Use #getOkActionValue()")
    protected String getOkActionName() {
        return getOkActionValue().get();
    }

    //TODO: rename to getCancelActionName() after deprecation removal and make abstract
    protected LocalizeValue getCancelActionValue() {
        return LocalizeValue.ofNullable(getCancelActionName());
    }

    @Deprecated
    @DeprecationInfo("Use #getCancelActionValue()")
    protected String getCancelActionName() {
        return getCancelActionValue().get();
    }

    @Override
    @Nonnull
    protected LocalizeAction[] createActions() {
        LocalizeAction okAction = getOKAction();
        LocalizeAction cancelAction = getCancelAction();
        okAction.setText(getOkActionValue());
        cancelAction.setText(getCancelActionValue());
        return new LocalizeAction[]{okAction, cancelAction};
    }

    @Override
    protected JComponent createNorthPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        if (myIcon != null) {
            JLabel iconLabel = new JBLabel(myIcon);
            Container container = new Container();
            container.setLayout(new BorderLayout());
            container.add(iconLabel, BorderLayout.NORTH);
            panel.add(container, BorderLayout.WEST);
        }

        if (myMessage != null) {
            JLabel textLabel = new JLabel(myMessage.get());
            textLabel.setUI(new MultiLineLabelUI());
            panel.add(textLabel, BorderLayout.CENTER);
        }
        return panel;
    }

    @Override
    protected JComponent createCenterPanel() {
        return null;
    }
}
