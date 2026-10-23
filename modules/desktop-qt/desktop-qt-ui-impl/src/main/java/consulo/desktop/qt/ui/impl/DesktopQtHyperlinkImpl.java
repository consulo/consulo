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

import consulo.desktop.qt.ui.impl.base.DesktopQtImageWidget;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.localize.LocalizeValue;
import consulo.ui.Hyperlink;
import consulo.ui.HyperlinkStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import consulo.ui.internal.SearchHighlightUtil;
import consulo.util.lang.xml.XmlStringUtil;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.Qt;
import consulo.platform.base.icon.PlatformIconGroup;
import io.qt.gui.QCursor;
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
    private @Nullable String mySearchHighlight;
    private boolean myUnderline;
    private boolean myHovered;
    private boolean myDropDown;

    private @Nullable DesktopQtImageWidget myIconLabel;
    private @Nullable QLabel myTextLabel;
    private @Nullable DesktopQtImageWidget myDropDownLabel;

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

        myIconLabel = new DesktopQtImageWidget(component);
        myIconLabel.setVisible(false);
        layout.addWidget(myIconLabel);

        myTextLabel = new QLabel(component);
        myTextLabel.setTextFormat(Qt.TextFormat.RichText);
        myTextLabel.setTextInteractionFlags(
            Qt.TextInteractionFlag.LinksAccessibleByMouse,
            Qt.TextInteractionFlag.LinksAccessibleByKeyboard
        );
        myTextLabel.setText(buildHtml());
        myTextLabel.installEventFilter(new QObject(myTextLabel) {
            @Override
            public boolean eventFilter(@Nullable QObject watched, @Nullable QEvent event) {
                if (event != null && (event.type() == QEvent.Type.Enter || event.type() == QEvent.Type.Leave)) {
                    setHovered(event.type() == QEvent.Type.Enter);
                }
                return false;
            }
        });
        layout.addWidget(myTextLabel);

        myTextLabel.linkActivated.connect(href ->
            getListenerDispatcher(HyperlinkEvent.class)
                .onEvent(new HyperlinkEvent(DesktopQtHyperlinkImpl.this, "", DesktopQtCurrentInput.current(myTextLabel)))
        );

        myDropDownLabel = new DesktopQtImageWidget(component);
        myDropDownLabel.setImage(PlatformIconGroup.generalLinkdroptriangle());
        myDropDownLabel.setVisible(myDropDown);
        myDropDownLabel.setCursor(new QCursor(Qt.CursorShape.PointingHandCursor));
        myDropDownLabel.installEventFilter(new QObject(myDropDownLabel) {
            @Override
            public boolean eventFilter(@Nullable QObject watched, @Nullable QEvent event) {
                if (event != null && event.type() == QEvent.Type.MouseButtonRelease) {
                    getListenerDispatcher(HyperlinkEvent.class)
                        .onEvent(new HyperlinkEvent(DesktopQtHyperlinkImpl.this, "", DesktopQtCurrentInput.current(myDropDownLabel)));
                    return true;
                }
                return false;
            }
        });
        layout.addWidget(myDropDownLabel);

        updateIcon();
    }

    private String buildHtml() {
        String text = QtMnemonic.plain(myText);
        String highlighted = DesktopQtSearchHighlight.toRichText(text, mySearchHighlight);
        String decoration = myUnderline || myHovered ? "underline" : "none";
        return "<a href=\"click\" style=\"text-decoration: " + decoration + "\">"
            + (highlighted == null ? XmlStringUtil.escapeText(text) : highlighted) + "</a>";
    }

    private void setHovered(boolean hovered) {
        if (myHovered != hovered) {
            myHovered = hovered;
            updateHtml();
        }
    }

    private void updateHtml() {
        QLabel textLabel = myTextLabel;
        if (textLabel != null && !textLabel.isDisposed()) {
            textLabel.setText(buildHtml());
        }
    }

    @Override
    public void addStyle(HyperlinkStyle style) {
        switch (style) {
            case UNDERLINE -> {
                myUnderline = true;
                updateHtml();
            }
            case DROP_DOWN -> {
                myDropDown = true;
                DesktopQtImageWidget dropDownLabel = myDropDownLabel;
                if (dropDownLabel != null && !dropDownLabel.isDisposed()) {
                    dropDownLabel.setVisible(true);
                }
            }
        }
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

    @RequiredUIAccess
    @Override
    public void setSearchHighlight(@Nullable String query) {
        mySearchHighlight = SearchHighlightUtil.normalizeQuery(query);

        if (myTextLabel != null) {
            myTextLabel.setText(buildHtml());
        }
    }

    @Override
    public @Nullable String getSearchHighlight() {
        return mySearchHighlight;
    }

    @Override
    public void setIcon(@Nullable Image icon) {
        myIcon = icon;

        updateIcon();
    }

    private void updateIcon() {
        DesktopQtImageWidget iconLabel = myIconLabel;
        if (iconLabel == null || iconLabel.isDisposed()) {
            return;
        }

        iconLabel.setImage(myIcon);
        iconLabel.setVisible(myIcon != null);
    }

    @Override
    public void refreshIcons() {
        if (myIcon != null) {
            updateIcon();
        }
        DesktopQtImageWidget dropDownLabel = myDropDownLabel;
        if (dropDownLabel != null && !dropDownLabel.isDisposed()) {
            dropDownLabel.setImage(PlatformIconGroup.generalLinkdroptriangle());
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
        myDropDownLabel = null;

        super.disposeQt();
    }
}
