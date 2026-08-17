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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.progressbar.ProgressBar;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LoadingLayout;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The awt panel draws its spinner over the content it covers. Here the two are stacked instead - the content
 * stays in the dom while it is hidden, so the browser keeps whatever it already laid out and the frame does not
 * have to be built a second time when the loading ends.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public class WebLoadingLayoutImpl<L extends Layout> extends VaadinComponentDelegate<WebLoadingLayoutImpl.Vaadin>
    implements LoadingLayout<L> {

    public class Vaadin extends Div implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebLoadingLayoutImpl.this;
        }
    }

    private final L myInnerLayout;

    private final Div myLoadingPanel = new Div();
    private final com.vaadin.flow.component.html.Span myLoadingLabel = new com.vaadin.flow.component.html.Span();

    public WebLoadingLayoutImpl(L inner, Disposable parent) {
        myInnerLayout = inner;

        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);

        myLoadingPanel.addClassName("web-loading-layout-panel");
        myLoadingPanel.add(progressBar, myLoadingLabel);
        myLoadingPanel.setVisible(false);

        com.vaadin.flow.component.Component innerComponent = TargetVaadin.to(inner);
        ((HasSize) innerComponent).setSizeFull();

        Vaadin vaadin = getVaadinComponent();
        vaadin.setSizeFull();
        vaadin.add(myLoadingPanel, innerComponent);
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
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
        myLoadingLabel.setText(loadingText.get());
    }

    private void setLoading(boolean loading) {
        myLoadingPanel.setVisible(loading);
        TargetVaadin.to(myInnerLayout).setVisible(!loading);
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
}
