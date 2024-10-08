/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.refactoring.ui;

import consulo.ide.impl.idea.util.ui.UpDownHandler;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Label;
import consulo.ui.ex.awt.ListCellRendererWrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Konstantin Bulenkov
 */
public class ComboBoxVisibilityPanel<V> extends VisibilityPanelBase<V> {
    private final Label myLabel;
    protected final JComboBox<V> myComboBox;
    private final Map<V, String> myNamesMap = new HashMap<>();

    public ComboBoxVisibilityPanel(LocalizeValue label, V[] options, String[] presentableNames) {
        setLayout(new BorderLayout(0, 2));
        myLabel = Label.create(label);
        add(TargetAWT.to(myLabel), BorderLayout.NORTH);
        myComboBox = new JComboBox<>(options);
        myComboBox.setRenderer(getRenderer());
        add(myComboBox, BorderLayout.SOUTH);
        for (int i = 0; i < options.length; i++) {
            myNamesMap.put(options[i], presentableNames[i]);
        }
        myComboBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(ComboBoxVisibilityPanel.this));
            }
        });

        myLabel.setTarget(TargetAWT.wrap(myComboBox));
        myLabel.addFocusListener(event -> myComboBox.showPopup());
    }

    protected ListCellRendererWrapper getRenderer() {
        return new ListCellRendererWrapper() {
            @Override
            public void customize(JList list, Object value, int index, boolean selected, boolean hasFocus) {
                setText(myNamesMap.get((V) value));
            }
        };
    }

    public ComboBoxVisibilityPanel(LocalizeValue name, V[] options) {
        this(name, options, getObjectNames(options));
    }

    private static String[] getObjectNames(Object[] options) {
        String[] names = new String[options.length];

        for (int i = 0; i < options.length; i++) {
            names[i] = options[i].toString();
        }

        return names;
    }

    public ComboBoxVisibilityPanel(V[] options) {
        this(RefactoringLocalize.visibilityComboTitle(), options);
    }

    public ComboBoxVisibilityPanel(V[] options, String[] presentableNames) {
        this(RefactoringLocalize.visibilityComboTitle(), options, presentableNames);
    }

    protected void addOption(int index, V option, String presentableName, boolean select) {
        myNamesMap.put(option, presentableName);
        myComboBox.insertItemAt(option, index);

        if (select) {
            myComboBox.setSelectedIndex(index);
        }
    }

    protected void addOption(V option) {
        addOption(myComboBox.getItemCount(), option, option.toString(), false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V getVisibility() {
        return (V) myComboBox.getSelectedItem();
    }

    @Override
    public void addListener(ChangeListener listener) {
        myEventDispatcher.addListener(listener);
    }

    public final void registerUpDownActionsFor(JComponent input) {
        UpDownHandler.register(input, myComboBox);
    }

    @Override
    public void setVisibility(V visibility) {
        myComboBox.setSelectedItem(visibility);
        myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(this));
    }
}
