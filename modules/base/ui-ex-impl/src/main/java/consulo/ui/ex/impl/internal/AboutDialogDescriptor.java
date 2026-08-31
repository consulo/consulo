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
package consulo.ui.ex.impl.internal;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.WidthAndHeight;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * What {@link AboutInfoBuilder} collected, beside a close and a copy button.
 *
 * @author VISTALL
 * @since 2026-08-13
 */
public class AboutDialogDescriptor extends DialogDescriptor {
    private class CopyToClipboardAction extends DumbAwareAction {
        private CopyToClipboardAction() {
            super(LocalizeValue.localizeTODO("Copy to clipboard"), LocalizeValue.empty(), null);
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            UIAccess.current().getClipboard().setText(myInfo);
        }
    }

    private final String myInfo;

    public AboutDialogDescriptor() {
        super(LocalizeValue.localizeTODO("About"));

        myInfo = AboutInfoBuilder.build();
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(35, 30);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        HtmlView view = HtmlView.create();
        view.render(new HtmlView.RenderData("<pre>" + StringUtil.escapeXmlEntities(myInfo) + "</pre>"));
        return view;
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        AnAction copyAction = new CopyToClipboardAction();
        AnAction closeAction = createOkAction();

        if (inverseOrder) {
            return new AnAction[]{copyAction, closeAction};
        }
        else {
            return new AnAction[]{closeAction, copyAction};
        }
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(CommonLocalize.buttonClose());
    }
}
