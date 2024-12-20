/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import consulo.application.ui.wm.IdeFocusManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;

/**
 * Use this editor if you wish your combobox editor to look good on Macs.
 * <p>
 * User: spLeaner
 */
public class FixedComboBoxEditor implements ComboBoxEditor {
    private JTextField myField;
    private Object oldValue;

    public FixedComboBoxEditor() {
        myField = new JBTextField();
        myField.setBorder(JBUI.Borders.empty());
    }

    protected JTextField getField() {
        return myField;
    }

    @Override
    public Component getEditorComponent() {
        return myField;
    }

    @Override
    public void setItem(Object anObject) {
        if (anObject != null) {
            myField.setText(anObject.toString());
            oldValue = anObject;
        }
        else {
            myField.setText("");
        }
    }

    @Override
    public Object getItem() {
        Object newValue = myField.getText();
        if (oldValue != null && !(oldValue instanceof String)) {
            // The original value is not a string. Should return the value in it's
            // original type.
            if (newValue.equals(oldValue.toString())) {
                return oldValue;
            }
            else {
                // Must take the value from the editor and get the value and cast it to the new type.
                Class cls = oldValue.getClass();
                try {
                    Method method = cls.getMethod("valueOf", new Class[]{String.class});
                    newValue = method.invoke(oldValue, new Object[]{myField.getText()});
                }
                catch (Exception ex) {
                    // Fail silently and return the newValue (a String object)
                }
            }
        }
        return newValue;
    }

    @Override
    public void selectAll() {
        myField.selectAll();
        IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myField);
    }

    @Override
    public void addActionListener(ActionListener l) {
    }

    @Override
    public void removeActionListener(ActionListener l) {
    }
}