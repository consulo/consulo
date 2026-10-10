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
package consulo.desktop.awt.codeInsight.parameterInfo;

import consulo.application.Application;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorFontType;
import consulo.language.editor.impl.internal.parameterInfo.ParameterInfoLines;
import consulo.language.editor.impl.internal.parameterInfo.ParameterInfoModel;
import consulo.language.editor.impl.internal.parameterInfo.ParameterInfoRun;
import consulo.language.editor.impl.internal.parameterInfo.ParameterInfoSignature;
import consulo.language.editor.parameterInfo.SignatureStyle;
import consulo.platform.Platform;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.accessibility.AccessibleContextUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.ComponentColors;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class ParameterInfoComponent extends JPanel {
    private static final Color BACKGROUND = TargetAWT.to(ComponentColors.PARAMETER_INFO_BACKGROUND);
    private static final Color FOREGROUND = TargetAWT.to(ComponentColors.PARAMETER_INFO_FOREGROUND);
    private static final Color HIGHLIGHTED_COLOR = TargetAWT.to(ComponentColors.PARAMETER_INFO_CURRENT_PARAMETER_FOREGROUND);
    private static final Color DISABLED_COLOR = TargetAWT.to(ComponentColors.PARAMETER_INFO_DISABLED_FOREGROUND);
    private static final Color CONTEXT_HELP_FOREGROUND = TargetAWT.to(ComponentColors.PARAMETER_INFO_INFO_FOREGROUND);
    static final Color BORDER_COLOR = TargetAWT.to(ComponentColors.PARAMETER_INFO_BORDER);
    private static final Color HIGHLIGHTED_BACKGROUND = TargetAWT.to(ComponentColors.PARAMETER_INFO_CURRENT_OVERLOAD_BACKGROUND);
    private static final Color SEPARATOR_COLOR = TargetAWT.to(ComponentColors.PARAMETER_INFO_LINE_SEPARATOR);
    private static final Border EMPTY_BORDER = JBUI.Borders.empty(2, 10);
    private static final Border BOTTOM_BORDER = new CompoundBorder(JBUI.Borders.customLine(SEPARATOR_COLOR, 0, 0, 1, 0), EMPTY_BORDER);
    private static final int DEFAULT_WIDTH_LIMIT = 500;

    private final Editor myEditor;
    private final Font myNormalFont;
    private final Font myBoldFont;
    private final int myMaxVisibleRows = Registry.intValue("parameter.info.max.visible.rows");

    private final JPanel myMainPanel = new JPanel(new GridBagLayout());
    private final List<JPanel> myRows = new ArrayList<>();
    private final List<String> myRowTexts = new ArrayList<>();
    private JLabel myShortcutLabel;
    private boolean myRequestFocus;

    public ParameterInfoComponent(Editor editor) {
        super(new BorderLayout());
        myEditor = editor;

        boolean editorFont = Registry.is("parameter.info.editor.font");
        myNormalFont = editorFont ? TargetAWT.to(editor.getColorsScheme().getFont(EditorFontType.PLAIN)) : UIUtil.getLabelFont();
        myBoldFont = editorFont ? TargetAWT.to(editor.getColorsScheme().getFont(EditorFontType.BOLD)) : myNormalFont.deriveFont(Font.BOLD);

        setBackground(BACKGROUND);
        myMainPanel.setBackground(BACKGROUND);

        add(ScrollPaneFactory.createScrollPane(myMainPanel, true), BorderLayout.CENTER);
    }

    public void setRequestFocus(boolean requestFocus) {
        myRequestFocus = requestFocus;
        if (requestFocus) {
            AccessibleContextUtil.setName(this, "Parameter Info. Press TAB to navigate through each element. Press ESC to close.");
        }
    }

    public void setModel(ParameterInfoModel model) {
        myMainPanel.removeAll();
        myRows.clear();
        myRowTexts.clear();

        FontMetrics metrics = getFontMetrics(myBoldFont);
        int widthLimit = getWidthLimit();

        JPanel highlightedRow = null;
        List<ParameterInfoSignature> signatures = model.signatures();
        for (int i = 0; i < signatures.size(); i++) {
            ParameterInfoSignature signature = signatures.get(i);

            Color background = signature.highlighted() ? HIGHLIGHTED_BACKGROUND : BACKGROUND;

            JPanel row = new JPanel(new GridBagLayout());
            row.setOpaque(true);
            row.setBackground(background);
            row.setBorder(signature.separatorAfter() ? BOTTOM_BORDER : EMPTY_BORDER);

            List<List<ParameterInfoRun>> lines = ParameterInfoLines.breakLines(signature, metrics::stringWidth, widthLimit);
            StringBuilder rowText = new StringBuilder();
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                SimpleColoredComponent line = new SimpleColoredComponent();
                line.setOpaque(false);
                line.setFont(myNormalFont);
                line.setIpad(JBUI.emptyInsets());
                line.setMyBorder(null);
                line.setFocusable(myRequestFocus);

                for (ParameterInfoRun run : lines.get(lineIndex)) {
                    line.append(run.text(), toAttributes(run));
                    rowText.append(run.text());
                }

                row.add(line, new GridBagConstraints(
                    0, lineIndex, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0
                ));
            }

            myMainPanel.add(row, new GridBagConstraints(
                0, i, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.emptyInsets(), 0, 0
            ));
            myRows.add(row);
            myRowTexts.add(signature.highlighted() ? "[" + rowText + "]" : rowText.toString());

            if (signature.highlighted()) {
                highlightedRow = row;
            }
        }

        setShortcutLabel(model);

        revalidate();
        repaint();

        if (highlightedRow != null) {
            myMainPanel.scrollRectToVisible(new Rectangle());
            myMainPanel.scrollRectToVisible(highlightedRow.getBounds());
        }
    }

    private void setShortcutLabel(ParameterInfoModel model) {
        if (myShortcutLabel != null) {
            remove(myShortcutLabel);
            myShortcutLabel = null;
        }

        if (model.switchHint().isEmpty()) {
            return;
        }

        myShortcutLabel = new JLabel(model.switchHint().get());
        myShortcutLabel.setForeground(CONTEXT_HELP_FOREGROUND);
        Font labelFont = UIUtil.getLabelFont();
        myShortcutLabel.setFont(labelFont.deriveFont(labelFont.getSize2D() - (Platform.current().os().isWindows() ? 1 : 2)));
        myShortcutLabel.setBorder(JBUI.Borders.empty(6, 10, 0, 10));
        add(myShortcutLabel, BorderLayout.SOUTH);
    }

    private int getWidthLimit() {
        if (Application.get().isUnitTestMode() || Application.get().isHeadlessEnvironment()) {
            return Integer.MAX_VALUE;
        }

        JRootPane rootPane = myEditor.getComponent().getRootPane();
        return rootPane == null ? DEFAULT_WIDTH_LIMIT : rootPane.getLayeredPane().getWidth();
    }

    private static SimpleTextAttributes toAttributes(ParameterInfoRun run) {
        int style = SimpleTextAttributes.STYLE_PLAIN;
        Color foreground = FOREGROUND;

        if (run.styles().contains(SignatureStyle.DISABLED)) {
            foreground = DISABLED_COLOR;
        }

        if (run.styles().contains(SignatureStyle.HIGHLIGHT)) {
            style |= SimpleTextAttributes.STYLE_BOLD;
            foreground = HIGHLIGHTED_COLOR;
        }

        if (run.styles().contains(SignatureStyle.STRIKEOUT)) {
            style |= SimpleTextAttributes.STYLE_STRIKEOUT;
        }

        return new SimpleTextAttributes(style, foreground);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        if (myRows.size() <= myMaxVisibleRows) {
            return preferredSize;
        }
        return new Dimension(preferredSize.width + 20, 200);
    }

    @Override
    public String toString() {
        return myRowTexts.stream().collect(Collectors.joining("\n"));
    }
}
