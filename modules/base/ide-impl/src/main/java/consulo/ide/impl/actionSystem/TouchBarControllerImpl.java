/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.actionSystem;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.disposer.Disposable;
import consulo.ui.UIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.internal.TouchBarControllerInternal;
import consulo.ui.ex.internal.TouchBarFacade;
import consulo.ui.ex.popup.JBPopup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 21.06.2025.
 */
@ServiceImpl
@Singleton
public class TouchBarControllerImpl implements TouchBarControllerInternal {
    private TouchBarFacade myFacade;

    private final Application myApplication;

    @Inject
    public TouchBarControllerImpl(Application application) {
        myApplication = application;
    }

    @Override
    public void tryToInitialize() {
        UIAccess.assetIsNotUIThread();

        ExtensionPoint<TouchBarFacade> point = myApplication.getExtensionPoint(TouchBarFacade.class);

        point.forEachBreakable(touchBarFacade -> {
            if (touchBarFacade.isAvailable()) {
                touchBarFacade.initialize();

                if (touchBarFacade.isEnabled()) {
                    myFacade = touchBarFacade;
                    return ExtensionPoint.Flow.BREAK;
                }
            }

            return ExtensionPoint.Flow.CONTINUE;
        });
    }

    @Override
    public void showPopupItems(@Nonnull JBPopup popup, @Nonnull JComponent popupComponent) {
        if (myFacade == null) {
            return;
        }

        myFacade.showPopupItems(popup, popupComponent);
    }

    @Override
    public void onUpdateEditorHeader(@Nonnull Object editor, JComponent header) {
        if (myFacade == null) {
            return;
        }

        myFacade.onUpdateEditorHeader(editor, header);
    }

    @Override
    public boolean isEnabled() {
        return myFacade != null && myFacade.isEnabled();
    }

    @Override
    public void setButtonActions(@Nonnull JComponent component, Collection<? extends JButton> buttons, Collection<? extends JButton> principal, JButton defaultButton, @Nullable ActionGroup extraActions) {
        if (myFacade == null) {
            return;
        }

        myFacade.setButtonActions(component, buttons, principal, defaultButton, extraActions);
    }

    @Nullable
    @Override
    public Disposable showWindowActions(@Nonnull Component contentPane) {
        return myFacade == null ? null : myFacade.showWindowActions(contentPane);
    }

    @Override
    public void setActions(@Nonnull JComponent component, @Nullable ActionGroup group) {
        if (myFacade == null) {
            return;
        }

        myFacade.setActions(component, group);
    }
}
