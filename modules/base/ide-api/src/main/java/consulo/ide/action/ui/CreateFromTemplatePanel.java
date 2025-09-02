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
package consulo.ide.action.ui;

import consulo.annotation.DeprecationInfo;
import consulo.document.util.TextRange;
import consulo.fileTemplate.AttributesDefaults;
import consulo.fileTemplate.FileTemplate;
import consulo.ide.localize.IdeLocalize;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.internal.DialogWrapperPeer;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * @author MYakovlev
 */
public class CreateFromTemplatePanel {
    private JPanel myMainPanel;
    private JPanel myAttrPanel;
    private JTextField myFilenameField;
    private final String[] myUnsetAttributes;
    private final ArrayList<Pair<String, JTextField>> myAttributes = new ArrayList<>();

    private int myLastRow = 0;

    private int myHorizontalMargin = -1;
    private int myVerticalMargin = -1;
    private final boolean myMustEnterName;
    private final AttributesDefaults myAttributesDefaults;

    public CreateFromTemplatePanel(String[] unsetAttributes, boolean mustEnterName, @Nullable AttributesDefaults attributesDefaults) {
        myMustEnterName = mustEnterName;
        myUnsetAttributes = unsetAttributes;
        myAttributesDefaults = attributesDefaults;
    }

    public boolean hasSomethingToAsk() {
        return myMustEnterName || myUnsetAttributes.length != 0;
    }

