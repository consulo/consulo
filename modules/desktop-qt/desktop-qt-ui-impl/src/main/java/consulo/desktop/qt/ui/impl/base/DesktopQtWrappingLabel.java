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
package consulo.desktop.qt.ui.impl.base;

import io.qt.core.QMargins;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QTextDocument;
import io.qt.widgets.QLabel;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

public class DesktopQtWrappingLabel extends QLabel {
    private @Nullable QTextDocument myMeasureDocument;

    public DesktopQtWrappingLabel(@Nullable QWidget parent) {
        super(parent);
    }

    @Override
    public QSize sizeHint() {
        QSize hint = super.sizeHint();
        String text = text();
        if (!wordWrap() || text.isEmpty()) {
            return hint;
        }

        QMargins margins = contentsMargins();
        int lineWidth = (int) Math.ceil(measure(text).idealWidth()) + margins.left() + margins.right() + 2 * margin();
        int width = Math.max(lineWidth, hint.width());
        return new QSize(width, heightForWidth(width));
    }

    private QTextDocument measure(String text) {
        QTextDocument document = myMeasureDocument;
        if (document == null) {
            document = new QTextDocument(this);
            document.setDocumentMargin(0);
            myMeasureDocument = document;
        }

        document.setDefaultFont(font());

        Qt.TextFormat format = textFormat();
        if (format == Qt.TextFormat.RichText) {
            document.setHtml(text);
        }
        else if (format == Qt.TextFormat.MarkdownText) {
            document.setMarkdown(text);
        }
        else {
            document.setPlainText(text);
        }
        return document;
    }
}
