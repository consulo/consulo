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
import consulo.localize.LocalizeValue;
import consulo.ui.Hyperlink;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import io.qt.core.Qt;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QLabel;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtHyperlinkImpl extends QtComponentDelegate<QWidget> implements Hyperlink, DesktopQtIconOwner {
    private LocalizeValue myText = LocalizeValue.empty();
    private @Nullable Image myIcon;

    private @Nullable QLabel myIconLabel;
    private @Nullable QLabel myTextLabel;

    public DesktopQtHyperlinkImpl(LocalizeValue text) {
        myText = text;
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        return new QWidget(parent);
    }

    /**
     * a qt label shows either its pixmap or its text, so the icon needs a label of its own next to the link
     */
    @Override
    protected void initialize(QWidget component) {
        QHBoxLayout layout = new QHBoxLayout(component);
        layout.setContentsMargins(0, 0, 0, 0);
        layout.setSpacing(4);

        myIconLabel = new QLabel(component);
        myIconLabel.setVisible(false);
        layout.addWidget(myIconLabel);

        myTextLabel = new QLabel(component);
        myTextLabel.setTextFormat(Qt.TextFormat.RichText);
        myTextLabel.setTextInteractionFlags(
            Qt.TextInteractionFlag.LinksAccessibleByMouse,
            Qt.TextInteractionFlag.LinksAccessibleByKeyboard
        );
        myTextLabel.setText(buildHtml());
        layout.addWidget(myTextLabel);

        myTextLabel.linkActivated.connect(href ->
            getListenerDispatcher(HyperlinkEvent.class)
                .onEvent(new HyperlinkEvent(DesktopQtHyperlinkImpl.this, "", DesktopQtCurrentInput.current(myTextLabel)))
        );

        updateIcon();
    }

    private String buildHtml() {
        return "<a href=\"click\">" + QtMnemonic.plain(myText) + "</a>";
    }

    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @RequiredUIAccess
    @Override
    public void setText(LocalizeValue text) {
        myText = text;

        if (myTextLabel != null) {
            myTextLabel.setText(buildHtml());
        }
    }

    @Override
    public void setIcon(@Nullable Image icon) {
        myIcon = icon;

        updateIcon();
    }

    private void updateIcon() {
        if (myIconLabel == null) {
            return;
        }

        if (myIcon instanceof DesktopQtImage qtImage) {
            myIconLabel.setPixmap(qtImage.toQPixmap());
            myIconLabel.setVisible(true);
        }
        else {
            myIconLabel.setVisible(false);
        }
    }

    @Override
    public void refreshIcons() {
        if (myIcon != null) {
            updateIcon();
        }
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }

    @Override
    public void disposeQt() {
        myIconLabel = null;
        myTextLabel = null;

        super.disposeQt();
    }
}
