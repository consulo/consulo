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
package consulo.desktop.qt.ui.impl.layout;

import consulo.desktop.qt.ui.impl.DesktopQtCurrentInput;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.desktop.qt.ui.impl.QtMnemonic;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.FoldoutLayout;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.event.FoldoutLayoutOpenedEvent;
import io.qt.core.Qt;
import io.qt.gui.QFont;
import io.qt.widgets.QFrame;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QLayout;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QToolButton;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtFoldoutLayoutImpl extends DesktopQtLayoutComponent<LayoutConstraint, Object> implements FoldoutLayout {
    /**
     * A tool button centers its label and keeps the frame of the style even when auto raised - both have to go for
     * the header to read as the caption of a section rather than as a button sitting above one.
     */
    private static final String HEADER_STYLE_SHEET = """
        QToolButton { border: none; background: transparent; text-align: left; padding: 2px 0px; }
        QToolButton:hover { color: palette(highlight); }
        """;

    /** what the awt decorator indents the body of a section by, so the caption reads as the parent of it */
    private static final int BODY_INDENT = 16;

    private LocalizeValue myTitle;

    private boolean myState;

    private @Nullable QToolButton myHeader;
    private @Nullable QWidget myBody;

    public DesktopQtFoldoutLayoutImpl(LocalizeValue titleValue, Component component, boolean state) {
        myTitle = titleValue;
        myState = state;

        addImpl(component, null);
    }

    @Override
    protected boolean isSingleChild() {
        return true;
    }

    @Override
    protected QLayout createLayout() {
        return new QVBoxLayout();
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        QWidget widget = new QWidget(parent);

        QLayout layout = createLayout();
        layout.setContentsMargins(0, 0, 0, 0);
        widget.setLayout(layout);

        QWidget headerRow = new QWidget(widget);
        QHBoxLayout headerLayout = new QHBoxLayout(headerRow);
        headerLayout.setContentsMargins(0, 0, 0, 0);
        headerLayout.setSpacing(6);

        myHeader = new QToolButton(headerRow);
        myHeader.setCheckable(true);
        myHeader.setAutoRaise(true);
        myHeader.setToolButtonStyle(Qt.ToolButtonStyle.ToolButtonTextBesideIcon);
        myHeader.setSizePolicy(QSizePolicy.Policy.Maximum, QSizePolicy.Policy.Fixed);
        myHeader.setStyleSheet(HEADER_STYLE_SHEET);
        myHeader.setText(QtMnemonic.plain(myTitle));

        QFont headerFont = new QFont(myHeader.font());
        headerFont.setBold(true);
        myHeader.setFont(headerFont);

        headerLayout.addWidget(myHeader);

        QFrame separator = new QFrame(headerRow);
        separator.setFrameShape(QFrame.Shape.HLine);
        separator.setFrameShadow(QFrame.Shadow.Sunken);
        headerLayout.addWidget(separator, 1);

        layout.addWidget(headerRow);

        myBody = new QWidget(widget);
        QVBoxLayout bodyLayout = new QVBoxLayout(myBody);
        bodyLayout.setContentsMargins(BODY_INDENT, 0, 0, 0);

        layout.addWidget(myBody);

        return widget;
    }

    @Override
    protected void initialize(QWidget component) {
        super.initialize(component);

        if (myHeader != null) {
            // the state is written before the signal is wired, so that binding a layout does not report an
            // opening which the caller never asked for
            myHeader.setChecked(myState);

            applyState(myState);

            myHeader.toggled.connect(this::onToggled);
        }
    }

    /**
     * The body of the section is a container of its own rather than the widget of the child, so that folding does
     * not fight {@link Component#setVisible} of whatever was put inside.
     */
    @Override
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        if (!isAlive(myBody)) {
            return;
        }

        myBody.layout().addWidget(child.toQtComponent());
    }

    @Override
    protected void detach(QtComponentDelegate<?> child) {
        QWidget widget = child.toQtComponent();
        if (!isAlive(widget) || !isAlive(myBody)) {
            return;
        }

        myBody.layout().removeWidget(widget);
    }

    @Override
    public void disposeQt() {
        super.disposeQt();

        myHeader = null;
        myBody = null;
    }

    @RequiredUIAccess
    private void onToggled(boolean state) {
        applyState(state);

        getListenerDispatcher(FoldoutLayoutOpenedEvent.class)
            .onEvent(new FoldoutLayoutOpenedEvent(this, DesktopQtCurrentInput.current(myComponent), state));
    }

    private void applyState(boolean state) {
        myState = state;

        if (myHeader != null) {
            myHeader.setArrowType(state ? Qt.ArrowType.DownArrow : Qt.ArrowType.RightArrow);
        }

        if (isAlive(myBody)) {
            myBody.setVisible(state);
        }
    }

    @Override
    @RequiredUIAccess
    public FoldoutLayout setState(boolean showing) {
        if (myHeader != null) {
            // toggling reports the change, the way the awt decorator does when it is switched from the outside
            myHeader.setChecked(showing);
        }
        else {
            myState = showing;
        }

        return this;
    }

    @Override
    @RequiredUIAccess
    public FoldoutLayout setTitle(LocalizeValue title) {
        myTitle = title;

        if (myHeader != null) {
            myHeader.setText(QtMnemonic.plain(myTitle));
        }

        return this;
    }
}
