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
package consulo.versionControlSystem.impl.internal.ui.awt;

import consulo.application.AllIcons;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EditorFontType;
import consulo.document.Document;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.FilteringListModel;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// TODO remove in future, replace by popup
@Deprecated
public abstract class TempContentChooser<Data> extends DialogWrapper {
    private List<Data> myAllContents;
    private Editor myViewer;

    private final boolean myUseIdeaEditor;

    private final JList myList;
    private final JBSplitter mySplitter;
    private final Project myProject;
    private final boolean myAllowMultipleSelections;
    private final Alarm myUpdateAlarm;
    private Image myListEntryIcon = AllIcons.FileTypes.Text;

    public TempContentChooser(Project project, String title, boolean useIdeaEditor) {
        this(project, title, useIdeaEditor, false);
    }

    public TempContentChooser(Project project, String title, boolean useIdeaEditor, boolean allowMultipleSelections) {
        super(project, true);
        myProject = project;
        myUseIdeaEditor = useIdeaEditor;
        myAllowMultipleSelections = allowMultipleSelections;
        myUpdateAlarm = new Alarm(getDisposable());
        mySplitter = new JBSplitter(true, 0.3f);
        mySplitter.setSplitterProportionKey(getDimensionServiceKey() + ".splitter");
        myList = new JBList(new CollectionListModel<Item>());

        setOKButtonText(CommonLocalize.buttonOk());
        setTitle(title);

        init();
    }

    public void setContentIcon(@Nullable Image icon) {
        myListEntryIcon = icon;
    }

    public void setSplitterOrientation(boolean vertical) {
        mySplitter.setOrientation(vertical);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myList;
    }

