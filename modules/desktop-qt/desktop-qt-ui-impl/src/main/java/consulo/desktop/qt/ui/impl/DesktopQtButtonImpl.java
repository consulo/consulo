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
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.details.InputDetails;
import consulo.ui.image.Image;
import consulo.ui.internal.SearchHighlightUtil;
import consulo.ui.style.ComponentColors;
import consulo.ui.color.RGBColor;
import io.qt.core.QRect;
import io.qt.core.QRectF;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QIcon;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QResizeEvent;
import io.qt.widgets.QLineEdit;
import io.qt.widgets.QPushButton;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QStyleOptionButton;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtButtonImpl extends QtComponentDelegate<QPushButton> implements Button, DesktopQtIconOwner {
    protected class HighlightPushButton extends QPushButton {
        private boolean myFieldHeight;

        private @Nullable QLineEdit myMetricsLineEdit;

        protected HighlightPushButton(QWidget parent) {
            super(parent);
        }

        private void setFieldHeight(boolean fieldHeight) {
            if (fieldHeight && myMetricsLineEdit == null) {
                myMetricsLineEdit = new QLineEdit(this);
                myMetricsLineEdit.setVisible(false);
            }

            if (myFieldHeight != fieldHeight) {
                myFieldHeight = fieldHeight;

                updateGeometry();
            }
        }

        private QSize withFieldHeight(QSize size) {
            QLineEdit metricsLineEdit = myMetricsLineEdit;
            if (!myFieldHeight || metricsLineEdit == null) {
                return size;
            }

            return new QSize(size.width(), Math.max(size.height(), metricsLineEdit.sizeHint().height()));
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            if (isVertical()) {
                paintVertical();
                return;
            }

            String query = mySearchHighlight;
            String text = QtMnemonic.plain(myText);
            if (query == null || !SearchHighlightUtil.matches(text, query)) {
                super.paintEvent(event);
                return;
            }

            QStyleOptionButton option = new QStyleOptionButton();
            initStyleOption(option);

            DesktopQtSearchHighlight.paintPushButton(this, option, text, query);
        }

        @Override
        protected void resizeEvent(QResizeEvent event) {
            super.resizeEvent(event);

            layoutSuffix();
        }

        @Override
        public QSize sizeHint() {
            if (isVertical()) {
                return verticalSizeHint();
            }
            return withFieldHeight(super.sizeHint());
        }

        @Override
        public QSize minimumSizeHint() {
            if (isVertical()) {
                return verticalSizeHint();
            }
            return withFieldHeight(super.minimumSizeHint());
        }

        private QSize verticalSizeHint() {
            QFontMetrics fm = fontMetrics();
            String text = QtMnemonic.plain(myText);
            Image icon = myIcon;

            int thickness = fm.height();
            int length = 0;
            if (icon != null) {
                thickness = Math.max(thickness, icon.getWidth());
                length += icon.getHeight();
            }
            if (!text.isEmpty()) {
                if (length > 0) {
                    length += VERTICAL_GAP;
                }
                length += fm.horizontalAdvance(text);
            }
            return new QSize(thickness + VERTICAL_PADDING * 2, length + VERTICAL_PADDING * 2);
        }

        private void paintVertical() {
            QPainter painter = new QPainter(this);
            try {
                painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
                painter.setRenderHint(QPainter.RenderHint.TextAntialiasing, true);
                painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, true);

                if (isDown() || underMouse()) {
                    QColor highlight = palette().color(QPalette.ColorRole.Highlight);
                    painter.save();
                    try {
                        painter.setPen(Qt.PenStyle.NoPen);
                        painter.setBrush(new QColor(highlight.red(), highlight.green(), highlight.blue(), isDown() ? 110 : 55));
                        painter.drawRoundedRect(new QRectF(0, 0, width(), height()), 3, 3);
                    }
                    finally {
                        painter.restore();
                    }
                }

                int top = VERTICAL_PADDING;

                Image icon = myIcon;
                if (icon != null) {
                    QRect iconRect = new QRect((width() - icon.getWidth()) / 2, top, icon.getWidth(), icon.getHeight());
                    DesktopQtImage.paint(painter, iconRect, isEnabled() ? QIcon.Mode.Normal : QIcon.Mode.Disabled, icon, null);
                    top += icon.getHeight() + VERTICAL_GAP;
                }

                String text = QtMnemonic.plain(myText);
                if (!text.isEmpty()) {
                    QFontMetrics fm = fontMetrics();
                    QPalette.ColorGroup group = isEnabled() ? QPalette.ColorGroup.Active : QPalette.ColorGroup.Disabled;
                    painter.setPen(palette().color(group, QPalette.ColorRole.ButtonText));
                    painter.rotate(-90);
                    int baseline = (width() - fm.height()) / 2 + fm.ascent();
                    painter.drawText(-(top + fm.horizontalAdvance(text)), baseline, text);
                }
            }
            finally {
                painter.end();
            }
        }
    }

    private static final int VERTICAL_PADDING = 6;

    private static final int VERTICAL_GAP = 4;

    private static final String VERTICAL_STYLE_SHEET =
        "QPushButton { border: none; background: transparent; padding: 0px; min-width: 0px; min-height: 0px; }";

    /**
     * A qt push button reserves four pixels of width of its own for an icon, on top of whatever padding is asked
     * for - so a padding written the same on both axes draws an icon only button four pixels wider than it is
     * tall. Two pixels are taken off each side to land on the square the awt frontend draws.
     */
    private static final int SUFFIX_GAP = 4;

    private static final String ICON_PADDING = "3px 1px";

    /**
     * What flatlaf gives a button carrying text - {@code Button.margin} of 2,14 plus the one pixel border - and
     * what the awt frontend therefore measures such a button by.
     */
    private static final String TEXT_PADDING = "2px 14px";

    /**
     * A push button is measured by whatever desktop style qt picked up - breeze answers 84x34 for a one word
     * label, where the awt frontend draws the same button 72x23 - so the box it is drawn in is spelled out here,
     * with the padding and the minimum width the awt look and feel carries. The frame is left to the style, which
     * is what keeps a plain button looking native.
     */
    private static final String DEFAULT_STYLE_SHEET = "QPushButton { padding: %s; min-width: 42px; }";

    /**
     * An icon only button carries no text to be measured by, and flatlaf drops the minimum width for it and pads
     * it evenly instead - a square of the icon and three pixels on every side.
     */
    private static final String DEFAULT_ICON_STYLE_SHEET = "QPushButton { padding: " + ICON_PADDING + "; }";

    /**
     * A flat button loses every state qt used to draw with the frame, so hover, pressed and checked are given back
     * here. The rounded fill is {@code Button.arc} of the awt look and feel, which is an arc diameter of six.
     */
    private static final String FLAT_STYLE_SHEET = """
        QPushButton { border: none; background: transparent; border-radius: 3px; padding: %s; }
        QPushButton:hover { background: rgba(128, 128, 128, 50); }
        QPushButton:pressed { background: rgba(128, 128, 128, 90); }
        QPushButton:checked { background: rgba(128, 128, 128, 90); }
        """;

    private static final String HELP_STYLE_SHEET = """
        QPushButton { border: 1px solid %s; border-radius: 11px; background: transparent; padding: 0px; min-width: 20px; max-width: 20px; min-height: 20px; max-height: 20px; }
        QPushButton:hover { background: rgba(128, 128, 128, 40); }
        QPushButton:pressed { background: rgba(128, 128, 128, 80); }
        """;

    private LocalizeValue myText = LocalizeValue.empty();

    private @Nullable String mySearchHighlight;
    private @Nullable Image myIcon;

    private boolean myFlat;

    private @Nullable Component mySuffixComponent;

    private final Set<ButtonStyle> myStyles = EnumSet.noneOf(ButtonStyle.class);

    public DesktopQtButtonImpl(LocalizeValue text) {
        myText = Objects.requireNonNull(text);
    }

    @Override
    protected QPushButton createQt(QWidget parent) {
        return new HighlightPushButton(parent);
    }

    @Override
    protected void initialize(QPushButton component) {
        component.setText(QtMnemonic.withMnemonic(myText));

        updateIcon();

        for (ButtonStyle style : myStyles) {
            applyStyle(style);
        }

        attachSuffix();

        updateStyleSheet();

        component.clicked.connect(() -> getListenerDispatcher(ClickEvent.class)
            .onEvent(new ClickEvent(DesktopQtButtonImpl.this, DesktopQtInputDetails.mouseAtCursor(component)))
        );
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> Disposable addListener(
        Class<? extends E> eventClass,
        ComponentEventListener<C, E> listener
    ) {
        if (eventClass == ClickEvent.class) {
            return myDataObject.addListener(eventClass, listener);
        }
        return super.addListener(eventClass, listener);
    }

    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @RequiredUIAccess
    @Override
    public void setText(LocalizeValue text) {
        myText = text;

        if (myComponent != null) {
            myComponent.setText(QtMnemonic.withMnemonic(myText));

            // whether the button carries text is what decides how it is padded
            updateStyleSheet();
        }
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }

    @RequiredUIAccess
    @Override
    public void setIcon(@Nullable Image image) {
        myIcon = image;

        updateIcon();

        if (myComponent != null) {
            updateStyleSheet();
        }
    }

    private void updateIcon() {
        if (myComponent == null) {
            return;
        }

        Image icon = myIcon;
        if (icon instanceof DesktopQtImage) {
            myComponent.setIcon(toHostQIcon(icon));
            myComponent.setIconSize(new QSize(icon.getWidth(), icon.getHeight()));
        }
        else {
            myComponent.setIcon(new QIcon());
        }
    }

    @Override
    public void refreshIcons() {
        if (myIcon != null) {
            updateIcon();
        }
    }

    @RequiredUIAccess
    @Override
    public void invoke(InputDetails inputDetails) {
        getListenerDispatcher(ClickEvent.class).onEvent(new ClickEvent(this, inputDetails));
    }

    @Override
    public void addStyle(ButtonStyle style) {
        myStyles.add(style);

        if (style == ButtonStyle.HELP) {
            if (myIcon == null) {
                myIcon = PlatformIconGroup.actionsHelp();

                updateIcon();
            }
        }

        applyStyle(style);
    }

    private void applyStyle(ButtonStyle style) {
        if (myComponent == null) {
            return;
        }

        switch (style) {
            case PRIMARY -> {
                myComponent.setDefault(true);
                myComponent.setAutoDefault(true);
            }
            case BORDERLESS, INPLACE -> applyFlat();
            case TOOLBAR -> {
                myComponent.setFocusPolicy(Qt.FocusPolicy.NoFocus);

                applyFlat();
            }
            case HELP -> {
                myComponent.setAutoDefault(false);
                myComponent.setFocusPolicy(Qt.FocusPolicy.TabFocus);

                updateStyleSheet();
            }
            case VERTICAL -> {
                myComponent.setAutoDefault(false);
                myComponent.setFocusPolicy(Qt.FocusPolicy.NoFocus);
                myComponent.setAttribute(Qt.WidgetAttribute.WA_Hover, true);
                myComponent.setSizePolicy(QSizePolicy.Policy.Fixed, QSizePolicy.Policy.Fixed);

                updateStyleSheet();
                myComponent.updateGeometry();
            }
        }
    }

    private boolean isVertical() {
        return myStyles.contains(ButtonStyle.VERTICAL);
    }

    private void applyFlat() {
        myComponent.setAutoDefault(false);
        // qt keeps painting the frame of a flat button on some styles, so the border is also cleared by sheet
        myComponent.setFlat(true);

        myFlat = true;

        updateStyleSheet();
    }

    @RequiredUIAccess
    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        Component oldSuffix = mySuffixComponent;
        if (oldSuffix == suffixComponent) {
            return;
        }

        if (oldSuffix instanceof QtComponentDelegate<?> delegate) {
            delegate.setParent(null);
        }

        mySuffixComponent = suffixComponent;

        if (myComponent != null) {
            attachSuffix();
            updateStyleSheet();
        }
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    private void attachSuffix() {
        QPushButton host = myComponent;
        if (host == null || !(mySuffixComponent instanceof QtComponentDelegate<?> delegate)) {
            return;
        }

        delegate.setParent(this);
        delegate.bind(host, null);

        QWidget widget = delegate.toQtComponent();
        if (widget != null) {
            widget.setAttribute(Qt.WidgetAttribute.WA_TransparentForMouseEvents, true);
            widget.show();
        }

        layoutSuffix();
    }

    private @Nullable QWidget suffixWidget() {
        QPushButton host = myComponent;
        QWidget widget = mySuffixComponent instanceof QtComponentDelegate<?> delegate ? delegate.toQtComponent() : null;
        return host != null && widget != null && !widget.isDisposed() && widget.parentWidget() == host && !isVertical() ? widget : null;
    }

    private void layoutSuffix() {
        QPushButton host = myComponent;
        QWidget suffix = suffixWidget();
        if (host == null || suffix == null) {
            return;
        }

        QSize hint = suffix.sizeHint();
        int right = host.width() - rightPadding(currentPadding());
        suffix.setGeometry(right - hint.width(), (host.height() - hint.height()) / 2, hint.width(), hint.height());
    }

    private String currentPadding() {
        boolean iconOnly = myIcon != null && myText.isEmpty();
        return iconOnly ? ICON_PADDING : textPadding();
    }

    private static int rightPadding(String padding) {
        String[] parts = padding.trim().split("\\s+");
        String right = parts.length >= 2 ? parts[1] : parts[0];
        try {
            return Integer.parseInt(right.replace("px", ""));
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    private String withSuffixPadding(String styleSheet) {
        QWidget suffix = suffixWidget();
        if (suffix == null) {
            return styleSheet;
        }
        int padding = rightPadding(currentPadding()) + suffix.sizeHint().width() + SUFFIX_GAP;
        return styleSheet + "\nQPushButton { padding-right: " + padding + "px; }";
    }

    private static String helpBorderColor() {
        RGBColor rgb = ComponentColors.BORDER.toRGB();
        return "rgba(%d, %d, %d, %d)".formatted(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), rgb.getAlpha());
    }

    protected String textPadding() {
        return TEXT_PADDING;
    }

    protected void updateStyleSheet() {
        boolean iconOnly = myIcon != null && myText.isEmpty();
        boolean fieldHeight = false;

        if (isVertical()) {
            setOwnStyleSheet(VERTICAL_STYLE_SHEET);
        }
        else if (myStyles.contains(ButtonStyle.HELP) && iconOnly) {
            setOwnStyleSheet(HELP_STYLE_SHEET.formatted(helpBorderColor()));
        }
        else if (myFlat) {
            setOwnStyleSheet(withSuffixPadding(FLAT_STYLE_SHEET.formatted(iconOnly ? ICON_PADDING : textPadding())));
        }
        else if (iconOnly) {
            setOwnStyleSheet(withSuffixPadding(DEFAULT_ICON_STYLE_SHEET));
        }
        else {
            setOwnStyleSheet(withSuffixPadding(DEFAULT_STYLE_SHEET.formatted(textPadding())));

            fieldHeight = true;
        }

        if (myComponent instanceof HighlightPushButton button) {
            button.setFieldHeight(fieldHeight);
        }
    }

    @RequiredUIAccess
    @Override
    public void setSearchHighlight(@Nullable String query) {
        mySearchHighlight = SearchHighlightUtil.normalizeQuery(query);

        if (myComponent != null) {
            myComponent.update();
        }
    }

    @Override
    public @Nullable String getSearchHighlight() {
        return mySearchHighlight;
    }
}
