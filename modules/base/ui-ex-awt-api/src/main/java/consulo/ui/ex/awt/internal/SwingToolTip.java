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
package consulo.ui.ex.awt.internal;

import consulo.application.util.HtmlBuilder;
import consulo.application.util.HtmlChunk;
import consulo.localize.LocalizeValue;
import consulo.ui.ToolTip;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.internal.ToolTipImpl;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public final class SwingToolTip {
    public static final Color SHORTCUT_COLOR = JBColor.namedColor("ToolTip.shortcutForeground", new JBColor(0x787878, 0x999999));
    private static final Color INFO_COLOR = JBColor.namedColor("ToolTip.infoForeground", UIUtil.getContextHelpForeground());

    private static final String PROPERTY = "consulo.swingToolTip";

    private final ToolTipImpl myToolTip;
    private final PropertyChangeListener myUIListener;
    private @Nullable String myText;

    private SwingToolTip(JComponent component, ToolTipImpl toolTip) {
        myToolTip = toolTip;
        myUIListener = event -> refresh(component);
    }

    public static void install(JComponent component, @Nullable ToolTip toolTip) {
        uninstall(component);

        ToolTipImpl impl = (ToolTipImpl) toolTip;
        if (impl == null || impl.isEmpty()) {
            return;
        }

        SwingToolTip swingToolTip = new SwingToolTip(component, impl);
        component.putClientProperty(PROPERTY, swingToolTip);
        component.addPropertyChangeListener("UI", swingToolTip.myUIListener);

        swingToolTip.myText = swingToolTip.buildText();
        component.setToolTipText(swingToolTip.myText);
    }

    public static void uninstall(JComponent component) {
        if (!(component.getClientProperty(PROPERTY) instanceof SwingToolTip swingToolTip)) {
            return;
        }

        component.putClientProperty(PROPERTY, null);
        component.removePropertyChangeListener("UI", swingToolTip.myUIListener);

        if (Objects.equals(swingToolTip.myText, component.getToolTipText())) {
            component.setToolTipText(null);
        }
    }

    private void refresh(JComponent component) {
        if (!Objects.equals(myText, component.getToolTipText())) {
            return;
        }

        myText = buildText();
        component.setToolTipText(myText);
    }

    private @Nullable String buildText() {
        LocalizeValue title = myToolTip.getTitle();
        LocalizeValue shortcut = myToolTip.getShortcut();
        LocalizeValue description = myToolTip.getDescription();

        boolean hasDescription = description.isNotEmpty() && !title.equals(description);
        if (shortcut.isEmpty() && !hasDescription) {
            return title.getNullIfEmpty();
        }

        HtmlBuilder builder = new HtmlBuilder();
        if (title.isNotEmpty()) {
            builder.appendRaw(title);
        }

        if (shortcut.isNotEmpty()) {
            if (!builder.isEmpty()) {
                builder.nbsp();
            }
            builder.append(HtmlChunk.span("color:" + ColorUtil.toHtmlColor(SHORTCUT_COLOR)).addText(shortcut));
        }

        if (hasDescription) {
            if (builder.isEmpty()) {
                builder.appendRaw(description);
            }
            else {
                builder.append(
                    HtmlChunk.div("margin-top:" + JBUI.scale(4) + "px; color:" + ColorUtil.toHtmlColor(INFO_COLOR)).addRaw(description)
                );
            }
        }

        return builder.wrapWithHtmlBody().toString();
    }
}
