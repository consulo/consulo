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
package consulo.ide.impl.idea.ui.debugger.extensions;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.ui.debugger.UiDebuggerExtension;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleColoredText;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.Splitter;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

@ExtensionImpl
public class FocusDebugger implements UiDebuggerExtension, PropertyChangeListener, ListSelectionListener {
    private JComponent myComponent;

    private JList<FocusElement> myLog;
    private DefaultListModel myLogModel;
    private JEditorPane myAllocation;

    @Override
    public JComponent getComponent() {
        if (myComponent == null) {
            myComponent = init();
        }

        return myComponent;
    }

    private JComponent init() {
        JPanel result = new JPanel(new BorderLayout());

        myLogModel = new DefaultListModel();
        myLog = new JBList<>(myLogModel);
        myLog.setCellRenderer(new FocusElementRenderer());


        myAllocation = new JEditorPane();
        DefaultCaret caret = new DefaultCaret();
        myAllocation.setCaret(caret);
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        myAllocation.setEditable(false);


        Splitter splitter = new Splitter(true);
        splitter.setFirstComponent(ScrollPaneFactory.createScrollPane(myLog));
        splitter.setSecondComponent(ScrollPaneFactory.createScrollPane(myAllocation));

        myLog.addListSelectionListener(this);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(this);

        result.add(splitter, BorderLayout.CENTER);


        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new ClearAction());

        result.add(ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true).getComponent(), BorderLayout.NORTH);

        return result;
    }

    class ClearAction extends AnAction {
        ClearAction() {
            super("Clear", "", PlatformIconGroup.actionsClose());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            myLogModel.clear();
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (myLog.getSelectedIndex() == -1) {
            myAllocation.setText(null);
        }
        else {
            FocusElement element = (FocusElement) myLog.getSelectedValue();
            StringWriter s = new StringWriter();
            PrintWriter writer = new PrintWriter(s);
            element.getAllocation().printStackTrace(writer);
            myAllocation.setText(s.toString());
        }
    }

    private boolean isInsideDebuggerDialog(Component c) {
        Window debuggerWindow = SwingUtilities.getWindowAncestor(myComponent);
        if (!(debuggerWindow instanceof Dialog)) {
            return false;
        }

        return c == debuggerWindow || SwingUtilities.getWindowAncestor(c) == debuggerWindow;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Object newValue = evt.getNewValue();
        Object oldValue = evt.getOldValue();

        boolean affectsDebugger = false;

        if (newValue instanceof Component && isInsideDebuggerDialog((Component) newValue)) {
            affectsDebugger |= true;
        }

        if (oldValue instanceof Component && isInsideDebuggerDialog((Component) oldValue)) {
            affectsDebugger |= true;
        }


        SimpleColoredText text = new SimpleColoredText();
        text.append(
            evt.getPropertyName(),
            maybeGrayOut(new SimpleTextAttributes(SimpleTextAttributes.STYLE_UNDERLINE, null), affectsDebugger)
        );
        text.append(" newValue=", maybeGrayOut(SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES, affectsDebugger));
        text.append(evt.getNewValue() + "", maybeGrayOut(SimpleTextAttributes.REGULAR_ATTRIBUTES, affectsDebugger));
        text.append(" oldValue=" + evt.getOldValue(), maybeGrayOut(SimpleTextAttributes.REGULAR_ATTRIBUTES, affectsDebugger));

        myLogModel.addElement(new FocusElement(text, new Throwable()));
        SwingUtilities.invokeLater(() -> {
            if (myLog != null && myLog.isShowing()) {
                int h = myLog.getFixedCellHeight();
                myLog.scrollRectToVisible(new Rectangle(0, myLog.getPreferredSize().height - h, myLog.getWidth(), h));
                if (myLog.getModel().getSize() > 0) {
                    myLog.setSelectedIndex(myLog.getModel().getSize() - 1);
                }
            }
        });
    }

    private SimpleTextAttributes maybeGrayOut(SimpleTextAttributes attr, boolean greyOut) {
        return greyOut ? attr.derive(attr.getStyle(), Color.gray, attr.getBgColor(), attr.getWaveColor()) : attr;
    }

    static class FocusElementRenderer extends ColoredListCellRenderer<FocusElement> {
        @Override
        protected void customizeCellRenderer(@Nonnull JList list, FocusElement value, int index, boolean selected, boolean hasFocus) {
            clear();
            SimpleColoredText text = value.getText();
            List<String> strings = text.getTexts();
            List<SimpleTextAttributes> attributes = value.getText().getAttributes();
            for (int i = 0; i < strings.size(); i++) {
                append(strings.get(i), attributes.get(i));
            }
        }
    }

    static class FocusElement {
        private final SimpleColoredText myText;
        private final Throwable myAllocation;

        FocusElement(SimpleColoredText text, Throwable allocation) {
            myText = text;
            myAllocation = allocation;
        }

        public SimpleColoredText getText() {
            return myText;
        }

        public Throwable getAllocation() {
            return myAllocation;
        }
    }

    @Override
    public String getName() {
        return "Focus";
    }

    @Override
    public void disposeUiResources() {
        myComponent = null;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(this);
    }
}
