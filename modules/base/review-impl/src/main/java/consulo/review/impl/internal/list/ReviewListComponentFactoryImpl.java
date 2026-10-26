// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.impl.internal.list;

import consulo.annotation.component.ServiceImpl;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.review.util.ReviewUIUtil;
import consulo.review.list.CodeReviewListItemsViewModel;
import consulo.review.list.ReviewListComponentFactory;
import consulo.review.list.error.ErrorStatusPanelFactory;
import consulo.review.list.error.ErrorStatusPresenter;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.ListBox;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.event.details.KeyCode;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.style.ComponentColors;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The review list of a review tool window: the rows, and the loading, empty and error states in their place.
 */
@Singleton
@ServiceImpl
public class ReviewListComponentFactoryImpl implements ReviewListComponentFactory {
    private final ErrorStatusPanelFactory myErrorStatusPanelFactory;

    @Inject
    public ReviewListComponentFactoryImpl(ErrorStatusPanelFactory errorStatusPanelFactory) {
        myErrorStatusPanelFactory = errorStatusPanelFactory;
    }

    @RequiredUIAccess
    @Override
    public <T> Component create(Disposable parentDisposable,
                                LocalizeValue projectName,
                                CodeReviewListItemsViewModel<T> listVm,
                                @RequiredUIAccess Consumer<T> opener,
                                @Nullable ActionGroup popupGroup,
                                BiConsumer<DataSink, @Nullable T> selectedItemProvider) {
        MutableFlatDataModel<T> model = FlatDataModel.of(new ArrayList<>());
        ListBox<T> list = ListBox.create(model);
        list.setRenderer(ReviewListCellRenderer.create(listVm::getPresentation));
        list.addDoubleClickListener(event -> {
            T value = event.getValue();
            if (value != null) {
                opener.accept(value);
            }
        });
        list.addKeyPressedListener(event -> {
            KeyCode keyCode = event.getInputDetails().getKeyCode();
            T value = list.getValue();
            if (value != null && keyCode != null && keyCode.key() == KeyCode.ENTER.key()) {
                opener.accept(value);
            }
        });

        list.putUserData(UiDataProvider.KEY, sink -> selectedItemProvider.accept(sink, list.getValue()));
        if (popupGroup != null) {
            list.addContextMenuListener(event -> {
                ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.POPUP, popupGroup);
                menu.setTargetComponent(list);
                menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
            });
        }

        Component listComponent = ScrollableLayout.create(list);

        DockLayout root = DockLayout.create();
        root.center(listComponent);

        ReviewUIUtil.bind(parentDisposable, listVm.getItems(), result -> {
            List<T> items = result.getOrNull();
            Throwable error = result.exceptionOrNull();
            if (items != null) {
                model.replaceAll(items);
                root.center(items.isEmpty() ? createStatus(ReviewLocalize.reviewListEmptyStateNothing(projectName)) : listComponent);
            }
            else if (error != null) {
                root.center(createError(error, listVm::reload));
            }
            else if (model.getSize() == 0) {
                root.center(createStatus(ReviewLocalize.reviewListEmptyStateLoading()));
            }
        });
        return root;
    }

    @RequiredUIAccess
    private static Component createStatus(LocalizeValue text) {
        Label label = Label.create(text);
        label.setForegroundColor(ComponentColors.DISABLED_TEXT);
        VerticalLayout layout = VerticalLayout.create(Space.NONE, HorizontalAlignment.CENTER);
        layout.add(label);
        layout.paddingBuilder().allSet(Space.X_LARGE).apply();
        return layout;
    }

    @RequiredUIAccess
    private Component createError(Throwable error, @RequiredUIAccess Runnable retry) {
        AnAction retryAction = DumbAwareAction.create(ReviewLocalize.reviewListErrorRetry(), e -> retry.run());
        ErrorStatusPresenter<Throwable> presenter = ErrorStatusPresenter.simple(ReviewLocalize.reviewListErrorTitle(), null, e -> retryAction);
        Component panel = myErrorStatusPanelFactory.create(error, presenter);
        VerticalLayout layout = VerticalLayout.create(Space.NONE, HorizontalAlignment.CENTER);
        layout.add(panel);
        layout.paddingBuilder().allSet(Space.X_LARGE).apply();
        return layout;
    }
}
