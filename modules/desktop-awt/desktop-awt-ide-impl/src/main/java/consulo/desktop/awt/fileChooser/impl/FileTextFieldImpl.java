// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.fileChooser.impl;

import consulo.application.Application;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileTextField;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.ListActions;
import consulo.ide.impl.idea.util.ui.update.LazyUiDisposable;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.ui.ex.awt.popup.GroupedItemsListRenderer;
import consulo.ui.ex.awt.popup.ListItemDescriptorAdapter;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

import static consulo.application.impl.internal.IdeaModalityState.stateForComponent;
import static consulo.desktop.awt.fileChooser.impl.FileTextFieldUtil.createMatcher;
import static consulo.ui.ex.action.IdeActions.ACTION_CODE_COMPLETION;

public abstract class FileTextFieldImpl implements FileLookup, Disposable, FileTextField {
    private final Object myLock = new Object();
    private final JTextField myPathTextField;

    private CompletionResult myCurrentCompletion;

    private JBPopup myCurrentPopup;
    private JBPopup myNoSuggestionsPopup;

    private JList<LookupFile> myList;

    private MergingUpdateQueue myUiUpdater;

    private boolean myPathIsUpdating;
    private Finder myFinder;
    private LookupFilter myFilter;

    private String myFileSpitRegExp;

    protected boolean myAutopopup = false;
    private FileTextFieldImpl.CancelAction myCancelAction;
    private final Set<Action> myDisabledTextActions;
    private Map<String, String> myMacroMap;