    public JComponent getComponent() {
        if (myMainPanel == null) {
            myMainPanel = new JPanel(new GridBagLayout()) {
                @Override
                public Dimension getPreferredSize() {
                    return getMainPanelPreferredSize(super.getPreferredSize());
                }
            };
            myAttrPanel = new JPanel(new GridBagLayout());
            JPanel myScrollPanel = new JPanel(new GridBagLayout());
            updateShown();

            myScrollPanel.setBorder(null);
            int attrCount = myUnsetAttributes.length;
            if (myMustEnterName && !Arrays.asList(myUnsetAttributes).contains(FileTemplate.ATTRIBUTE_NAME)) {
                attrCount++;
            }
            Insets insets = (attrCount > 1) ? new Insets(2, 2, 2, 2) : new Insets(0, 0, 0, 0);
            myScrollPanel.add(
                myAttrPanel,
                new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0)
            );
            if (attrCount > 1) {
                myScrollPanel.add(
                    new JPanel(),
                    new GridBagConstraints(
                        0,
                        1,
                        1,
                        1,
                        0.0,
                        1.0,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(2, 2, 2, 2),
                        0,
                        0
                    )
                );
                JScrollPane attrScroll = ScrollPaneFactory.createScrollPane(myScrollPanel);
                attrScroll.setViewportBorder(null);
                myMainPanel.add(
                    attrScroll,
                    new GridBagConstraints(
                        0,
                        0,
                        1,
                        1,
                        1.0,
                        1.0,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(2, 2, 2, 2),
                        0,
                        0
                    )
                );
            }
            else {
                myMainPanel.add(
                    myScrollPanel,
                    new GridBagConstraints(
                        0,
                        0,
                        1,
                        1,
                        1.0,
                        1.0,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0),
                        0,
                        0
                    )
                );
            }
        }
        return myMainPanel;
    }

    public void ensureFitToScreen(int horizontalMargin, int verticalMargin) {
        myHorizontalMargin = horizontalMargin;
        myVerticalMargin = verticalMargin;
    }

    private Dimension getMainPanelPreferredSize(Dimension superPreferredSize) {
        if ((myHorizontalMargin > 0) && (myVerticalMargin > 0)) {
            Dimension screenSize = ScreenUtil.getMainScreenBounds().getSize();
            Dimension preferredSize = superPreferredSize;
            Dimension maxSize = new Dimension(screenSize.width - myHorizontalMargin, screenSize.height - myVerticalMargin);
            int width = Math.min(preferredSize.width, maxSize.width);
            int height = Math.min(preferredSize.height, maxSize.height);
            if (height < preferredSize.height) {
                width = Math.min(width + 50, maxSize.width); // to disable horizontal scroller
            }
            preferredSize = new Dimension(width, height);
            return preferredSize;
        }
        else {
            return superPreferredSize;
        }
    }

    private void updateShown() {
        Insets insets = new Insets(2, 4, 4, 2);
        if (myMustEnterName || Arrays.asList(myUnsetAttributes).contains(FileTemplate.ATTRIBUTE_NAME)) {
            JLabel filenameLabel = new JLabel(IdeLocalize.labelFileName().get());
            myAttrPanel.add(
                filenameLabel,
                new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0)
            );
            myFilenameField = new JTextField(20);

            // if default settings specified
            if (myAttributesDefaults != null) {
                String fileName = myAttributesDefaults.getDefaultFileName();
                // if default file name specified
                if (fileName != null) {
                    // set predefined file name value
                    myFilenameField.setText(fileName);
                    TextRange selectionRange;
                    // select range from default attributes or select file name without extension
                    if (myAttributesDefaults.getDefaultFileNameSelection() != null) {
                        selectionRange = myAttributesDefaults.getDefaultFileNameSelection();
                    }
                    else {
                        int dot = fileName.indexOf('.');
                        if (dot > 0) {
                            selectionRange = new TextRange(0, dot);
                        }
                        else {
                            selectionRange = null;
                        }
                    }
                    // set selection in editor
                    if (selectionRange != null) {
                        setPredefinedSelectionFor(myFilenameField, selectionRange);
                    }
                }
            }
            myAttrPanel.add(
                myFilenameField,
                new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0)
            );
        }

        myLastRow = 2;
        for (String attribute : myUnsetAttributes) {
            if (attribute.equals(FileTemplate.ATTRIBUTE_NAME)) { // already asked above
                continue;
            }
            JLabel label = new JLabel(attribute.replace('_', ' ') + ":");
            JTextField field = new JTextField();
            field.setColumns(30);
            if (myAttributesDefaults != null) {
                String defaultValue = myAttributesDefaults.getDefaultValueFor(attribute);
                TextRange selectionRange = myAttributesDefaults.getRangeFor(attribute);
                if (defaultValue != null) {
                    field.setText(defaultValue);
                    // set default selection
                    if (selectionRange != null) {
                        setPredefinedSelectionFor(field, selectionRange);
                    }
                }
            }
            myAttributes.add(Pair.create(attribute, field));
            myAttrPanel.add(
                label,
                new GridBagConstraints(0, myLastRow, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0)
            );
            myAttrPanel.add(
                field,
                new GridBagConstraints(1, myLastRow, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0)
            );
            myLastRow++;
        }

        myAttrPanel.repaint();
        myAttrPanel.revalidate();
        myMainPanel.revalidate();
    }

    @Nullable
    public String getFileName() {
        if (myFilenameField != null) {
            String fileName = myFilenameField.getText();
            return fileName == null ? "" : fileName;
        }
        else {
            return null;
        }
    }

    @Nonnull
    public Map<String, Object> getVariables(Map<String, Object> predefinedProperties) {
        Map<String, Object> result = new HashMap<>(predefinedProperties);
        for (Pair<String, JTextField> pair : myAttributes) {
            result.put(pair.first, pair.second.getText());
        }
        return result;
    }

    @Deprecated
    @DeprecationInfo("#getVariables")
    public Properties getProperties(Properties predefinedProperties) {
        Properties result = (Properties) predefinedProperties.clone();
        for (Pair<String, JTextField> pair : myAttributes) {
            result.setProperty(pair.first, pair.second.getText());
        }
        return result;
    }

    private static void setPredefinedSelectionFor(JTextField field, TextRange selectionRange) {
        field.select(selectionRange.getStartOffset(), selectionRange.getEndOffset());
        field.putClientProperty(DialogWrapperPeer.HAVE_INITIAL_SELECTION, true);
    }
}

