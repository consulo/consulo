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

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.LoadingLayout;
import io.qt.core.Qt;
import io.qt.widgets.QLabel;
import io.qt.widgets.QLayout;
import io.qt.widgets.QProgressBar;
import io.qt.widgets.QStackedWidget;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * As in the web frontend the spinner and the content are stacked rather than painted over each other, so the
 * content keeps the layout it already has while it is hidden.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
public class DesktopQtLoadingLayoutImpl<L extends Layout> extends DesktopQtLayoutComponent<LayoutConstraint, Object>
    implements LoadingLayout<L> {

    private final L myInnerLayout;

    private @Nullable QWidget myLoadingPanel;
    private @Nullable QLabel myLoadingLabel;

    private LocalizeValue myLoadingText = LocalizeValue.empty();
    private boolean myLoading;

    public DesktopQtLoadingLayoutImpl(L inner, Disposable parent) {
        myInnerLayout = inner;

        addImpl(inner, null);
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        return new QStackedWidget(parent);
    }

    @Override
    protected @Nullable QLayout createLayout() {
        return null;
    }

    @Override
    protected void initialize(QWidget component) {
        QStackedWidget stacked = (QStackedWidget) component;

        QWidget loadingPanel = new QWidget(stacked);
        QVBoxLayout layout = new QVBoxLayout(loadingPanel);
        layout.setAlignment(Qt.AlignmentFlag.AlignCenter);

        QProgressBar progressBar = new QProgressBar(loadingPanel);
        // an empty range is how qt spells an indeterminate bar
        progressBar.setRange(0, 0);
        progressBar.setTextVisible(false);
        layout.addWidget(progressBar);

        QLabel loadingLabel = new QLabel(myLoadingText.get(), loadingPanel);
        loadingLabel.setAlignment(Qt.AlignmentFlag.AlignCenter);
        layout.addWidget(loadingLabel);

        stacked.addWidget(loadingPanel);

        myLoadingPanel = loadingPanel;
        myLoadingLabel = loadingLabel;

        super.initialize(component);

        updateCurrentPage();
    }

    @Override
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        ((QStackedWidget) myComponent).addWidget(child.toQtComponent());
    }

    @Override
    protected void detach(QtComponentDelegate<?> child) {
        QWidget widget = child.toQtComponent();
        if (isAlive(widget) && isAlive(myComponent)) {
            ((QStackedWidget) myComponent).removeWidget(widget);
        }
    }

    @Override
    @RequiredUIAccess
    public <Value> Future<Value> startLoading(Supplier<Value> valueGetter, BiConsumer<L, Value> uiSetter) {
        UIAccess uiAccess = UIAccess.current();

        startLoading();

        return AppExecutorUtil.getAppScheduledExecutorService().submit(() -> {
            Value value = valueGetter.get();

            uiAccess.give(() -> stopLoading(l -> uiSetter.accept(l, value)));
            return value;
        });
    }

    @Override
    @RequiredUIAccess
    public void startLoading() {
        myInnerLayout.removeAll();
        setLoading(true);
    }

    @Override
    @RequiredUIAccess
    public void startLoading(LocalizeValue loadingText) {
        myInnerLayout.removeAll();
        setLoadingText(loadingText);
        setLoading(true);
    }

    @Override
    @RequiredUIAccess
    public void stopLoading(Consumer<L> consumer) {
        consumer.accept(myInnerLayout);
        setLoading(false);
    }

    @Override
    @RequiredUIAccess
    public void setLoadingText(LocalizeValue loadingText) {
        myLoadingText = loadingText;

        if (myLoadingLabel != null) {
            myLoadingLabel.setText(loadingText.get());
        }
    }

    @Override
    @RequiredUIAccess
    public void remove(Component component) {
        myInnerLayout.remove(component);
    }

    @Override
    @RequiredUIAccess
    public void removeAll() {
        myInnerLayout.removeAll();
    }

    private void setLoading(boolean loading) {
        myLoading = loading;

        updateCurrentPage();
    }

    private void updateCurrentPage() {
        if (!isAlive(myComponent)) {
            return;
        }

        QWidget page = myLoading ? myLoadingPanel : ((QtComponentDelegate<?>) myInnerLayout).toQtComponent();
        if (isAlive(page)) {
            ((QStackedWidget) myComponent).setCurrentWidget(page);
        }
    }
}
