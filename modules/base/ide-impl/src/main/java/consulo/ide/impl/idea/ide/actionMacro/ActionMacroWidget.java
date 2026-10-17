/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actionMacro;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.layout.HorizontalLayout;
import org.jspecify.annotations.Nullable;

class ActionMacroWidget implements StatusBarWidget, StatusBarWidget.IconPresentation {
    private static final int TYPING_SAMPLE_LENGTH = 20;
    private static final String RECORDED = "Recorded: ";

    private final Image myIcon = ImageEffects.blinking(PlatformIconGroup.ideMacroRecording_1());

    private @Nullable StatusBar myStatusBar;
    private @Nullable LightPopup myPopup;
    private @Nullable Label myText;
    private LocalizeValue myTextValue = LocalizeValue.localizeTODO("Macro recording started…");
    private String myLastTyping = "";

    @Override
    public String getId() {
        return "MacroRecording";
    }

    @Override
    public WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public Image getIcon() {
        return myIcon;
    }

    @Override
    public LocalizeValue getTooltipText() {
        return LocalizeValue.localizeTODO("Macro is being recorded now");
    }

    @Override
    public ComponentEventListener<Component, ClickEvent> getClickListener() {
        return event -> togglePopup(event.getComponent());
    }

    @Override
    @RequiredUIAccess
    public void install(StatusBar statusBar) {
        myStatusBar = statusBar;

        UIAccess.current().give(() -> {
            Component anchor = statusBar instanceof StatusBarEx statusBarEx ? statusBarEx.getWidgetComponent(getId()) : null;
            if (anchor != null && myStatusBar != null) {
                togglePopup(anchor);
            }
        });
    }

    @RequiredUIAccess
    private void togglePopup(Component anchor) {
        LightPopup shown = myPopup;
        if (shown != null) {
            shown.close();
            return;
        }

        ActionGroup group = ActionGroup.newImmutableBuilder()
            .add(ActionManager.getInstance().getAction("StartStopMacroRecording"))
            .build();

        HorizontalLayout layout = HorizontalLayout.create();

        ActionToolbar toolbar =
            ActionToolbarFactory.getInstance().createActionToolbar(ActionPlaces.STATUS_BAR_PLACE, group, ActionToolbar.Style.INPLACE);
        toolbar.setTargetUIComponent(layout);

        Label text = Label.create(myTextValue);

        layout.add(toolbar.getUIComponent());
        layout.add(text);

        LightPopup popup = LightPopup.create(
            PopupOptions.builder()
                .position(PopupPosition.TOP)
                .disableCancelOnClickOutside()
                .disableRequestFocus()
                .build()
        );
        popup.setContent(layout);
        popup.addCloseListener(event -> {
            if (myPopup == popup) {
                myPopup = null;
                myText = null;
            }
        });

        myPopup = popup;
        myText = text;

        popup.showBy(anchor);
    }

    @Override
    public void dispose() {
        myStatusBar = null;

        LightPopup popup = myPopup;
        myPopup = null;
        myText = null;
        if (popup != null) {
            popup.close();
        }
    }

    @RequiredUIAccess
    public void notifyUser(String text, boolean typing) {
        String actualText = text;
        if (typing) {
            myLastTyping += text;
            if (myLastTyping.length() > TYPING_SAMPLE_LENGTH) {
                myLastTyping = "..." + myLastTyping.substring(myLastTyping.length() - TYPING_SAMPLE_LENGTH);
            }
            actualText = myLastTyping;
        }
        else {
            myLastTyping = "";
        }

        myTextValue = LocalizeValue.of(RECORDED + actualText);

        Label label = myText;
        if (label != null) {
            label.setText(myTextValue);
        }
    }
}
