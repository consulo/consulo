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
package consulo.desktop.awt.wm.impl.mac;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.dataContext.AsyncDataContext;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.ui.mac.screenmenu.Menu;
import consulo.desktop.awt.ui.mac.screenmenu.MenuItem;
import consulo.application.ui.UISettings;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionUpdater;
import consulo.ui.UIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ShortcutUtil;
import consulo.ui.ex.internal.LocalizeValueWithMnemonic;
import consulo.ui.util.TextWithMnemonic;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Expands an {@link ActionGroup} and populates a native {@link Menu} peer with child {@link MenuItem}/{@link Menu} peers.
 *
 * @author VISTALL
 */
public final class MacScreenMenuFiller {
    /**
     * Icons in the macOS screen menu are only partially supported by the OS, so they are disabled by default.
     * Set {@code -Dconsulo.mac.screenMenu.icons=true} to enable.
     */
    public static final boolean SHOW_ICONS = Boolean.getBoolean("consulo.mac.screenMenu.icons");

    private MacScreenMenuFiller() {
    }

    /**
     * When unicode shortcuts are off, shortcuts are drawn as text by the native custom item view. In that mode every
     * dropdown item is given a custom view so items render with a single, consistent style.
     */
    public static boolean isTextShortcutMode() {
        return !ShortcutUtil.isUseUnicodeShortcuts();
    }

    /**
     * The menu item title. macOS menus never underline mnemonics; when mnemonics are enabled the access key is
     * appended in parentheses (e.g. {@code 文件(F)}) but only if it is not already part of the visible text, so latin
     * titles are not made redundant.
     */
    public static String menuText(Presentation presentation) {
        TextWithMnemonic textWithMnemonic = LocalizeValueWithMnemonic.get(presentation.getTextValue());
        String text = textWithMnemonic.getText();
        if (!UISettings.getInstance().DISABLE_MNEMONICS && textWithMnemonic.hasMnemonic()) {
            char mnemonic = (char)textWithMnemonic.getMnemonic();
            if (text.indexOf(mnemonic) < 0 && text.indexOf(Character.toLowerCase(mnemonic)) < 0) {
                text = text + "(" + mnemonic + ")";
            }
        }
        return text;
    }

    public static CompletableFuture<Void> fill(Menu menuPeer,
                                               ActionGroup group,
                                               DataContext context,
                                               String place,
                                               PresentationFactory presentationFactory,
                                               Component contextComponent) {
        boolean checked = group instanceof CheckedActionGroup;

        AsyncDataContext asyncContext = context instanceof AsyncDataContext
            ? (AsyncDataContext)context
            : DataManager.getInstance().createAsyncDataContext(context);

        UIAccess uiAccess = UIAccess.current();
        ActionUpdater updater =
            new ActionUpdater(ActionManager.getInstance(), presentationFactory, asyncContext, place, true, false, uiAccess);

        return updater.expandActionGroupAsync(group, false, new EmptyProgressIndicator()).thenAcceptAsync(list -> {
            menuPeer.beginFill();

            int added = 0;
            for (AnAction action : list) {
                if (action instanceof AnSeparator) {
                    menuPeer.add(null); // native treats a null child as a separator
                    added++;
                }
                else if (action instanceof ActionGroup subGroup
                    && !Boolean.TRUE.equals(presentationFactory.getPresentation(action).getClientProperty("actionGroup.perform.only"))) {
                    Menu submenu = MacNativeActionMenu.create(context, place, subGroup, presentationFactory, contextComponent);
                    if (isTextShortcutMode()) {
                        // custom item view for consistent styling with the leaf items (draws the submenu arrow itself)
                        submenu.setAcceleratorText("");
                    }
                    menuPeer.add(submenu);
                    added++;
                }
                else {
                    Presentation presentation = presentationFactory.getPresentation(action);
                    menuPeer.add(MacNativeActionMenuItem.create(action, presentation, place, context, checked, contextComponent));
                    added++;
                }
            }

            if (added == 0) {
                MenuItem empty = new MenuItem();
                empty.setLabel("  ");
                empty.setEnabled(false);
                menuPeer.add(empty);
            }

            menuPeer.endFill(true);
        }, uiAccess);
    }
}
