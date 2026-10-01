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

import consulo.ui.image.Image;
import io.qt.core.Qt;
import io.qt.widgets.QFrame;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QLabel;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

public class DesktopQtIconLabel extends QFrame {
    private static final int ICON_TEXT_GAP = 4;

    private final DesktopQtImageWidget myImageWidget;
    private final QLabel myTextLabel;

    private @Nullable Image myImage;

    public DesktopQtIconLabel(@Nullable QWidget parent) {
        super(parent);

        QHBoxLayout layout = new QHBoxLayout(this);
        layout.setContentsMargins(0, 0, 0, 0);
        layout.setSpacing(ICON_TEXT_GAP);

        myImageWidget = new DesktopQtImageWidget(this);
        myImageWidget.setVisible(false);
        layout.addWidget(myImageWidget);

        myTextLabel = new DesktopQtWrappingLabel(this);
        layout.addWidget(myTextLabel, 1);
    }

    public @Nullable Image getImage() {
        return myImage;
    }

    public void setImage(@Nullable Image image) {
        myImage = image;

        myImageWidget.setImage(image);
        myImageWidget.setVisible(image != null);

        updateTextVisibility();
    }

    public String text() {
        return myTextLabel.text();
    }

    public void setText(String text) {
        myTextLabel.setText(text);

        updateTextVisibility();
    }

    public @Nullable QWidget buddy() {
        return myTextLabel.buddy();
    }

    public void setBuddy(@Nullable QWidget buddy) {
        myTextLabel.setBuddy(buddy);
    }

    public void setTextFormat(Qt.TextFormat format) {
        myTextLabel.setTextFormat(format);
    }

    public void setWordWrap(boolean wordWrap) {
        myTextLabel.setWordWrap(wordWrap);
        myTextLabel.setMinimumWidth(wordWrap ? 1 : 0);
    }

    public void setTextInteractionFlags(Qt.TextInteractionFlag... flags) {
        myTextLabel.setTextInteractionFlags(flags);
    }

    private void updateTextVisibility() {
        myTextLabel.setVisible(myImage == null || !myTextLabel.text().isEmpty());
    }
}