    @Override
    protected JComponent createCenterPanel() {
        int selectionMode = myAllowMultipleSelections ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION;
        myList.setSelectionMode(selectionMode);
        if (myUseIdeaEditor) {
            EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
            myList.setFont(scheme.getFont(EditorFontType.PLAIN));
            Color fg = ObjectUtil.chooseNotNull(TargetAWT.to(scheme.getDefaultForeground()), UIUtil.getListForeground());
            Color bg = ObjectUtil.chooseNotNull(TargetAWT.to(scheme.getDefaultBackground()), UIUtil.getListBackground());
            myList.setForeground(fg);
            myList.setBackground(bg);
        }

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                close(OK_EXIT_CODE);
                return true;
            }
        }.installOn(myList);


        myList.setCellRenderer(new MyListCellRenderer());
        myList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    int newSelectionIndex = -1;
                    for (Object o : myList.getSelectedValues()) {
                        int i = ((Item) o).index;
                        removeContentAt(myAllContents.get(i));
                        if (newSelectionIndex < 0) {
                            newSelectionIndex = i;
                        }
                    }

                    rebuildListContent();
                    if (myAllContents.isEmpty()) {
                        close(CANCEL_EXIT_CODE);
                        return;
                    }
                    newSelectionIndex = Math.min(newSelectionIndex, myAllContents.size() - 1);
                    myList.setSelectedIndex(newSelectionIndex);
                }
                else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    doOKAction();
                }
                else {
                    char aChar = e.getKeyChar();
                    if (aChar >= '0' && aChar <= '9') {
                        int idx = aChar == '0' ? 9 : aChar - '1';
                        if (idx < myAllContents.size()) {
                            myList.setSelectedIndex(idx);
                            e.consume();
                            doOKAction();
                        }
                    }
                }
            }
        });

        mySplitter.setFirstComponent(ListWithFilter.wrap(myList, ScrollPaneFactory.createScrollPane(myList), o -> ((Item) o).longText));
        mySplitter.setSecondComponent(new JPanel());
        rebuildListContent();

        ScrollingUtil.installActions(myList);
        ScrollingUtil.ensureSelectionExists(myList);
        updateViewerForSelection();
        myList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                myUpdateAlarm.cancelAllRequests();
                myUpdateAlarm.addRequest(new Runnable() {
                    @Override
                    public void run() {
                        updateViewerForSelection();
                    }
                }, 100);
            }
        });

        mySplitter.setPreferredSize(new Dimension(500, 500));

        SplitterProportionsData d = new SplitterProportionsDataImpl();
        d.externalizeToDimensionService(getClass().getName());
        d.restoreSplitterProportions(mySplitter);

        return mySplitter;
    }

    protected abstract void removeContentAt(Data content);

    @Override
    protected String getDimensionServiceKey() {
        return getClass().getName(); // store different values for multi-paste, history and commit messages
    }

    @Override
    protected void doOKAction() {
        if (getSelectedIndex() < 0) return;
        super.doOKAction();
    }

    private void updateViewerForSelection() {
        if (myAllContents.isEmpty()) return;
        String fullString = getSelectedText();

        if (myViewer != null) {
            EditorFactory.getInstance().releaseEditor(myViewer);
        }

        if (myUseIdeaEditor) {
            myViewer = createIdeaEditor(fullString);
            JComponent component = myViewer.getComponent();
            component.setPreferredSize(new Dimension(300, 500));
            mySplitter.setSecondComponent(component);
        }
        else {
            JTextArea textArea = new JTextArea(fullString);
            textArea.setRows(3);
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setSelectionStart(0);
            textArea.setSelectionEnd(textArea.getText().length());
            textArea.setEditable(false);
            mySplitter.setSecondComponent(ScrollPaneFactory.createScrollPane(textArea));
        }
        mySplitter.revalidate();
    }

    protected Editor createIdeaEditor(String text) {
        Document doc = EditorFactory.getInstance().createDocument(text);
        Editor editor = EditorFactory.getInstance().createViewer(doc, myProject);
        editor.getSettings().setFoldingOutlineShown(false);
        editor.getSettings().setLineNumbersShown(false);
        editor.getSettings().setLineMarkerAreaShown(false);
        editor.getSettings().setIndentGuidesShown(false);
        return editor;
    }

    @Override
    public void dispose() {
        super.dispose();

        SplitterProportionsData d = new SplitterProportionsDataImpl();
        d.externalizeToDimensionService(getClass().getName());
        d.saveSplitterProportions(mySplitter);

        if (myViewer != null) {
            EditorFactory.getInstance().releaseEditor(myViewer);
            myViewer = null;
        }
    }

    private void rebuildListContent() {
        ArrayList<Item> items = new ArrayList<Item>();
        int i = 0;
        List<Data> contents = new ArrayList<Data>(getContents());
        for (Data content : contents) {
            String fullString = getStringRepresentationFor(content);
            if (fullString != null) {
                String shortString;
                fullString = StringUtil.convertLineSeparators(fullString);
                int newLineIdx = fullString.indexOf('\n');
                if (newLineIdx == -1) {
                    shortString = fullString.trim();
                }
                else {
                    int lastLooked = 0;
                    do {
                        int nextLineIdx = fullString.indexOf("\n", lastLooked);
                        if (nextLineIdx > lastLooked) {
                            shortString = fullString.substring(lastLooked, nextLineIdx).trim() + " ...";
                            break;
                        }
                        else if (nextLineIdx == -1) {
                            shortString = " ...";
                            break;
                        }
                        lastLooked = nextLineIdx + 1;
                    }
                    while (true);
                }
                items.add(new Item(i++, shortString, fullString));
            }
        }
        myAllContents = contents;
        FilteringListModel listModel = (FilteringListModel) myList.getModel();
        ((CollectionListModel) listModel.getOriginalModel()).removeAll();
        listModel.addAll(items);
        ListWithFilter listWithFilter = UIUtil.getParentOfType(ListWithFilter.class, myList);
        if (listWithFilter != null) {
            listWithFilter.getSpeedSearch().update();
            if (listModel.getSize() == 0) listWithFilter.resetFilter();
        }
    }

    protected abstract String getStringRepresentationFor(Data content);

    protected abstract List<Data> getContents();

    public int getSelectedIndex() {
        Object o = myList.getSelectedValue();
        return o == null ? -1 : ((Item) o).index;
    }

    public void setSelectedIndex(int index) {
        myList.setSelectedIndex(index);
        ScrollingUtil.ensureIndexIsVisible(myList, index, 0);
        updateViewerForSelection();
    }

    @Nonnull
    public int[] getSelectedIndices() {
        Object[] values = myList.getSelectedValues();
        int[] result = new int[values.length];
        for (int i = 0, length = values.length; i < length; i++) {
            result[i] = ((Item) values[i]).index;
        }
        return result;
    }

    public List<Data> getAllContents() {
        return myAllContents;
    }

    @Nonnull
    public String getSelectedText() {
        StringBuilder sb = new StringBuilder();
        for (Object o : myList.getSelectedValues()) {
            String s = ((Item) o).longText;
            sb.append(StringUtil.convertLineSeparators(s));
        }
        return sb.toString();
    }

    private class MyListCellRenderer extends ColoredListCellRenderer {
        @Override
        protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
            setIcon(myListEntryIcon);
            if (myUseIdeaEditor) {
                int max = list.getModel().getSize();
                String indexString = String.valueOf(index + 1);
                int count = String.valueOf(max).length() - indexString.length();
                char[] spaces = new char[count];
                Arrays.fill(spaces, ' ');
                String prefix = indexString + new String(spaces) + "  ";
                append(prefix, SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
            else if (UIUtil.isUnderGTKLookAndFeel()) {
                // Fix GTK background
                Color background = selected ? UIUtil.getListSelectionBackground() : UIUtil.getListBackground();
                UIUtil.changeBackGround(this, background);
            }
            String text = ((Item) value).shortText;

            FontMetrics metrics = list.getFontMetrics(list.getFont());
            int charWidth = metrics.charWidth('m');
            int maxLength = list.getParent().getParent().getWidth() * 3 / charWidth / 2;
            text = StringUtil.first(text, maxLength, true); // do not paint long strings
            append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }

    private static class Item {
        final int index;
        final String shortText;
        final String longText;

        private Item(int index, String shortText, String longText) {
            this.index = index;
            this.shortText = shortText;
            this.longText = longText;
        }
    }
}
