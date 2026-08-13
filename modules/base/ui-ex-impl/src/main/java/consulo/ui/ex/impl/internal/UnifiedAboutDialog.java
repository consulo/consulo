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

import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.HtmlView;
import consulo.ui.Size2D;
import consulo.ui.StaticPosition;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderStyle;
import consulo.ui.UIAccess;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TableLayout;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * Unified counterpart of the about dialog - what {@link AboutInfoBuilder} collected, beside a column of a close
 * and a copy button.
 *
 * @author VISTALL
 * @since 2026-08-13
 */
public class UnifiedAboutDialog {
    private Window myWindow;

    @RequiredUIAccess
    public UnifiedAboutDialog(@Nullable Window parentWindow) {
        String info = AboutInfoBuilder.build();

        HtmlView view = HtmlView.create();
        view.render(new HtmlView.RenderData("<pre>" + StringUtil.escapeXmlEntities(info) + "</pre>"));

        Button copyButton = Button.create(
            LocalizeValue.localizeTODO("Copy to clipboard"),
            event -> UIAccess.current().getClipboard().setText(info)
        );
        Button closeButton = Button.create(CommonLocalize.buttonClose(), event -> myWindow.close());
        closeButton.addStyle(ButtonStyle.PRIMARY);

        // a cell of its own for each button, filled - the widest of them sets the column, so the buttons are of
        // one size rather than each of the size of its own text
        TableLayout buttons = TableLayout.create(StaticPosition.TOP);
        buttons.add(closeButton, TableLayout.cell(0, 0).fill());
        buttons.add(copyButton, TableLayout.cell(1, 0).fill());
        buttons.addBorders(BorderStyle.EMPTY, null, 8);

        TwoComponentSplitLayout root = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        root.setProportion(72);
        root.setFirstComponent(view);
        root.setSecondComponent(buttons);

        myWindow = Window.create(LocalizeValue.localizeTODO("About").get(), WindowOptions.builder().owner(parentWindow).build());
        myWindow.setSize(new Size2D(700, 600));
        myWindow.setContent(root);
    }

    @RequiredUIAccess
    public void show() {
        myWindow.show();
    }
}
