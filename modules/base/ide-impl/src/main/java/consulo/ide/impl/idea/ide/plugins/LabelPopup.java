/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.impl.idea.ide.plugins;

import consulo.dataContext.DataManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.awt.ClickListener;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopupFactory;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2020-12-03
 */
public class LabelPopup extends JLabel {
    private final LocalizeValue myPrefix;

    public LabelPopup(LocalizeValue prefix, Function<LabelPopup, ? extends ActionGroup> groupBuilder) {
        myPrefix = prefix;
        setForeground(UIUtil.getLabelDisabledForeground());
        setBorder(JBUI.Borders.empty(1, 1, 1, 5));
        setIcon(TargetAWT.to(PlatformIconGroup.generalArrowdown()));
        setHorizontalTextPosition(SwingConstants.LEADING);

        new ClickListener() {
            @Override
            public boolean onClick(@Nonnull MouseEvent event, int clickCount) {
                LabelPopup component = LabelPopup.this;
                JBPopupFactory.getInstance()
                    .createActionGroupPopup(
                        myPrefix.get(),
                        groupBuilder.apply(component),
                        DataManager.getInstance().getDataContext(component),
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        true
                    )
                    .showUnderneathOf(component);
                return true;
            }
        }.installOn(this);
    }

    public void setPrefixedText(LocalizeValue tagValue) {
        setText(LocalizeValue.join(myPrefix, LocalizeValue.space(), tagValue).get());
    }
}