    public FileTextFieldImpl(
        final JTextField field,
        Finder finder,
        LookupFilter filter,
        Map<String, String> macroMap,
        final Disposable parent
    ) {
        myPathTextField = field;
        myMacroMap = new TreeMap<>(macroMap);

        InputMap listMap = (InputMap) UIManager.getDefaults().get("List.focusInputMap");
        KeyStroke[] listKeys = listMap.keys();
        myDisabledTextActions = new HashSet<>();
        for (KeyStroke eachListStroke : listKeys) {
            String listActionID = (String) listMap.get(eachListStroke);
            if (ListActions.Down.ID.equals(listActionID) || ListActions.Up.ID.equals(listActionID)) {
                Object textActionID = field.getInputMap().get(eachListStroke);
                if (textActionID != null) {
                    Action textAction = field.getActionMap().get(textActionID);
                    if (textAction != null) {
                        myDisabledTextActions.add(textAction);
                    }
                }
            }
        }


        FileTextFieldImpl assigned = (FileTextFieldImpl) myPathTextField.getClientProperty(KEY);
        if (assigned != null) {
            assigned.myFinder = finder;
            assigned.myFilter = filter;
            return;
        }

        myPathTextField.putClientProperty(KEY, this);
        boolean headless = Application.get().isUnitTestMode();

        myUiUpdater = new MergingUpdateQueue("FileTextField.UiUpdater", 200, false, myPathTextField);
        if (!headless) {
            new UiNotifyConnector(myPathTextField, myUiUpdater);
        }

        myFinder = finder;
        myFilter = filter;

        myFileSpitRegExp = myFinder.getSeparator().replaceAll("\\\\", "\\\\\\\\");

        myPathTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                processTextChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                processTextChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                processTextChanged();
            }
        });

        myPathTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                processListSelection(e);
            }
        });

        myPathTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                closePopup();
            }
        });

        myCancelAction = new CancelAction();


        new LazyUiDisposable<>(parent, field, this) {
            @Override
            protected void initialize(@Nonnull Disposable parent, @Nonnull FileTextFieldImpl child, @Nullable Project project) {
                Disposer.register(child, myUiUpdater);
            }
        };
    }

    @SuppressWarnings("unused") //used by rider
    public void resetMacroMap(Map<String, String> macroMap) {
        synchronized (myLock) {
            myMacroMap = new TreeMap<>(macroMap);
        }
    }

    @Override
    public void dispose() {
        myUiUpdater = null;
    }

    private void processTextChanged() {
        if (myAutopopup && !isPathUpdating()) {
            // Hide current popup as early as we can
            hideCurrentPopup();
            suggestCompletion(false, false);
        }

        onTextChanged(getTextFieldText());
    }

    protected void onTextChanged(String newValue) {
    }

    private void suggestCompletion(final boolean selectReplacedText, final boolean isExplicitCall) {
        if (isExplicitCall) {
            myAutopopup = true;
        }

        if (!getField().isFocusOwner()) {
            return;
        }

        final CompletionResult result = new CompletionResult();
        if (myList != null && myCurrentCompletion != null) {
            int index = myList.getSelectedIndex();
            if (index >= 0 && index < myList.getModel().getSize()) {
                result.myPreselected = myList.getSelectedValue();
            }
        }

        myUiUpdater.queue(new Update("textField.suggestCompletion") {
            @Override
            public void run() {
                String completionBase = getCompletionBase();
                if (completionBase != null) {
                    LookupFile file = myFinder.find(completionBase);
                    if (file != null && file.exists() && !file.isDirectory()) {
                        // we've entered a complete path already, no need to autopopup completion again (IDEA-78996)
                        return;
                    }
                }
                result.myCompletionBase = completionBase;
                if (result.myCompletionBase == null) {
                    return;
                }
                result.myFieldText = myPathTextField.getText();
                EmptyProgressIndicator indicator = new EmptyProgressIndicator(stateForComponent(myPathTextField));
                Application.get().executeOnPooledThread(() -> {
                    ProgressManager.getInstance().runProcess(() -> processCompletion(result), indicator);
                    SwingUtilities.invokeLater(() -> {
                        if (!result.myCompletionBase.equals(getCompletionBase())) {
                            return;
                        }

                        selectCompletionRemoveText(result, selectReplacedText);

                        showCompletionPopup(result, isExplicitCall);
                    });
                });
            }
        });
    }

    private int selectCompletionRemoveText(CompletionResult result, boolean selectReplacedText) {
        int pos = myPathTextField.getCaretPosition();

        if (result.myToComplete.size() > 0 && selectReplacedText) {
            myPathTextField.setCaretPosition(myPathTextField.getText().length());
            myPathTextField.moveCaretPosition(pos);
        }

        return pos;
    }

    @Nullable
    public static String getAdText(CompletionResult result) {
        if (result.myCompletionBase == null) {
            return null;
        }
        if (result.myCompletionBase.length() == result.myFieldText.length()) {
            return null;
        }

        String strokeText =
            KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_REPLACE));
        return IdeLocalize.fileChooserCompletionAdText(strokeText).get();
    }

    public static class CompletionResult {
        public List<LookupFile> myMacros;
        public List<LookupFile> myToComplete;
        public List<LookupFile> mySiblings;
        public List<LookupFile> myKidsAfterSeparator;
        public String myCompletionBase;
        public LookupFile myClosestParent;
        public LookupFile myPreselected;
        public LookupFile current;
        public boolean currentParentMatch;
        public String effectivePrefix;
        public LookupFile currentGrandparent;
        public String grandparentPrefix;
        public boolean closedPath;
        public String myFieldText;
    }

    private void showCompletionPopup(CompletionResult result, boolean isExplicit) {
        if (myList == null) {
            myList = new JBList<>();
            myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            myList.setCellRenderer(new GroupedItemsListRenderer<>(new ListItemDescriptorAdapter<>() {
                @Override
                public String getTextFor(LookupFile file) {
                    return FileTextFieldUtil.getLookupString(file, myFinder, myCurrentCompletion);
                }

                @Override
                public Image getIconFor(LookupFile value) {
                    return value.getIcon();
                }

                private
                @Nullable
                String getSeparatorAboveOf(Object value) {
                    if (myCurrentCompletion == null) {
                        return null;
                    }
                    LookupFile file = (LookupFile) value;

                    int fileIndex = myCurrentCompletion.myToComplete.indexOf(file);
                    if (fileIndex > 0 && !myCurrentCompletion.myMacros.contains(file)) {
                        LookupFile prev = myCurrentCompletion.myToComplete.get(fileIndex - 1);
                        if (myCurrentCompletion.myMacros.contains(prev)) {
                            return "";
                        }
                    }

                    if (myCurrentCompletion.myKidsAfterSeparator.indexOf(file) == 0 && myCurrentCompletion.mySiblings.size() > 0) {
                        LookupFile parent = file.getParent();
                        return parent != null ? parent.getName() : "";
                    }

                    if (myCurrentCompletion.myMacros.size() > 0 && fileIndex == 0) {
                        return getPathVariablesSeparatorText();
                    }

                    return null;
                }

                @Override
                public boolean hasSeparatorAboveOf(LookupFile value) {
                    return getSeparatorAboveOf(value) != null;
                }

                @Override
                public String getCaptionAboveOf(LookupFile value) {
                    return getSeparatorAboveOf(value);
                }
            }));
        }

        if (myCurrentPopup != null) {
            closePopup();
        }

        myCurrentCompletion = result;

        if (myCurrentCompletion.myToComplete.size() == 0) {
            showNoSuggestions(isExplicit);
            return;
        }

        myList.setModel(new AbstractListModel<>() {
            @Override
            public int getSize() {
                return myCurrentCompletion.myToComplete.size();
            }

            @Override
            public LookupFile getElementAt(int index) {
                return myCurrentCompletion.myToComplete.get(index);
            }
        });
        myList.getSelectionModel().clearSelection();
        PopupChooserBuilder<LookupFile> builder = new PopupChooserBuilder<>(myList);
        builder.addListener(new JBPopupListener() {
            @Override
            public void beforeShown(@Nonnull LightweightWindowEvent event) {
                myPathTextField.registerKeyboardAction(
                    myCancelAction,
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW
                );
                for (Action each : myDisabledTextActions) {
                    each.setEnabled(false);
                }
            }

            @Override
            public void onClosed(@Nonnull LightweightWindowEvent event) {
                myPathTextField.unregisterKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
                for (Action each : myDisabledTextActions) {
                    each.setEnabled(true);
                }
            }
        });

        myCurrentPopup = builder.setRequestFocus(false)
            .setAdText(getAdText(myCurrentCompletion))
            .setAutoSelectIfEmpty(false)
            .setResizable(false)
            .setCancelCallback(() -> {
                int caret = myPathTextField.getCaretPosition();
                myPathTextField.setSelectionStart(caret);
                myPathTextField.setSelectionEnd(caret);
                myPathTextField.setFocusTraversalKeysEnabled(true);
                IdeFocusManager.getGlobalInstance()
                    .doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(getField(), true));
                return Boolean.TRUE;
            })
            .setItemChoosenCallback(() -> processChosenFromCompletion(false))
            .setCancelKeyEnabled(false)
            .setAlpha(0.1f)
            .setFocusOwners(new Component[]{myPathTextField}).
            createPopup();


        if (result.myPreselected != null) {
            myList.setSelectedValue(result.myPreselected, false);
        }

        myPathTextField.setFocusTraversalKeysEnabled(false);

        myCurrentPopup.showInScreenCoordinates(getField(), getLocationForCaret(myPathTextField));
    }

    private void processChosenFromCompletion(boolean nameOnly) {
        FileTextFieldUtil.processChosenFromCompletion(
            getSelectedFileFromCompletionPopup(),
            new FileTextFieldUtil.TextFieldDocumentOwner(myPathTextField, this::setTextToFile),
            myFinder,
            nameOnly
        );
    }

    @Nonnull
    protected String getPathVariablesSeparatorText() {
        return IdeLocalize.fileChooserCompletionPathVariablesText().get();
    }

    private void showNoSuggestions(boolean isExplicit) {
        hideCurrentPopup();

        if (!isExplicit) {
            return;
        }

        JComponent message = HintUtil.createErrorLabel(IdeLocalize.fileChooserCompletionNoSuggestions().get());
        ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(message, message);
        builder.setRequestFocus(false).setResizable(false).setAlpha(0.1f).setFocusOwners(new Component[]{myPathTextField});
        myNoSuggestionsPopup = builder.createPopup();
        myNoSuggestionsPopup.showInScreenCoordinates(getField(), getLocationForCaret(myPathTextField));
    }

    private void hideCurrentPopup() {
        if (myCurrentPopup != null) {
            myCurrentPopup.cancel();
            myCurrentPopup = null;
        }

        if (myNoSuggestionsPopup != null) {
            myNoSuggestionsPopup.cancel();
            myNoSuggestionsPopup = null;
        }
    }

    private static Point getLocationForCaret(JTextComponent pathTextField) {
        Point point;

        int position = pathTextField.getCaretPosition();
        try {
            Rectangle rec = pathTextField.modelToView(position);
            point = new Point((int) rec.getMaxX(), (int) rec.getMaxY());
        }
        catch (BadLocationException e) {
            point = pathTextField.getCaret().getMagicCaretPosition();
        }

        SwingUtilities.convertPointToScreen(point, pathTextField);

        point.y += 2;

        return point;
    }

    public void processCompletion(CompletionResult result) {
        Map<String, String> macroMap;
        synchronized (myLock) {
            macroMap = myMacroMap;
        }
        FileTextFieldUtil.processCompletion(result, myFinder, myFilter, myFileSpitRegExp, macroMap);
    }

    static void addMacroPaths(CompletionResult result, String typedText, @Nonnull Finder finder, Map<String, String> macroMap) {
        result.myMacros = new ArrayList<>();

        MinusculeMatcher matcher = createMatcher(typedText);

        for (String eachMacro : macroMap.keySet()) {
            if (matcher.matches(eachMacro)) {
                String eachPath = macroMap.get(eachMacro);
                if (eachPath != null) {
                    LookupFile macroFile = finder.find(eachPath);
                    if (macroFile != null && macroFile.exists()) {
                        result.myMacros.add(macroFile);
                        result.myToComplete.add(macroFile);
                        macroFile.setMacro(eachMacro);
                    }
                }
            }
        }
    }

    @Nullable
    public LookupFile getFile() {
        String text = getTextFieldText();
        if (text == null) {
            return null;
        }
        return myFinder.find(text);
    }

    protected void setTextToFile(LookupFile file) {
        String text = file.getAbsolutePath();
        if (file.isDirectory() && !text.endsWith(myFinder.getSeparator())) {
            text += myFinder.getSeparator();
        }
        myPathTextField.setText(text);
    }

    private void processListSelection(KeyEvent e) {
        if (togglePopup(e)) {
            return;
        }

        if (!isPopupShowing()) {
            return;
        }

        InputMap map = myPathTextField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (map != null) {
            Object object = map.get(KeyStroke.getKeyStrokeForEvent(e));
            if (object instanceof Action action && action.isEnabled()) {
                action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "action"));
                e.consume();
                return;
            }
        }

        Object action = getAction(e, myList);

        if (ListActions.Down.ID.equals(action)) {
            if (ensureSelectionExists()) {
                ScrollingUtil.moveDown(myList, e.getModifiersEx());
                e.consume();
            }
        }
        else if (ListActions.Up.ID.equals(action)) {
            ScrollingUtil.moveUp(myList, e.getModifiersEx());
            e.consume();
        }
        else if (ListActions.PageDown.ID.equals(action)) {
            ScrollingUtil.movePageDown(myList);
            e.consume();
        }
        else if (ListActions.PageUp.ID.equals(action)) {
            ScrollingUtil.movePageUp(myList);
            e.consume();
        }
        else if (getSelectedFileFromCompletionPopup() != null
            && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB)
            && e.getModifiers() == 0) {
            hideCurrentPopup();
            e.consume();
            processChosenFromCompletion(e.getKeyCode() == KeyEvent.VK_TAB);
        }
    }

    private
    @Nullable
    LookupFile getSelectedFileFromCompletionPopup() {
        if (myList == null) {
            return null;
        }
        return myList.getSelectedValue();
    }

    private boolean ensureSelectionExists() {
        if (myList.getSelectedIndex() < 0 || myList.getSelectedIndex() >= myList.getModel().getSize()) {
            if (myList.getModel().getSize() >= 0) {
                myList.setSelectedIndex(0);
                return false;
            }
        }

        return true;
    }

    private boolean togglePopup(KeyEvent e) {
        KeyStroke stroke = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers());
        Object action = ((InputMap) UIManager.get("ComboBox.ancestorInputMap")).get(stroke);
        if ("selectNext".equals(action)) {
            return !isPopupShowing();
        }
        else if ("togglePopup".equals(action)) {
            if (isPopupShowing()) {
                closePopup();
            }
            else {
                suggestCompletion(true, true);
            }
            return true;
        }
        else {
            Keymap active = KeymapManager.getInstance().getActiveKeymap();
            for (String id : active.getActionIds(stroke)) {
                if (ACTION_CODE_COMPLETION.equals(id)) {
                    suggestCompletion(true, true);
                }
            }
        }

        return false;
    }

    private static Object getAction(KeyEvent e, JComponent comp) {
        KeyStroke stroke = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers());
        return comp.getInputMap().get(stroke);
    }


    @Override
    public JTextField getField() {
        return myPathTextField;
    }

    @Override
    public boolean isPopupDisplayed() {
        return myCurrentPopup != null && myCurrentPopup.isVisible();
    }

    public Finder getFinder() {
        return myFinder;
    }

    private boolean isPopupShowing() {
        return myCurrentPopup != null && myList != null && myList.isShowing();
    }

    private void closePopup() {
        hideCurrentPopup();
        myCurrentCompletion = null;
    }

    @Nullable
    public String getTextFieldText() {
        String text = myPathTextField.getText();
        if (text == null) {
            return null;
        }
        return text;
    }

    public final void setText(final String text, boolean now, @Nullable final Runnable onDone) {
        Update update = new Update("pathFromTree") {
            @Override
            public void run() {
                myPathIsUpdating = true;
                getField().setText(text);
                myPathIsUpdating = false;
                if (onDone != null) {
                    onDone.run();
                }
            }
        };
        if (now) {
            update.run();
        }
        else {
            myUiUpdater.queue(update);
        }
    }

    public boolean isPathUpdating() {
        return myPathIsUpdating;
    }

    public
    @Nullable
    String getCompletionBase() {
        String text = getTextFieldText();
        if (text == null) {
            return null;
        }
        int pos = myPathTextField.getCaretPosition();
        return pos < text.length() ? text.substring(0, pos) : text;
    }

    public static class Vfs extends FileTextFieldImpl {
        public Vfs(JTextField field, Map<String, String> macroMap, Disposable parent, LookupFilter chooserFilter) {
            super(field, new LocalFsFinder(), chooserFilter, macroMap, parent);
        }

        public Vfs(Map<String, String> macroMap, Disposable parent, LookupFilter chooserFilter) {
            this(new JTextField(), macroMap, parent, chooserFilter);
        }

        @Override
        public VirtualFile getSelectedFile() {
            LookupFile lookupFile = getFile();
            return lookupFile != null ? ((LocalFsFinder.VfsFile) lookupFile).getFile() : null;
        }
    }

    private class CancelAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (myCurrentPopup != null) {
                myAutopopup = false;
                hideCurrentPopup();
            }
        }
    }
}
