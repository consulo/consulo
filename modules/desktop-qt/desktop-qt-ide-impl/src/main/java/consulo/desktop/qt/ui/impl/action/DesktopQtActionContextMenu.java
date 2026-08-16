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
package consulo.desktop.qt.ui.impl.action;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.dataContext.DataContext;
import consulo.desktop.qt.ui.impl.DesktopQtMenuImpl;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.MenuItem;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.impl.internal.action.MenuItemPresentationFactory;
import consulo.ui.ex.impl.internal.action.UnifiedActionMenuExpander;
import io.qt.core.QPoint;
import io.qt.core.Qt;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Popup menu of an action group, shown by qt on right click. Qt analog of
 * {@code consulo.ui.ex.awt.PopupHandler#installPopupHandler}.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public final class DesktopQtActionContextMenu {
    private static final Logger LOG = Logger.getInstance(DesktopQtActionContextMenu.class);

    private final QWidget myWidget;
    private final Supplier<ActionGroup> myGroupSupplier;
    private final String myPlace;
    private final Supplier<DataContext> myContextSupplier;
    private final MenuItemPresentationFactory myPresentationFactory = new MenuItemPresentationFactory();

    private @Nullable DesktopQtMenuImpl myMenu;
    private @Nullable ProgressIndicator myUpdateIndicator;

    public static void install(
        Component component,
        Supplier<ActionGroup> groupSupplier,
        String place,
        Supplier<DataContext> contextSupplier
    ) {
        if (!(component instanceof QtComponentDelegate<?> delegate)) {
            return;
        }

        delegate.whenBound(widget -> new DesktopQtActionContextMenu(widget, groupSupplier, place, contextSupplier));
    }

    private DesktopQtActionContextMenu(
        QWidget widget,
        Supplier<ActionGroup> groupSupplier,
        String place,
        Supplier<DataContext> contextSupplier
    ) {
        myWidget = widget;
        myGroupSupplier = groupSupplier;
        myPlace = place;
        myContextSupplier = contextSupplier;

        widget.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu);
        widget.customContextMenuRequested.connect(this::showMenu);
    }

    @RequiredUIAccess
    private void showMenu(QPoint position) {
        ActionGroup group = myGroupSupplier.get();
        if (group == null) {
            return;
        }

        // the expansion runs off the ui thread and the widget may have moved by the time it comes back
        QPoint globalPosition = myWidget.mapToGlobal(position);

        UIAccess uiAccess = UIAccess.current();

        ProgressIndicator previousIndicator = myUpdateIndicator;
        if (previousIndicator != null) {
            previousIndicator.cancel();
        }

        ProgressIndicator indicator = new EmptyProgressIndicator();
        myUpdateIndicator = indicator;

        UnifiedActionMenuExpander
            .expandAsync(group, myContextSupplier.get(), myPlace, myPresentationFactory, uiAccess, indicator, false)
            .whenCompleteAsync((nodes, throwable) -> {
                if (myUpdateIndicator != indicator) {
                    return;
                }

                myUpdateIndicator = null;

                if (throwable != null) {
                    if (!UnifiedActionMenuExpander.isProcessCanceled(throwable)) {
                        LOG.warn("Failed to expand action group of " + myPlace, throwable);
                    }
                    return;
                }

                popupMenu(nodes, globalPosition);
            }, uiAccess);
    }

    @RequiredUIAccess
    private void popupMenu(List<UnifiedActionMenuExpander.MenuNode> nodes, QPoint globalPosition) {
        if (myWidget.isDisposed()) {
            return;
        }

        DesktopQtMenuImpl previousMenu = myMenu;
        if (previousMenu != null) {
            previousMenu.disposeQt();
        }

        DesktopQtMenuImpl menu = new DesktopQtMenuImpl(LocalizeValue.empty());
        myMenu = menu;

        for (UnifiedActionMenuExpander.MenuNode node : nodes) {
            MenuItem item = UnifiedActionMenuExpander.createMenuItem(node, myContextSupplier, myPlace, myPresentationFactory);

            menu.add(item);
        }

        menu.popupDetached(myWidget, globalPosition);
    }
}
