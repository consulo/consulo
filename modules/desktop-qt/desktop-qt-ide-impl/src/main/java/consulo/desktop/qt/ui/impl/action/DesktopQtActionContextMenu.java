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
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.Qt;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Popup menu of an action group, shown by qt on right click. Qt analog of
 * {@code consulo.ui.ex.awt.PopupHandler#installPopupHandler}.
 *
 * @author VISTALL
 * @since 2026-08-16
 * @implNote The menu is the object the signal of the widget is answered by, and nothing else holds it: a connection made to
 * a method of a java object does not keep that object alive, so one left to itself is collected and the right
 * click stops opening anything, with the policy still set and the signal still emitted. Handing it to the widget
 * as a child is what makes it live exactly as long as the widget whose menu it is.
 */
public final class DesktopQtActionContextMenu extends QObject {
    private static final Logger LOG = Logger.getInstance(DesktopQtActionContextMenu.class);

    private final QWidget myWidget;
    private final Function<QPoint, ActionGroup> myGroupSupplier;
    private final String myPlace;
    private final Function<QPoint, DataContext> myContextSupplier;
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
            // a component of another frontend cannot be given a qt context menu, and a right click on it would
            // otherwise do nothing at all with nothing said about why
            LOG.warn("Can't install " + place + " on " + component);
            return;
        }

        delegate.whenBound(widget -> installOn(widget, position -> groupSupplier.get(), place, position -> contextSupplier.get()));
    }

    /**
     * For a widget standing for several things at once - a tab bar, where the group belongs to the tab under the
     * pointer rather than to the bar - so the position of the click decides what is shown.
     */
    public static void installOn(
        QWidget widget,
        Function<QPoint, ActionGroup> groupSupplier,
        String place,
        Function<QPoint, DataContext> contextSupplier
    ) {
        new DesktopQtActionContextMenu(widget, groupSupplier, place, contextSupplier);
    }

    private DesktopQtActionContextMenu(
        QWidget widget,
        Function<QPoint, ActionGroup> groupSupplier,
        String place,
        Function<QPoint, DataContext> contextSupplier
    ) {
        super(widget);

        myWidget = widget;
        myGroupSupplier = groupSupplier;
        myPlace = place;
        myContextSupplier = contextSupplier;

        widget.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu);
        widget.customContextMenuRequested.connect(this::showMenu);
    }

    @RequiredUIAccess
    private void showMenu(QPoint position) {
        ActionGroup group = myGroupSupplier.apply(position);
        if (group == null) {
            // a right click that produces nothing is indistinguishable from one that never arrived, and the two
            // are fixed in different places - so say which it was
            LOG.warn("No action group registered for " + myPlace);
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
            .expandAsync(group, myContextSupplier.apply(position), myPlace, myPresentationFactory, uiAccess, indicator, false)
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

                popupMenu(nodes, globalPosition, () -> myContextSupplier.apply(position));
            }, uiAccess);
    }

    @RequiredUIAccess
    private void popupMenu(
        List<UnifiedActionMenuExpander.MenuNode> nodes,
        QPoint globalPosition,
        Supplier<DataContext> contextSupplier
    ) {
        if (myWidget.isDisposed()) {
            return;
        }

        if (nodes.isEmpty()) {
            LOG.warn("Action group of " + myPlace + " expanded to nothing");
            return;
        }

        DesktopQtMenuImpl previousMenu = myMenu;
        if (previousMenu != null) {
            previousMenu.disposeQt();
        }

        DesktopQtMenuImpl menu = new DesktopQtMenuImpl(LocalizeValue.empty());
        myMenu = menu;

        for (UnifiedActionMenuExpander.MenuNode node : nodes) {
            MenuItem item = UnifiedActionMenuExpander.createMenuItem(node, contextSupplier, myPlace, myPresentationFactory);

            menu.add(item);
        }

        menu.popupDetached(myWidget, globalPosition);
    }
}
