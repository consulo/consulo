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
package consulo.desktop.awt.uiOld;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.component.ComponentManager;
import consulo.ide.impl.idea.openapi.ui.MessageType;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.*;
import consulo.ui.Button;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2025-05-31
 */
@ActionImpl(id = "ShowBallonTestAction", parents = @ActionParentRef(@ActionRef(id = "Internal.UI.Desktop")))
public class ShowBallonTestAction extends DumbAwareAction {
    private static final String BORDER_RADIUS_VAR = "TextComponent.arc";

    private static class BalloonTestDialog extends DialogWrapper {
        private IntBox myBorderRadius;

        private BalloonTestDialog(@Nullable ComponentManager project) {
            super(project);
            setTitle(LocalizeValue.of("Balloon Test"));
            init();
        }

        @Nullable
        @Override
        @RequiredUIAccess
        protected JComponent createCenterPanel() {
            VerticalLayout layout = VerticalLayout.create();
            layout.addBorders(BorderStyle.EMPTY, null, 150);

            ComboBox<MessageType> typeComboBox = ComboBox.<MessageType>builder()
                .add(MessageType.INFO, LocalizeValue.of("INFO"))
                .add(MessageType.WARNING, LocalizeValue.of("WARNING"))
                .add(MessageType.ERROR, LocalizeValue.of("ERROR"))
                .build();
            typeComboBox.setValue(MessageType.INFO);

            CheckBox shiftPointer = CheckBox.create(LocalizeValue.of("Shift Pointer"));
            CheckBox dialogMode = CheckBox.create(LocalizeValue.of("Dialog Mode"));

            IntBox borderRadius = IntBox.create().withRange(0, 10);
            borderRadius.setValue((Integer)UIManager.get(BORDER_RADIUS_VAR));
            myBorderRadius = borderRadius;

            DockLayout buttonLayout = DockLayout.create();
            for (StaticPosition position : StaticPosition.values()) {
                buttonLayout.add(create(position, typeComboBox::getValue, shiftPointer::getValue, dialogMode::getValue), position);
            }

            layout
                .add(buttonLayout)
                .add(typeComboBox)
                .add(shiftPointer)
                .add(dialogMode)
                .add(DockLayout.create().right(Label.create(LocalizeValue.of("Border Radius"))).center(borderRadius));

            return (JComponent)TargetAWT.to(layout);
        }

        private Button create(
            StaticPosition position,
            Supplier<MessageType> messageTypeSupplier,
            Supplier<Boolean> shiftPointerSupplier,
            Supplier<Boolean> dialogModeSupplier
        ) {
            Button button = Button.create(LocalizeValue.ofNullable(position.name()));
            button.addClickListener(event -> {
                MessageType messageType = messageTypeSupplier.get();

                Balloon.Position bPosition = switch (position) {
                    case TOP -> Balloon.Position.above;
                    case BOTTOM -> Balloon.Position.below;
                    case LEFT -> Balloon.Position.atLeft;
                    case RIGHT -> Balloon.Position.atRight;
                    default -> null;
                };

                UIManager.put(BORDER_RADIUS_VAR, myBorderRadius.getValue());
                Balloon balloon = JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder("<html>" + position.name() + "</html>", null, messageType.getPopupBackground(), null)
                    .setHideOnClickOutside(true)
                    .setBorderColor(messageType.getBorderColor())
                    .setHideOnFrameResize(false)
                    .setHideOnKeyOutside(true)
                    .setHideOnAction(true)
                    .setShowCallout(position != StaticPosition.CENTER)
                    .setPointerShiftedToStart(shiftPointerSupplier.get())
                    .setDialogMode(dialogModeSupplier.get())
                    .createBalloon();

                JComponent awtComponent = (JComponent)TargetAWT.to(event.getComponent());

                Rectangle bounds = awtComponent.getBounds();
                Point target = UIUtil.getCenterPoint(bounds, new Dimension(1, 1));
                switch (position) {
                    case TOP -> target.y = 0;
                    case BOTTOM -> target.y = bounds.height - 3;
                    case LEFT -> target.x = 0;
                    case RIGHT -> target.x = bounds.width;
                }

                RelativePoint point = new RelativePoint(awtComponent, target);

                if (bPosition != null) {
                    balloon.show(point, bPosition);
                }
                else {
                    balloon.showInCenterOf(awtComponent);
                }
            });
            return button;
        }
    }

    public ShowBallonTestAction() {
        super(LocalizeValue.localizeTODO("Show Balloon Test"));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        BalloonTestDialog testDialog = new BalloonTestDialog(e.getData(Project.KEY));
        testDialog.showAsync();
    }
}
