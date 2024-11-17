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
package consulo.ui.ex.awt;

import consulo.annotation.DeprecationInfo;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;

public class LabeledComponent<Comp extends JComponent> extends JPanel implements PanelWithAnchor {
    private final JBLabel myLabel = new JBLabel();
    private final boolean mySided;
    private Comp myComponent;
    private String myLabelConstraints = BorderLayout.WEST;
    private JComponent myAnchor;

    public LabeledComponent() {
        this(false);
    }

    public LabeledComponent(boolean sided) {
        super(new BorderLayout(UIUtil.DEFAULT_HGAP, 2));
        mySided = sided;
        insertLabel();
    }

    @Nonnull
    public static <Comp extends JComponent> LabeledComponent<Comp> create(@Nonnull Comp component, @Nonnull String text) {
        final LabeledComponent<Comp> labeledComponent = new LabeledComponent<>();
        labeledComponent.setComponent(component);
        labeledComponent.setText(text);
        return labeledComponent;
    }

    @Nonnull
    public static <Comp extends JComponent> LabeledComponent<Comp> sided(@Nonnull Comp component, @Nonnull String text) {
        final LabeledComponent<Comp> labeledComponent = new LabeledComponent<>(true);
        labeledComponent.setComponent(component);
        labeledComponent.setText(text);
        return labeledComponent;
    }

    @Nonnull
    @Deprecated
    @DeprecationInfo("Use #create() - default position is left already")
    public static <Comp extends JComponent> LabeledComponent<Comp> left(@Nonnull Comp component, @Nonnull String text) {
        LabeledComponent<Comp> labeledComponent = create(component, text);
        labeledComponent.setLabelLocation(BorderLayout.WEST);
        return labeledComponent;
    }

    private void insertLabel() {
        remove(myLabel);
        add(myLabel, myLabelConstraints);
        setAnchor(myLabel);
    }

    public void setText(String textWithMnemonic) {
        if (!StringUtil.endsWithChar(textWithMnemonic, ':')) {
            textWithMnemonic += ":";
        }
        TextWithMnemonic withMnemonic = TextWithMnemonic.fromTextWithMnemonic(textWithMnemonic);
        withMnemonic.setToLabel(myLabel);
    }

    public String getText() {
        String text = TextWithMnemonic.fromLabel(myLabel).getTextWithMnemonic();
        if (StringUtil.endsWithChar(text, ':')) {
            return text.substring(0, text.length() - 1);
        }
        return text;
    }

    public void setComponentClass(@NonNls String className) throws ClassNotFoundException, InstantiationException,
        IllegalAccessException {
        if (className != null) {
            Class<Comp> aClass = (Class<Comp>) getClass().getClassLoader().loadClass(className);
            Comp component = aClass.newInstance();
            setComponent(component);
        }
        else {
            setComponent(null);
        }
    }

    public void setComponent(Comp component) {
        if (myComponent != null) {
            remove(myComponent);
        }
        myComponent = component;
        if (myComponent != null) {
            add(myComponent, mySided ? BorderLayout.EAST : BorderLayout.CENTER);
        }

        if (myComponent instanceof ComponentWithBrowseButton && !(myComponent instanceof TextFieldWithBrowseButton)) {
            myLabel.setLabelFor(((ComponentWithBrowseButton) myComponent).getChildComponent());
        }
        else {
            myLabel.setLabelFor(myComponent);
        }
    }

    public String getComponentClass() {
        if (myComponent == null) {
            return null;
        }
        return getComponent().getClass().getName();
    }

    public Comp getComponent() {
        return myComponent;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (myComponent != null) {
            myComponent.setEnabled(enabled);
        }
        myLabel.setEnabled(enabled);
    }

    public LabeledComponent<Comp> setLabelLocation(@NonNls String borderConstrains) {
        String constrains = findBorderConstrains(borderConstrains);
        if (constrains == null || constrains.equals(myLabelConstraints)) {
            return this;
        }
        myLabelConstraints = borderConstrains;
        insertLabel();
        return this;
    }

    public String getLabelLocation() {
        return myLabelConstraints;
    }

    public Insets getLabelInsets() {
        return myLabel.getInsets();
    }

    public void setLabelInsets(Insets insets) {
        if (Comparing.equal(insets, getLabelInsets())) {
            return;
        }
        myLabel.setBorder(IdeBorderFactory.createEmptyBorder(insets));
    }

    private static final String[] LABEL_BORDER_CONSTRAINS = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};

    private static String findBorderConstrains(String str) {
        for (String constrain : LABEL_BORDER_CONSTRAINS) {
            if (constrain.equals(str)) {
                return constrain;
            }
        }
        return null;
    }

    public String getRawText() {
        return myLabel.getText().replace("\u001B", "");
    }

    public JBLabel getLabel() {
        return myLabel;
    }

    @Override
    public JComponent getAnchor() {
        return myAnchor;
    }

    @Override
    public void setAnchor(@Nullable JComponent labelAnchor) {
        myAnchor = labelAnchor;
        myLabel.setAnchor(labelAnchor);
    }

    public static class TextWithMnemonic {
        private final String myText;
        private final int myMnemoniIndex;

        public TextWithMnemonic(String text, int mnemoniIndex) {
            myText = text;
            myMnemoniIndex = mnemoniIndex;
        }

        public void setToLabel(JLabel label) {
            label.setText(myText);
            if (myMnemoniIndex != -1) {
                label.setDisplayedMnemonic(myText.charAt(myMnemoniIndex));
            }
            else {
                label.setDisplayedMnemonic(0);
            }
            label.setDisplayedMnemonicIndex(myMnemoniIndex);
        }

        public String getTextWithMnemonic() {
            if (myMnemoniIndex == -1) {
                return myText;
            }
            return myText.substring(0, myMnemoniIndex) + "&" + myText.substring(myMnemoniIndex);
        }

        public static TextWithMnemonic fromTextWithMnemonic(String textWithMnemonic) {
            int mnemonicIndex = textWithMnemonic.indexOf('&');
            if (mnemonicIndex == -1) {
                return new TextWithMnemonic(textWithMnemonic, -1);
            }
            textWithMnemonic = textWithMnemonic.substring(0, mnemonicIndex) + textWithMnemonic.substring(mnemonicIndex + 1);
            return new TextWithMnemonic(textWithMnemonic, mnemonicIndex);
        }

        public static TextWithMnemonic fromLabel(JLabel label) {
            return new TextWithMnemonic(label.getText(), label.getDisplayedMnemonicIndex());
        }
    }
}
