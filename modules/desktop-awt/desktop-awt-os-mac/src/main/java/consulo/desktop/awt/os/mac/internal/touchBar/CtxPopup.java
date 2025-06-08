// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.LaterInvocator;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.ui.ModalityState;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.MnemonicNavigationFilter;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CtxPopup {
    private static final boolean DISABLED = Boolean.getBoolean("touchbar.popups.disable");

    static @Nullable Disposable showPopupItems(@Nonnull JBPopup popup, @Nonnull JComponent popupComponent) {
        if (DISABLED || !(popup instanceof @Nonnull ListPopupImpl listPopup)) {
            return null;
        }

        final TBPanel tb = createScrubberBarFromPopup(listPopup);
        TouchBarsManager.registerAndShow(popupComponent, tb);

        return () -> {
            TouchBarsManager.unregister(popupComponent);
        };
    }

    // creates releaseOnClose touchbar
    private static TBPanel createScrubberBarFromPopup(@Nonnull ListPopupImpl listPopup) {
        final TBPanel result = new TBPanel("popup_scrubber_bar_" + listPopup.hashCode(), new TBPanel.CrossEscInfo(true, false), false);

        final ModalityState ms = LaterInvocator.getCurrentModalityState();

        final TBItemScrubber scrub = result.addScrubber();
        final @Nonnull ListPopupStep<Object> listPopupStep = listPopup.getListStep();
        final @Nonnull List<Object> stepValues = listPopupStep.getValues();
        final List<Integer> disabledItems = new ArrayList<>();
        int currIndex = 0;
        final Map<Object, Integer> obj2index = new HashMap<>();
        for (Object obj : stepValues) {
            final Image ic = listPopupStep.getIconFor(obj);
            String txt = listPopupStep.getTextFor(obj);

            if (listPopupStep.isMnemonicsNavigationEnabled()) {
                final MnemonicNavigationFilter<Object> filter = listPopupStep.getMnemonicNavigationFilter();
                final int pos = filter == null ? -1 : filter.getMnemonicPos(obj);
                if (pos != -1) {
                    txt = txt.substring(0, pos) + txt.substring(pos + 1);
                }
            }

            final Runnable edtAction = () -> {
                if (obj != null) {
                    listPopup.getList().setSelectedValue(obj, false);
                }
                else {
                    listPopup.getList().setSelectedIndex(stepValues.indexOf(null));
                }
                listPopup.handleSelect(true);
            };

            final Runnable action = () -> {
                ApplicationManager.getApplication().invokeLater(edtAction, ms);
            };
            scrub.addItem(ic, txt, action);
            if (!listPopupStep.isSelectable(obj)) {
                disabledItems.add(currIndex);
            }
            obj2index.put(obj, currIndex);
            ++currIndex;
        }

        final ListModel model = listPopup.getList().getModel();
        model.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                final List<Integer> visibleIndices = new ArrayList<>();
                for (int c = 0; c < model.getSize(); ++c) {
                    final Object visibleItem = model.getElementAt(c);
                    final Integer itemId = obj2index.get(visibleItem);
                    if (itemId != null) {
                        visibleIndices.add(itemId);
                    }
                }

                scrub.showItems(visibleIndices, true, true);
            }
        });

        result.selectVisibleItemsToShow();
        scrub.enableItems(disabledItems, false);
        return result;
    }
}
