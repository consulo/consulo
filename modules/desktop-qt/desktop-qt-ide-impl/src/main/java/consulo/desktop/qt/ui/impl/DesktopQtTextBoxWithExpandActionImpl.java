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
package consulo.desktop.qt.ui.impl;

import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import io.qt.gui.QAction;
import io.qt.gui.QIcon;
import io.qt.widgets.QDialog;
import io.qt.widgets.QDialogButtonBox;
import io.qt.widgets.QLineEdit;
import io.qt.widgets.QPlainTextEdit;
import io.qt.widgets.QStyle;
import io.qt.widgets.QVBoxLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtTextBoxWithExpandActionImpl extends DesktopQtTextBoxImpl
    implements TextBoxWithExpandAction, DesktopQtIconOwner {
    private static final int DIALOG_WIDTH = 500;
    private static final int DIALOG_HEIGHT = 350;

    private final @Nullable Image myEditButtonImage;
    private final Function<String, List<String>> myParser;
    private final Function<List<String>, String> myJoiner;

    private String myDialogTitle;

    private @Nullable QAction myExpandAction;

    public DesktopQtTextBoxWithExpandActionImpl(
        @Nullable Image editButtonImage,
        String dialogTitle,
        Function<String, List<String>> parser,
        Function<List<String>, String> joiner
    ) {
        super("");

        myEditButtonImage = editButtonImage;
        myDialogTitle = StringUtil.notNullize(dialogTitle);
        myParser = parser;
        myJoiner = joiner;
    }

    @Override
    protected void initialize(QLineEdit component) {
        super.initialize(component);

        myExpandAction = component.addAction(expandIcon(), QLineEdit.ActionPosition.TrailingPosition);
        myExpandAction.setToolTip(myDialogTitle);
        myExpandAction.setEnabled(isEditable());
        myExpandAction.triggered.connect(this::showExpandDialog);
    }

    @Override
    public void refreshIcons() {
        if (myExpandAction != null) {
            myExpandAction.setIcon(expandIcon());
        }
    }

    private QIcon expandIcon() {
        if (myEditButtonImage instanceof DesktopQtImage qtImage) {
            return qtImage.toQIcon();
        }

        // the api lets the caller leave the icon out, and the awt frontend then takes one from the look and feel
        return myComponent == null
            ? new QIcon()
            : myComponent.style().standardIcon(QStyle.StandardPixmap.SP_TitleBarMaxButton);
    }

    /**
     * The api splits the text into lines only while it is being edited, so the parser runs on the way into the
     * dialog and the joiner on the way back - the value itself is never held in the split form.
     */
    @RequiredUIAccess
    private void showExpandDialog() {
        QLineEdit component = myComponent;
        if (component == null) {
            return;
        }

        QDialog dialog = new QDialog(component.window());
        dialog.setWindowTitle(myDialogTitle);
        dialog.resize(DIALOG_WIDTH, DIALOG_HEIGHT);

        QVBoxLayout layout = new QVBoxLayout(dialog);

        QPlainTextEdit editor = new QPlainTextEdit(dialog);
        editor.setPlainText(String.join("\n", myParser.apply(StringUtil.notNullize(getValue()))));
        layout.addWidget(editor);

        QDialogButtonBox buttons = new QDialogButtonBox(dialog);
        buttons.setStandardButtons(QDialogButtonBox.StandardButton.Ok, QDialogButtonBox.StandardButton.Cancel);
        buttons.accepted.connect(dialog::accept);
        buttons.rejected.connect(dialog::reject);
        layout.addWidget(buttons);

        try {
            if (dialog.exec() != QDialog.DialogCode.Accepted.value()) {
                return;
            }

            List<String> lines = new ArrayList<>();
            for (String line : StringUtil.splitByLines(editor.toPlainText())) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }

            setValue(myJoiner.apply(lines), true);
        }
        finally {
            dialog.dispose();
        }
    }

    @Override
    public void disposeQt() {
        super.disposeQt();

        myExpandAction = null;
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);

        if (myExpandAction != null) {
            myExpandAction.setEnabled(editable);
        }
    }

    @Override
    public TextBoxWithExpandAction withDialogTitle(String text) {
        myDialogTitle = StringUtil.notNullize(text);

        if (myExpandAction != null) {
            myExpandAction.setToolTip(myDialogTitle);
        }

        return this;
    }
}
