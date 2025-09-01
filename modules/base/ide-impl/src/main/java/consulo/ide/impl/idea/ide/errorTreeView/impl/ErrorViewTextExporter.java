/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.errorTreeView.impl;

import consulo.ide.impl.idea.ide.errorTreeView.ErrorTreeElement;
import consulo.ide.impl.idea.ide.errorTreeView.ErrorViewStructure;
import consulo.ide.impl.idea.ide.errorTreeView.NavigatableMessageElement;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.Platform;
import consulo.ui.ex.action.ExporterToTextFile;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.util.TooManyListenersException;

public class ErrorViewTextExporter implements ExporterToTextFile {
    private final JCheckBox myCbShowDetails;
    private final ErrorViewStructure myStructure;
    private ChangeListener myChangeListener;

    public ErrorViewTextExporter(ErrorViewStructure treeStructure) {
        myStructure = treeStructure;
        myCbShowDetails = new JCheckBox(IdeLocalize.checkboxErrortreeExportDetails().get());
        myCbShowDetails.setSelected(true);
        myCbShowDetails.addActionListener(e -> myChangeListener.stateChanged(null));
    }

    @Override
    public JComponent getSettingsEditor() {
        return myCbShowDetails;
    }

    @Override
    public void addSettingsChangedListener(ChangeListener listener) throws TooManyListenersException {
        if (myChangeListener != null) {
            throw new TooManyListenersException();
        }
        myChangeListener = listener;
    }

    @Override
    public void removeSettingsChangedListener(ChangeListener listener) {
        myChangeListener = null;
    }

    @Nonnull
    @Override
    public String getReportText() {
        StringBuffer buffer = new StringBuffer();
        getReportText(buffer, (ErrorTreeElement) myStructure.getRootElement(), myCbShowDetails.isSelected(), 0);
        return buffer.toString();
    }

    @Nonnull
    @Override
    public String getDefaultFilePath() {
        return "";
    }

    @Override
    public void exportedTo(@Nonnull String filePath) {
    }

    @Override
    public boolean canExport() {
        return true;
    }

    private void getReportText(StringBuffer buffer, ErrorTreeElement element, boolean withUsages, int indent) {
        String newline = Platform.current().os().lineSeparator().getSeparatorString();
        Object[] children = myStructure.getChildElements(element);
        for (Object child : children) {
            if (!(child instanceof ErrorTreeElement childElement)) {
                continue;
            }
            if (!withUsages && child instanceof NavigatableMessageElement) {
                continue;
            }
            if (buffer.length() > 0) {
                buffer.append(newline);
            }
            shift(buffer, indent);
            exportElement(childElement, buffer, indent, newline);
            getReportText(buffer, childElement, withUsages, indent + 4);
        }
    }

    public static void exportElement(ErrorTreeElement element, StringBuffer buffer, int baseIntent, String newline) {
        int startLength = buffer.length();
        buffer.append(element.getKind().getPresentableText());
        buffer.append(element.getExportTextPrefix());
        int localIndent = startLength - buffer.length();

        String[] text = element.getText();
        if (text != null && text.length > 0) {
            buffer.append(text[0]);
            for (int i = 1; i < text.length; i++) {
                buffer.append(newline);
                shift(buffer, baseIntent + localIndent);
                buffer.append(text[i]);
            }
        }
    }

    private static void shift(StringBuffer buffer, int indent) {
        for (int i = 0; i < indent; i++) {
            buffer.append(' ');
        }
    }
}
