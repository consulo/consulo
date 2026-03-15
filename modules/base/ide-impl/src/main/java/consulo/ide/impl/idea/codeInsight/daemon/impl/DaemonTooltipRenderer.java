// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.daemon.impl.actions.ShowErrorDescriptionAction;
import consulo.ide.impl.idea.codeInsight.hint.LineTooltipRenderer;
import consulo.ide.impl.idea.codeInspection.ui.InspectionNodeInfo;
import consulo.language.editor.inspection.TooltipLinkHandlers;
import consulo.language.editor.localize.DaemonLocalize;
import consulo.ui.ex.Html;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

class DaemonTooltipRenderer extends LineTooltipRenderer {
    
    protected static final String END_MARKER = "<!-- end marker -->";


    DaemonTooltipRenderer(String text, Object[] comparable) {
        super(text, comparable);
    }

    DaemonTooltipRenderer(String text, int width, Object[] comparable) {
        super(text, width, comparable);
    }

    @Override
    protected void onHide(JComponent contentComponent) {
        ShowErrorDescriptionAction.rememberCurrentWidth(contentComponent.getWidth());
    }

    
    @Override
    protected String dressDescription(Editor editor, String tooltipText, boolean expand) {
        if (!expand) {
            return super.dressDescription(editor, tooltipText, false);
        }

        List<String> problems = getProblems(tooltipText);
        StringBuilder text = new StringBuilder();
        for (String problem : problems) {
            String ref = getLinkRef(problem);
            if (ref != null) {
                String description = TooltipLinkHandlers.getDescription(ref, editor);
                if (description != null) {
                    description =
                        InspectionNodeInfo.stripUIRefsFromInspectionDescription(UIUtil.getHtmlBody(new Html(description).setKeepFont(true)));
                    text.append(getHtmlForProblemWithLink(problem))
                        .append(END_MARKER)
                        .append("<p>")
                        .append("<span style=\"color:")
                        .append(ColorUtil.toHex(getDescriptionTitleColor()))
                        .append("\">")
                        .append(TooltipLinkHandlers.getDescriptionTitle(ref, editor))
                        .append(":</span>")
                        .append(description)
                        .append(UIUtil.BORDER_LINE);
                }
            }
            else {
                text.append(UIUtil.getHtmlBody(new Html(problem).setKeepFont(true))).append(UIUtil.BORDER_LINE);
            }
        }
        if (text.length() > 0) { //otherwise do not change anything
            return XmlStringUtil.wrapInHtml(StringUtil.trimEnd(text.toString(), UIUtil.BORDER_LINE));
        }
        return super.dressDescription(editor, tooltipText, true);
    }

    
    protected List<String> getProblems(String tooltipText) {
        return StringUtil.split(UIUtil.getHtmlBody(new Html(tooltipText).setKeepFont(true)), UIUtil.BORDER_LINE);
    }

    
    protected String getHtmlForProblemWithLink(String problem) {
        Html html = new Html(problem).setKeepFont(true);
        return UIUtil.getHtmlBody(html).replace(
            DaemonLocalize.inspectionExtendedDescription().get(),
            DaemonLocalize.inspectionCollapseDescription().get()
        );
    }

    @Nullable
    protected static String getLinkRef(String text) {
        String linkWithRef = "<a href=\"";
        int linkStartIdx = text.indexOf(linkWithRef);
        if (linkStartIdx >= 0) {
            String ref = text.substring(linkStartIdx + linkWithRef.length());
            int quoteIdx = ref.indexOf('"');
            if (quoteIdx > 0) {
                return ref.substring(0, quoteIdx);
            }
        }
        return null;
    }

    
    protected Color getDescriptionTitleColor() {
        return JBColor.namedColor("ToolTip.infoForeground", new JBColor(0x919191, 0x919191));
    }

    
    @Override
    public LineTooltipRenderer createRenderer(@Nullable String text, int width) {
        return new DaemonTooltipRenderer(text, width, getEqualityObjects());
    }
}
