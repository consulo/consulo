// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list;

import com.intellij.openapi.application.Dispatchers;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.scroll.BoundedRangeModelThresholdListener;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.Dispatchers_UI_Kt;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public final class ReviewListUtil {
    private ReviewListUtil() {
    }

    public static @Nonnull JScrollPane wrapWithLazyVerticalScroll(
        @Nonnull CoroutineScope cs,
        @Nonnull JList<?> list,
        @Nonnull Runnable requestor
    ) {
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(list, true);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        BoundedRangeModel model = scrollPane.getVerticalScrollBar().getModel();
        BoundedRangeModelThresholdListener listener = new BoundedRangeModelThresholdListener(model, 0.7f) {
            @Override
            protected void onThresholdReached() {
                requestor.run();
            }
        };
        model.addChangeListener(listener);

        list.getModel().addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                kotlinx.coroutines.BuildersKt.launch(cs, Dispatchers.getUI(), kotlinx.coroutines.CoroutineStart.DEFAULT,
                    (scope, continuation) -> {
                        kotlinx.coroutines.YieldKt.yield(continuation);
                        checkScroll();
                        return kotlin.Unit.INSTANCE;
                    }
                );
            }

            private void checkScroll() {
                if (list.isShowing()) {
                    listener.stateChanged(new ChangeEvent(list));
                }
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
            }
        });

        return scrollPane;
    }
}
