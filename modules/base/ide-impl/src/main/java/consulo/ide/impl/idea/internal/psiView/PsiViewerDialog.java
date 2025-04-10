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
package consulo.ide.impl.idea.internal.psiView;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AccessToken;
import consulo.application.ui.DimensionService;
import consulo.codeEditor.*;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.event.SelectionEvent;
import consulo.codeEditor.event.SelectionListener;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.internal.psiView.formattingblocks.BlockTreeBuilder;
import consulo.ide.impl.idea.internal.psiView.formattingblocks.BlockTreeNode;
import consulo.ide.impl.idea.internal.psiView.formattingblocks.BlockTreeStructure;
import consulo.ide.impl.idea.openapi.fileTypes.impl.AbstractFileType;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.*;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.file.FileTypeManager;
import consulo.language.file.LanguageFileType;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.impl.DebugUtil;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.plain.PlainTextFileType;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FilenameIndex;
import consulo.language.util.IncorrectOperationException;
import consulo.language.version.LanguageVersion;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.Size;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.style.StandardColors;
import consulo.ui.util.LightDarkColorValue;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Konstantin Bulenkov
 */
public class PsiViewerDialog extends DialogWrapper implements DataProvider, Disposable {
    private static final String REFS_CACHE = "References Resolve Cache";
    private static final ColorValue SELECTION_BG_COLOR = new LightDarkColorValue(new RGBColor(0, 153, 153), new RGBColor(0, 80, 80));
    private static final Logger LOG = Logger.getInstance(PsiViewerDialog.class);
    private final Project myProject;

    private JPanel myPanel;
    private JComboBox myFileTypeComboBox;
    private JCheckBox myShowWhiteSpacesBox;
    private JCheckBox myShowTreeNodesCheckBox;
    private JBLabel myDialectLabel;
    private JComboBox myDialectComboBox;
    private JLabel myExtensionLabel;
    private JComboBox myExtensionComboBox;
    private JPanel myTextPanel;
    private JPanel myStructureTreePanel;
    private JPanel myReferencesPanel;
    private JSplitPane myTextSplit;
    private JSplitPane myTreeSplit;
    private Tree myPsiTree;
    private ViewerTreeBuilder myPsiTreeBuilder;
    private JList myRefs;

    private Tree myBlockTree;
    private JPanel myBlockStructurePanel;
    private JSplitPane myBlockRefSplitPane;
    private JCheckBox myShowBlocksCheckBox;
    private TitledSeparator myTextSeparator;
    private TitledSeparator myPsiTreeSeparator;
    private TitledSeparator myRefsSeparator;
    private TitledSeparator myBlockTreeSeparator;
    @Nullable
    private BlockTreeBuilder myBlockTreeBuilder;
    private RangeHighlighter myHighlighter;
    private RangeHighlighter myIntersectHighlighter;
    private HashMap<PsiElement, BlockTreeNode> myPsiToBlockMap;

    private final Set<SourceWrapper> mySourceWrappers = new TreeSet<>();
    private final EditorEx myEditor;
    private final EditorListener myEditorListener = new EditorListener();
    private String myLastParsedText = null;
    private int myLastParsedTextHashCode = 17;
    private int myNewDocumentHashCode = 11;

    private int myIgnoreBlockTreeSelectionMarker = 0;

    private PsiFile myCurrentFile;
    private String myInitText;
    private String myFileType;

    private void createUIComponents() {
        myPsiTree = new Tree(new DefaultTreeModel(new DefaultMutableTreeNode()));
        myBlockTree = new Tree(new DefaultTreeModel(new DefaultMutableTreeNode()));
        myRefs = new JBList(new DefaultListModel());
    }

    private static class ExtensionComparator implements Comparator<String> {
        private final String myOnTop;

        public ExtensionComparator(String onTop) {
            myOnTop = onTop;
        }

        @Override
        public int compare(String o1, String o2) {
            if (o1.equals(myOnTop)) {
                return -1;
            }
            if (o2.equals(myOnTop)) {
                return 1;
            }
            return o1.compareToIgnoreCase(o2);
        }
    }

    private static class SourceWrapper implements Comparable<SourceWrapper> {
        private final FileType myFileType;
        private final PsiViewerExtension myExtension;

        public SourceWrapper(FileType fileType) {
            myFileType = fileType;
            myExtension = null;
        }

        public SourceWrapper(PsiViewerExtension extension) {
            myFileType = null;
            myExtension = extension;
        }

        public String getText() {
            return myFileType != null ? myFileType.getName() + " file" : myExtension.getName();
        }

        @Nullable
        public Icon getIcon() {
            return myFileType != null ? TargetAWT.to(myFileType.getIcon()) : myExtension.getIcon();
        }

        @Override
        public int compareTo(@Nonnull SourceWrapper o) {
            return o == null ? -1 : getText().compareToIgnoreCase(o.getText());
        }
    }

    @RequiredUIAccess
    public PsiViewerDialog(Project project, boolean modal, @Nullable PsiFile currentFile, @Nullable Editor currentEditor) {
        super(project, true);
        myCurrentFile = currentFile;
        myProject = project;
        setModal(modal);
        setOKButtonText("&Build PSI Tree");
        setCancelButtonText("&Close");
        Disposer.register(myProject, getDisposable());
        EditorEx editor = null;
        if (myCurrentFile == null) {
            setTitle("PSI Viewer");
        }
        else {
            setTitle("PSI Context Viewer: " + myCurrentFile.getName());
            myFileType = myCurrentFile.getLanguage().getDisplayName();
            if (currentEditor != null) {
                myInitText = currentEditor.getSelectionModel().getSelectedText();
            }
            if (myInitText == null) {
                myInitText = currentFile.getText();
                editor = (EditorEx)EditorFactory.getInstance().createEditor(currentFile.getViewProvider().getDocument(), myProject);
            }
        }
        if (editor == null) {
            Document document = EditorFactory.getInstance().createDocument("");
            editor = (EditorEx)EditorFactory.getInstance().createEditor(document, myProject);
        }
        editor.getSettings().setLineMarkerAreaShown(false);
        myEditor = editor;
        init();
        if (myCurrentFile != null) {
            doOKAction();
        }
    }

    @Override
    @RequiredUIAccess
    protected void init() {
        initMnemonics();

        initTree(myPsiTree);
        TreeCellRenderer renderer = myPsiTree.getCellRenderer();
        myPsiTree.setCellRenderer((tree, value, selected, expanded, leaf, row, hasFocus) -> {
            Component c = renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode mutableTreeNode
                && mutableTreeNode.getUserObject() instanceof ViewerNodeDescriptor viewerNodeDescriptor) {
                Object element = viewerNodeDescriptor.getElement();
                if (c instanceof NodeRenderer nodeRenderer) {
                    nodeRenderer.setToolTipText(element == null ? null : element.getClass().getName());
                }
                if ((element instanceof PsiElement psiElement && FileContextUtil.getFileContext(psiElement.getContainingFile()) != null)
                    || element instanceof ViewerTreeStructure.Inject) {
                    TextAttributes attr =
                        EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.INJECTED_LANGUAGE_FRAGMENT);
                    c.setBackground(TargetAWT.to(attr.getBackgroundColor()));
                }
            }
            return c;
        });
        myPsiTreeBuilder = new ViewerTreeBuilder(myProject, myPsiTree);
        Disposer.register(getDisposable(), myPsiTreeBuilder);
        myPsiTree.addTreeSelectionListener(new MyPsiTreeSelectionListener());

        GoToListener listener = new GoToListener();
        myRefs.addKeyListener(listener);
        myRefs.addMouseListener(listener);
        myRefs.getSelectionModel().addListSelectionListener(listener);
        myRefs.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (resolve(index) == null) {
                    comp.setForeground(JBColor.RED);
                }
                return comp;
            }
        });

        initTree(myBlockTree);

        myEditor.getSettings().setFoldingOutlineShown(false);
        myEditor.getDocument().addDocumentListener(myEditorListener);
        myEditor.getSelectionModel().addSelectionListener(myEditorListener);
        myEditor.getCaretModel().addCaretListener(myEditorListener);

        getPeer().getWindow().setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
            @Override
            public Component getInitialComponent(Window window) {
                return myEditor.getComponent();
            }
        });
        PsiViewerSettings settings = PsiViewerSettings.getSettings();
        String type = myFileType != null ? myFileType : settings.type;
        SourceWrapper lastUsed = null;
        PsiViewerExtension.EP_NAME.forEachExtensionSafe(extension -> {
            SourceWrapper wrapper = new SourceWrapper(extension);
            mySourceWrappers.add(wrapper);
        });
        Set<FileType> allFileTypes = new HashSet<>();
        Collections.addAll(allFileTypes, FileTypeManager.getInstance().getRegisteredFileTypes());
        for (Language language : Language.getRegisteredLanguages()) {
            FileType fileType = language.getAssociatedFileType();
            if (fileType != null) {
                allFileTypes.add(fileType);
            }
        }
        Language curLanguage = myCurrentFile != null ? myCurrentFile.getLanguage() : null;
        for (FileType fileType : allFileTypes) {
            if (!(fileType instanceof ArchiveFileType)
                && fileType != UnknownFileType.INSTANCE
                && fileType != PlainTextFileType.INSTANCE
                && !(fileType instanceof AbstractFileType)
                && !fileType.isBinary()
                && !fileType.isReadOnly()) {
                SourceWrapper wrapper = new SourceWrapper(fileType);
                mySourceWrappers.add(wrapper);
                if (lastUsed == null && wrapper.getText().equals(type)) {
                    lastUsed = wrapper;
                }
                if (myCurrentFile != null && wrapper.myFileType instanceof LanguageFileType languageFileType
                    && languageFileType.equals(curLanguage.getAssociatedFileType())) {
                    lastUsed = wrapper;
                }
            }
        }
        myFileTypeComboBox.setModel(new CollectionComboBoxModel(new ArrayList<>(mySourceWrappers), lastUsed));
        myFileTypeComboBox.setRenderer(new ListCellRendererWrapper<SourceWrapper>() {
            @Override
            public void customize(JList list, SourceWrapper value, int index, boolean selected, boolean hasFocus) {
                if (value != null) {
                    setText(value.getText());
                    setIcon(value.getIcon());
                }
            }
        });
        new ComboboxSpeedSearch(myFileTypeComboBox) {
            @Override
            protected String getElementText(Object element) {
                return element instanceof SourceWrapper sourceWrapper ? sourceWrapper.getText() : null;
            }
        };
        myFileTypeComboBox.addActionListener(e -> {
            updateVersionsCombo(null);
            updateExtensionsCombo();
            updateEditor();
        });
        myDialectComboBox.addActionListener(e -> updateEditor());
        myFileTypeComboBox.addFocusListener(new AutoExpandFocusListener(myFileTypeComboBox));
        if (myCurrentFile == null && lastUsed == null && mySourceWrappers.size() > 0) {
            myFileTypeComboBox.setSelectedIndex(0);
        }

        myDialectComboBox.setRenderer(new ListCellRendererWrapper<LanguageVersion>() {
            @Override
            public void customize(JList list, LanguageVersion value, int index, boolean selected, boolean hasFocus) {
                setText(value != null ? value.getName() : "<default>");
            }
        });
        myDialectComboBox.addFocusListener(new AutoExpandFocusListener(myDialectComboBox));
        myExtensionComboBox.setRenderer(new ListCellRendererWrapper<String>() {
            @Override
            public void customize(JList list, String value, int index, boolean selected, boolean hasFocus) {
                if (value != null) {
                    setText("." + value);
                }
            }
        });
        myExtensionComboBox.addFocusListener(new AutoExpandFocusListener(myExtensionComboBox));

        ViewerTreeStructure psiTreeStructure = (ViewerTreeStructure)myPsiTreeBuilder.getTreeStructure();
        myShowWhiteSpacesBox.addActionListener(e -> {
            psiTreeStructure.setShowWhiteSpaces(myShowWhiteSpacesBox.isSelected());
            myPsiTreeBuilder.queueUpdate();
        });
        myShowTreeNodesCheckBox.addActionListener(e -> {
            psiTreeStructure.setShowTreeNodes(myShowTreeNodesCheckBox.isSelected());
            myPsiTreeBuilder.queueUpdate();
        });
        myShowWhiteSpacesBox.setSelected(settings.showWhiteSpaces);
        psiTreeStructure.setShowWhiteSpaces(settings.showWhiteSpaces);
        myShowTreeNodesCheckBox.setSelected(settings.showTreeNodes);
        psiTreeStructure.setShowTreeNodes(settings.showTreeNodes);
        myShowBlocksCheckBox.setSelected(settings.showBlocks);
        myBlockStructurePanel.setVisible(settings.showBlocks);
        myShowBlocksCheckBox.addActionListener(e -> {
            if (!myShowBlocksCheckBox.isSelected()) {
                settings.blockRefDividerLocation = myBlockRefSplitPane.getDividerLocation();
            }
            else {
                myBlockRefSplitPane.setDividerLocation(settings.blockRefDividerLocation);
            }
            myBlockStructurePanel.setVisible(myShowBlocksCheckBox.isSelected());
            myBlockStructurePanel.repaint();
        });
        myTextPanel.setLayout(new BorderLayout());
        myTextPanel.add(myEditor.getComponent(), BorderLayout.CENTER);

        String text = myCurrentFile == null ? settings.text : myInitText;
        AccessToken token = myProject.getApplication().acquireWriteActionLock(getClass());
        try {
            myEditor.getDocument().setText(text);
            myEditor.getSelectionModel().setSelection(0, text.length());
        }
        finally {
            token.finish();
        }

        updateVersionsCombo(settings.dialect);
        updateExtensionsCombo();

        registerCustomKeyboardActions();

        Size size = DimensionService.getInstance().getSize(getDimensionServiceKey(), myProject);
        if (size == null) {
            DimensionService.getInstance().setSize(getDimensionServiceKey(), new Size(800, 600));
        }
        myTextSplit.setDividerLocation(settings.textDividerLocation);
        myTreeSplit.setDividerLocation(settings.treeDividerLocation);
        myBlockRefSplitPane.setDividerLocation(settings.blockRefDividerLocation);

        updateEditor();
        super.init();
    }

    private static void initTree(JTree tree) {
        UIUtil.setLineStyleAngled(tree);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.updateUI();
        ToolTipManager.sharedInstance().registerComponent(tree);
        TreeUtil.installActions(tree);
        new TreeSpeedSearch(tree);
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#consulo.ide.impl.idea.internal.psiView.PsiViewerDialog";
    }

    @Override
    protected String getHelpId() {
        return "reference.psi.viewer";
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myEditor.getContentComponent();
    }

    private void registerCustomKeyboardActions() {
        int mask = Platform.current().os().isMac() ? InputEvent.META_DOWN_MASK : InputEvent.ALT_DOWN_MASK;

        registerKeyboardAction(e -> focusEditor(), KeyStroke.getKeyStroke(KeyEvent.VK_T, mask));

        registerKeyboardAction(e -> focusTree(), KeyStroke.getKeyStroke(KeyEvent.VK_S, mask));

        registerKeyboardAction(e -> focusBlockTree(), KeyStroke.getKeyStroke(KeyEvent.VK_K, mask));

        registerKeyboardAction(e -> focusRefs(), KeyStroke.getKeyStroke(KeyEvent.VK_R, mask));

        registerKeyboardAction(e -> {
            if (myRefs.isFocusOwner()) {
                focusBlockTree();
            }
            else if (myPsiTree.isFocusOwner()) {
                focusRefs();
            }
            else if (myBlockTree.isFocusOwner()) {
                focusTree();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
    }

    private void registerKeyboardAction(ActionListener actionListener, KeyStroke keyStroke) {
        getRootPane().registerKeyboardAction(actionListener, keyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void focusEditor() {
        ProjectIdeFocusManager.getInstance(myProject).requestFocus(myEditor.getContentComponent(), true);
    }

    private void focusTree() {
        ProjectIdeFocusManager.getInstance(myProject).requestFocus(myPsiTree, true);
    }

    private void focusRefs() {
        ProjectIdeFocusManager.getInstance(myProject).requestFocus(myRefs, true);
        if (myRefs.getModel().getSize() > 0) {
            if (myRefs.getSelectedIndex() == -1) {
                myRefs.setSelectedIndex(0);
            }
        }
    }

    private void focusBlockTree() {
        ProjectIdeFocusManager.getInstance(myProject).requestFocus(myBlockTree, true);
    }

    private void initMnemonics() {
        myTextSeparator.setLabelFor(myEditor.getContentComponent());
        myPsiTreeSeparator.setLabelFor(myPsiTree);
        myRefsSeparator.setLabelFor(myRefs);
        myBlockTreeSeparator.setLabelFor(myBlockTree);
    }

    private void updateIntersectHighlighter(int highlightStart, int highlightEnd) {
        if (myIntersectHighlighter != null) {
            myEditor.getMarkupModel().removeHighlighter(myIntersectHighlighter);
            myIntersectHighlighter.dispose();
        }
        if (myEditor.getSelectionModel().hasSelection()) {
            int selectionStart = myEditor.getSelectionModel().getSelectionStart();
            int selectionEnd = myEditor.getSelectionModel().getSelectionEnd();
            TextRange resRange = new TextRange(highlightStart, highlightEnd).intersection(new TextRange(selectionStart, selectionEnd));
            if (resRange != null) {
                TextAttributes attributes = new TextAttributes();
                attributes.setBackgroundColor(StandardColors.LIGHT_GRAY);
                attributes.setForegroundColor(StandardColors.WHITE);
                myIntersectHighlighter = myEditor.getMarkupModel().addRangeHighlighter(
                    resRange.getStartOffset(),
                    resRange.getEndOffset(),
                    HighlighterLayer.LAST + 1,
                    attributes,
                    HighlighterTargetArea.EXACT_RANGE
                );
            }
        }
    }

    @Nullable
    private PsiElement getPsiElement() {
        TreePath path = myPsiTree.getSelectionPath();
        return path == null ? null : getPsiElement((DefaultMutableTreeNode)path.getLastPathComponent());
    }

    @Nullable
    private static PsiElement getPsiElement(DefaultMutableTreeNode node) {
        if (node.getUserObject() instanceof ViewerNodeDescriptor descriptor) {
            Object elementObject = descriptor.getElement();
            return elementObject instanceof PsiElement psiElement ? psiElement
                : elementObject instanceof ASTNode astNode ? astNode.getPsi() : null;
        }
        return null;
    }

    private void updateVersionsCombo(@Nullable String lastUsed) {
        Object source = getSource();
        List<LanguageVersion> items = new ArrayList<>();
        if (source instanceof LanguageFileType languageFileType) {
            Language baseLang = languageFileType.getLanguage();
            LanguageVersion[] versions = baseLang.getVersions();

            Collections.addAll(items, versions);
        }
        myDialectComboBox.setModel(new CollectionComboBoxModel(items));

        int size = items.size();
        boolean visible = size > 1;
        myDialectLabel.setVisible(visible);
        myDialectComboBox.setVisible(visible);
        if (visible && (myCurrentFile != null || lastUsed != null)) {
            String currentLaversion = myCurrentFile != null ? myCurrentFile.getLanguageVersion().getName() : lastUsed;
            for (int i = 0; i < size; ++i) {
                if (currentLaversion.equals(items.get(i).getName())) {
                    myDialectComboBox.setSelectedIndex(i);
                    return;
                }
            }
            myDialectComboBox.setSelectedIndex(size > 0 ? 0 : -1);
        }
    }

    private void updateExtensionsCombo() {
        Object source = getSource();
        if (source instanceof LanguageFileType languageFileType) {
            List<String> extensions = getAllExtensions(languageFileType);
            if (extensions.size() > 1) {
                ExtensionComparator comp = new ExtensionComparator(extensions.get(0));
                Collections.sort(extensions, comp);
                SortedComboBoxModel<String> model = new SortedComboBoxModel<>(comp);
                model.setAll(extensions);
                myExtensionComboBox.setModel(model);
                myExtensionComboBox.setVisible(true);
                myExtensionLabel.setVisible(true);
                String fileExt = myCurrentFile != null ? FileUtil.getExtension(myCurrentFile.getName()) : "";
                if (fileExt.length() > 0 && extensions.contains(fileExt)) {
                    myExtensionComboBox.setSelectedItem(fileExt);
                    return;
                }
                myExtensionComboBox.setSelectedIndex(0);
                return;
            }
        }
        myExtensionComboBox.setVisible(false);
        myExtensionLabel.setVisible(false);
    }

    private static final Pattern EXT_PATTERN = Pattern.compile("[a-z0-9]*");

    private static List<String> getAllExtensions(LanguageFileType fileType) {
        List<FileNameMatcher> associations = FileTypeManager.getInstance().getAssociations(fileType);
        List<String> extensions = new ArrayList<>();
        extensions.add(fileType.getDefaultExtension().toLowerCase());
        for (FileNameMatcher matcher : associations) {
            String presentableString = matcher.getPresentableString().toLowerCase();
            if (presentableString.startsWith("*.")) {
                String ext = presentableString.substring(2);
                if (ext.length() > 0 && !extensions.contains(ext) && EXT_PATTERN.matcher(ext).matches()) {
                    extensions.add(ext);
                }
            }
        }
        return extensions;
    }

    @Override
    protected JComponent createCenterPanel() {
        return myPanel;
    }

    @Nullable
    private Object getSource() {
        SourceWrapper wrapper = (SourceWrapper)myFileTypeComboBox.getSelectedItem();
        if (wrapper != null) {
            return wrapper.myFileType != null ? wrapper.myFileType : wrapper.myExtension;
        }
        return null;
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    protected Action[] createActions() {
        AbstractAction copyPsi = new AbstractAction("Cop&y PSI") {
            @Override
            @RequiredUIAccess
            public void actionPerformed(ActionEvent e) {
                PsiElement element = parseText(myEditor.getDocument().getText());
                List<PsiElement> allToParse = new ArrayList<>();
                if (element instanceof PsiFile file) {
                    allToParse.addAll(file.getViewProvider().getAllFiles());
                }
                else if (element != null) {
                    allToParse.add(element);
                }
                String data = "";
                for (PsiElement psiElement : allToParse) {
                    data += DebugUtil.psiToString(psiElement, !myShowWhiteSpacesBox.isSelected(), true);
                }
                CopyPasteManager.getInstance().setContents(new StringSelection(data));
            }
        };
        return ArrayUtil.mergeArrays(new Action[]{copyPsi}, super.createActions());
    }

    @Override
    @RequiredUIAccess
    protected void doOKAction() {
        if (myBlockTreeBuilder != null) {
            Disposer.dispose(myBlockTreeBuilder);
        }
        String text = myEditor.getDocument().getText();
        myEditor.getSelectionModel().removeSelection();

        myLastParsedText = text;
        myLastParsedTextHashCode = text.hashCode();
        myNewDocumentHashCode = myLastParsedTextHashCode;
        PsiElement rootElement = parseText(text);
        focusTree();
        ViewerTreeStructure structure = (ViewerTreeStructure)myPsiTreeBuilder.getTreeStructure();
        structure.setRootPsiElement(rootElement);

        myPsiTreeBuilder.queueUpdate();
        myPsiTree.setRootVisible(true);
        myPsiTree.expandRow(0);
        myPsiTree.setRootVisible(false);

        if (!myShowBlocksCheckBox.isSelected()) {
            return;
        }
        Block rootBlock = rootElement == null ? null : buildBlocks(rootElement);
        if (rootBlock == null) {
            myBlockTreeBuilder = null;
            myBlockTree.setRootVisible(false);
            myBlockTree.setVisible(false);
            return;
        }

        myBlockTree.setVisible(true);
        BlockTreeStructure blockTreeStructure = new BlockTreeStructure();
        BlockTreeNode rootNode = new BlockTreeNode(rootBlock, null);
        blockTreeStructure.setRoot(rootNode);
        myBlockTreeBuilder = new BlockTreeBuilder(myBlockTree, blockTreeStructure);
        myPsiToBlockMap = new HashMap<>();
        PsiElement psiFile = ((ViewerTreeStructure)myPsiTreeBuilder.getTreeStructure()).getRootPsiElement();
        initMap(rootNode, psiFile);
        PsiElement rootPsi = rootNode.getBlock() instanceof ASTBlock astBlock ? astBlock.getNode().getPsi() : rootElement;
        BlockTreeNode blockNode = myPsiToBlockMap.get(rootPsi);

        if (blockNode == null) {
            //LOG.error(LogMessageEx.createEvent(
            //    "PsiViewer: rootNode not found",
            //    "Current language: " + rootElement.getContainingFile().getLanguage(),
            //    AttachmentFactory.createAttachment(rootElement.getContainingFile().getOriginalFile().getVirtualFile()))
            //);
            blockNode = findBlockNode(rootPsi);
        }


        blockTreeStructure.setRoot(blockNode);
        myBlockTree.addTreeSelectionListener(new MyBlockTreeSelectionListener());
        myBlockTree.setRootVisible(true);
        myBlockTree.expandRow(0);
        myBlockTreeBuilder.queueUpdate();
    }

    @RequiredUIAccess
    private PsiElement parseText(String text) {
        Object source = getSource();
        try {
            if (source instanceof PsiViewerExtension psiViewerExtension) {
                return psiViewerExtension.createElement(myProject, text);
            }
            if (source instanceof FileType type) {
                String ext = type.getDefaultExtension();
                if (myExtensionComboBox.isVisible()) {
                    ext = myExtensionComboBox.getSelectedItem().toString().toLowerCase();
                }
                if (type instanceof LanguageFileType languageFileType) {
                    Language language = languageFileType.getLanguage();
                    LanguageVersion languageVersion = (LanguageVersion)myDialectComboBox.getSelectedItem();
                    return PsiFileFactory.getInstance(myProject).createFileFromText("Dummy." + ext, language, languageVersion, text);
                }
                return PsiFileFactory.getInstance(myProject).createFileFromText("Dummy." + ext, type, text);
            }
        }
        catch (IncorrectOperationException e) {
            Messages.showMessageDialog(myProject, e.getMessage(), "Error", UIUtil.getErrorIcon());
        }
        return null;
    }

    @Nullable
    @RequiredReadAction
    private static Block buildBlocks(@Nonnull PsiElement rootElement) {
        FormattingModelBuilder formattingModelBuilder = FormattingModelBuilder.forContext(rootElement);
        CodeStyleSettings settings = CodeStyleSettingsManager.getSettings(rootElement.getProject());
        if (formattingModelBuilder != null) {
            FormattingModel formattingModel = formattingModelBuilder.createModel(FormattingContext.create(rootElement, settings));
            return formattingModel.getRootBlock();
        }
        else {
            return null;
        }
    }

    @RequiredReadAction
    private void initMap(BlockTreeNode rootBlockNode, PsiElement psiEl) {
        PsiElement currentElem = null;
        if (rootBlockNode.getBlock() instanceof ASTBlock astBlock) {
            ASTNode node = astBlock.getNode();
            if (node != null) {
                currentElem = node.getPsi();
            }
        }
        if (currentElem == null) {
            currentElem = InjectedLanguageUtil.findElementAtNoCommit(
                psiEl.getContainingFile(),
                rootBlockNode.getBlock().getTextRange().getStartOffset()
            );
        }
        myPsiToBlockMap.put(currentElem, rootBlockNode);

//nested PSI elements with same ranges will be mapped to one blockNode
//    assert currentElem != null;      //for Scala-language plugin etc it can be null, because formatterBlocks is not instance of ASTBlock
        TextRange curTextRange = currentElem.getTextRange();
        PsiElement parentElem = currentElem.getParent();
        while (parentElem != null && parentElem.getTextRange().equals(curTextRange)) {
            myPsiToBlockMap.put(parentElem, rootBlockNode);
            parentElem = parentElem.getParent();
        }
        for (BlockTreeNode block : rootBlockNode.getChildren()) {
            initMap(block, psiEl);
        }
    }

    @Override
    public Object getData(@Nonnull @NonNls Key<?> dataId) {
        if (Navigatable.KEY == dataId) {
            String fqn = null;
            if (myPsiTree.hasFocus()) {
                TreePath path = myPsiTree.getSelectionPath();
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
                    if (!(node.getUserObject() instanceof ViewerNodeDescriptor)) {
                        return null;
                    }
                    ViewerNodeDescriptor descriptor = (ViewerNodeDescriptor)node.getUserObject();
                    Object elementObject = descriptor.getElement();
                    PsiElement element = elementObject instanceof PsiElement psiElement ? psiElement
                        : elementObject instanceof ASTNode astNode ? astNode.getPsi() : null;
                    if (element != null) {
                        fqn = element.getClass().getName();
                    }
                }
            }
            else if (myRefs.hasFocus()) {
                Object value = myRefs.getSelectedValue();
                if (value instanceof String s) {
                    fqn = s;
                }
            }
            if (fqn != null) {
                return getContainingFileForClass(fqn);
            }
        }
        return null;
    }

    private class MyPsiTreeSelectionListener implements TreeSelectionListener {
        private final TextAttributes myAttributes;

        public MyPsiTreeSelectionListener() {
            myAttributes = new TextAttributes();
            myAttributes.setBackgroundColor(SELECTION_BG_COLOR);
            myAttributes.setForegroundColor(StandardColors.WHITE);
        }

        @Override
        @RequiredUIAccess
        public void valueChanged(TreeSelectionEvent e) {
            if (!myEditor.getDocument().getText().equals(myLastParsedText) || myBlockTree.hasFocus()) {
                return;
            }
            TreePath path = myPsiTree.getSelectionPath();
            clearSelection();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
                if (!(node.getUserObject() instanceof ViewerNodeDescriptor)) {
                    return;
                }
                ViewerNodeDescriptor descriptor = (ViewerNodeDescriptor)node.getUserObject();
                Object elementObject = descriptor.getElement();
                PsiElement element = elementObject instanceof PsiElement psiElement ? psiElement
                    : elementObject instanceof ASTNode astNode ? astNode.getPsi() : null;
                if (element != null) {
                    TextRange rangeInHostFile =
                        InjectedLanguageManager.getInstance(myProject).injectedToHost(element, element.getTextRange());
                    int start = rangeInHostFile.getStartOffset();
                    int end = rangeInHostFile.getEndOffset();
                    ViewerTreeStructure treeStructure = (ViewerTreeStructure)myPsiTreeBuilder.getTreeStructure();
                    PsiElement rootPsiElement = treeStructure.getRootPsiElement();
                    if (rootPsiElement != null) {
                        int baseOffset = rootPsiElement.getTextRange().getStartOffset();
                        start -= baseOffset;
                        end -= baseOffset;
                    }
                    int textLength = myEditor.getDocument().getTextLength();
                    if (end <= textLength) {
                        myHighlighter = myEditor.getMarkupModel()
                            .addRangeHighlighter(start, end, HighlighterLayer.LAST, myAttributes, HighlighterTargetArea.EXACT_RANGE);
                        updateIntersectHighlighter(start, end);

                        if (myPsiTree.hasFocus()) {
                            myEditor.getCaretModel().moveToOffset(start);
                            myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
                        }
                    }
                    if (myBlockTreeBuilder != null && myPsiTree.hasFocus()) {
                        BlockTreeNode currentBlockNode = findBlockNode(element);
                        if (currentBlockNode != null) {
                            selectBlockNode(currentBlockNode);
                        }
                    }
                    updateReferences(element);
                }
            }
        }
    }

    @Nullable
    @RequiredReadAction
    private BlockTreeNode findBlockNode(PsiElement element) {
        BlockTreeNode result = myPsiToBlockMap.get(element);
        if (result == null) {
            TextRange rangeInHostFile = InjectedLanguageManager.getInstance(myProject).injectedToHost(element, element.getTextRange());
            result = findBlockNode(rangeInHostFile, true);
        }
        return result;
    }

    private void selectBlockNode(@Nullable BlockTreeNode currentBlockNode) {
        if (myBlockTreeBuilder == null) {
            return;
        }
        if (currentBlockNode != null) {
            myIgnoreBlockTreeSelectionMarker++;
            myBlockTreeBuilder.select(currentBlockNode, () -> {
                // hope this is always called!
                assert myIgnoreBlockTreeSelectionMarker > 0;
                myIgnoreBlockTreeSelectionMarker--;
            });
        }
        else {
            myIgnoreBlockTreeSelectionMarker++;
            try {
                myBlockTree.getSelectionModel().clearSelection();
            }
            finally {
                assert myIgnoreBlockTreeSelectionMarker > 0;
                myIgnoreBlockTreeSelectionMarker--;
            }
        }
    }

    private class MyBlockTreeSelectionListener implements TreeSelectionListener {
        private final TextAttributes myAttributes;

        public MyBlockTreeSelectionListener() {
            myAttributes = new TextAttributes();
            myAttributes.setBackgroundColor(SELECTION_BG_COLOR);
            myAttributes.setForegroundColor(StandardColors.WHITE);
        }

        @Override
        @RequiredUIAccess
        public void valueChanged(TreeSelectionEvent e) {
            if (myIgnoreBlockTreeSelectionMarker > 0 || myBlockTreeBuilder == null) {
                return;
            }

            Set<?> blockElementsSet = myBlockTreeBuilder.getSelectedElements();
            if (blockElementsSet.isEmpty()) {
                return;
            }
            BlockTreeNode descriptor = (BlockTreeNode)blockElementsSet.iterator().next();
            PsiElement rootPsi = ((ViewerTreeStructure)myPsiTreeBuilder.getTreeStructure()).getRootPsiElement();
            int blockStart = descriptor.getBlock().getTextRange().getStartOffset();
            PsiElement currentPsiEl = InjectedLanguageUtil.findElementAtNoCommit(rootPsi.getContainingFile(), blockStart);
            int blockLength = descriptor.getBlock().getTextRange().getLength();
            while (currentPsiEl.getParent() != null
                && currentPsiEl.getTextRange().getStartOffset() == blockStart
                && currentPsiEl.getTextLength() != blockLength) {
                currentPsiEl = currentPsiEl.getParent();
            }
            BlockTreeStructure treeStructure = (BlockTreeStructure)myBlockTreeBuilder.getTreeStructure();
            BlockTreeNode rootBlockNode = treeStructure.getRootElement();
            int baseOffset = 0;
            if (rootBlockNode != null) {
                baseOffset = rootBlockNode.getBlock().getTextRange().getStartOffset();
            }
            if (currentPsiEl != null) {
                TextRange range = descriptor.getBlock().getTextRange();
                range = range.shiftRight(-baseOffset);
                int start = range.getStartOffset();
                int end = range.getEndOffset();
                int textLength = myEditor.getDocument().getTextLength();

                if (myBlockTree.hasFocus()) {
                    clearSelection();
                    if (end <= textLength) {
                        myHighlighter = myEditor.getMarkupModel()
                            .addRangeHighlighter(start, end, HighlighterLayer.LAST, myAttributes, HighlighterTargetArea.EXACT_RANGE);
                        updateIntersectHighlighter(start, end);

                        myEditor.getCaretModel().moveToOffset(start);
                        myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
                    }
                }
                updateReferences(currentPsiEl);
                if (!myPsiTree.hasFocus()) {
                    myPsiTreeBuilder.select(currentPsiEl);
                }
            }
        }
    }

    public void updateReferences(PsiElement element) {
        DefaultListModel model = (DefaultListModel)myRefs.getModel();
        model.clear();
        Object cache = myRefs.getClientProperty(REFS_CACHE);
        if (cache instanceof Map cacheMap) {
            cacheMap.clear();
        }
        else {
            myRefs.putClientProperty(REFS_CACHE, new HashMap());
        }
        if (element != null) {
            for (PsiReference reference : element.getReferences()) {
                model.addElement(reference.getClass().getName());
            }
        }
    }

    private void clearSelection() {
        if (myHighlighter != null) {
            myEditor.getMarkupModel().removeHighlighter(myHighlighter);
            myHighlighter.dispose();
        }
    }

    @Override
    public void doCancelAction() {
        PsiViewerSettings settings = PsiViewerSettings.getSettings();
        SourceWrapper wrapper = (SourceWrapper)myFileTypeComboBox.getSelectedItem();
        if (wrapper != null) {
            settings.type = wrapper.getText();
        }
        settings.text = myEditor.getDocument().getText();
        settings.showTreeNodes = myShowTreeNodesCheckBox.isSelected();
        settings.showWhiteSpaces = myShowWhiteSpacesBox.isSelected();
        Object selectedDialect = myDialectComboBox.getSelectedItem();
        settings.dialect = myDialectComboBox.isVisible() && selectedDialect != null ? selectedDialect.toString() : "";
        settings.textDividerLocation = myTextSplit.getDividerLocation();
        settings.treeDividerLocation = myTreeSplit.getDividerLocation();
        settings.showBlocks = myShowBlocksCheckBox.isSelected();
        if (myShowBlocksCheckBox.isSelected()) {
            settings.blockRefDividerLocation = myBlockRefSplitPane.getDividerLocation();
        }
        super.doCancelAction();
    }

    @Override
    public void dispose() {
        Disposer.dispose(myPsiTreeBuilder);
        if (myBlockTreeBuilder != null) {
            Disposer.dispose(myBlockTreeBuilder);
        }
        if (!myEditor.isDisposed()) {
            EditorFactory.getInstance().releaseEditor(myEditor);
        }
        super.dispose();
    }

    @Nullable
    @RequiredReadAction
    private PsiElement resolve(int index) {
        PsiElement element = getPsiElement();
        if (element == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<PsiElement, PsiElement[]> map = (Map<PsiElement, PsiElement[]>)myRefs.getClientProperty(REFS_CACHE);
        if (map == null) {
            myRefs.putClientProperty(REFS_CACHE, map = new HashMap<>());
        }
        PsiElement[] cache = map.get(element);
        if (cache == null) {
            PsiReference[] references = element.getReferences();
            cache = new PsiElement[references.length];
            for (int i = 0; i < references.length; i++) {
                cache[i] = references[i].resolve();
            }
            map.put(element, cache);
        }
        return index >= cache.length ? null : cache[index];
    }

    @Nullable
    private PsiFile getContainingFileForClass(String fqn) {
        String filename = fqn;
        if (fqn.contains(".")) {
            filename = fqn.substring(fqn.lastIndexOf('.') + 1);
        }
        if (filename.contains("$")) {
            filename = filename.substring(0, filename.indexOf('$'));
        }
        filename += ".java";
        PsiFile[] files = FilenameIndex.getFilesByName(myProject, filename, GlobalSearchScope.allScope(myProject));
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
    }

    @Nullable
    public static TreeNode findNodeWithObject(Object object, TreeModel model, Object parent) {
        for (int i = 0; i < model.getChildCount(parent); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)model.getChild(parent, i);
            if (childNode.getUserObject().equals(object)) {
                return childNode;
            }
            else {
                TreeNode node = findNodeWithObject(object, model, childNode);
                if (node != null) {
                    return node;
                }
            }
        }
        return null;
    }

    private class GoToListener implements KeyListener, MouseListener, ListSelectionListener {
        private RangeHighlighter myListenerHighlighter;
        private final TextAttributes myAttributes =
            new TextAttributes(StandardColors.WHITE, SELECTION_BG_COLOR, StandardColors.RED, EffectType.BOXED, Font.PLAIN);

        private void navigate() {
            Object value = myRefs.getSelectedValue();
            if (value instanceof String fqn) {
                PsiFile file = getContainingFileForClass(fqn);
                if (file != null) {
                    file.navigate(true);
                }
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                navigate();
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() > 1) {
                navigate();
            }
        }

        @Override
        @RequiredUIAccess
        public void valueChanged(ListSelectionEvent e) {
            clearSelection();
            updateVersionsCombo(null);
            updateExtensionsCombo();
            int ind = myRefs.getSelectedIndex();
            PsiElement element = getPsiElement();
            if (ind > -1 && element != null) {
                PsiReference[] references = element.getReferences();
                if (ind < references.length) {
                    TextRange textRange = references[ind].getRangeInElement();
                    TextRange range = InjectedLanguageManager.getInstance(myProject).injectedToHost(element, element.getTextRange());
                    int start = range.getStartOffset();
                    int end = range.getEndOffset();
                    ViewerTreeStructure treeStructure = (ViewerTreeStructure)myPsiTreeBuilder.getTreeStructure();
                    PsiElement rootPsiElement = treeStructure.getRootPsiElement();
                    if (rootPsiElement != null) {
                        int baseOffset = rootPsiElement.getTextRange().getStartOffset();
                        start -= baseOffset;
                        end -= baseOffset;
                    }

                    start += textRange.getStartOffset();
                    end = start + textRange.getLength();
                    myListenerHighlighter = myEditor.getMarkupModel()
                        .addRangeHighlighter(start, end, HighlighterLayer.FIRST + 1, myAttributes, HighlighterTargetArea.EXACT_RANGE);
                }
            }
        }

        public void clearSelection() {
            if (myListenerHighlighter != null &&
                ArrayUtil.contains(myListenerHighlighter, (Object[])myEditor.getMarkupModel().getAllHighlighters())) {
                myListenerHighlighter.dispose();
                myListenerHighlighter = null;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }
    }

    private void updateEditor() {
        Object source = getSource();

        String fileName = "Dummy." + (source instanceof FileType fileType ? fileType.getDefaultExtension() : "txt");
        LightVirtualFile lightFile;
        if (source instanceof PsiViewerExtension viewerExtension) {
            lightFile = new LightVirtualFile(fileName, viewerExtension.getDefaultFileType(), "");
        }
        else if (source instanceof LanguageFileType languageFileType) {
            lightFile = new LightVirtualFile(fileName, languageFileType.getLanguage(), "");
        }
        else if (source instanceof FileType fileType) {
            lightFile = new LightVirtualFile(fileName, fileType, "");
        }
        else {
            return;
        }
        myEditor.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(myProject, lightFile));
    }

    private class EditorListener implements CaretListener, SelectionListener, DocumentListener {
        @Override
        @RequiredUIAccess
        public void caretPositionChanged(CaretEvent e) {
            if (!available() || myEditor.getSelectionModel().hasSelection()) {
                return;
            }
            ViewerTreeStructure treeStructure = (ViewerTreeStructure)myPsiTreeBuilder.getTreeStructure();
            PsiElement rootPsiElement = treeStructure.getRootPsiElement();
            if (rootPsiElement == null) {
                return;
            }
            PsiElement rootElement = ((ViewerTreeStructure)myPsiTreeBuilder.getTreeStructure()).getRootPsiElement();
            int baseOffset = rootPsiElement.getTextRange().getStartOffset();
            int offset = myEditor.getCaretModel().getOffset() + baseOffset;
            PsiElement element = InjectedLanguageUtil.findElementAtNoCommit(rootElement.getContainingFile(), offset);
            if (element != null && myBlockTreeBuilder != null) {
                TextRange rangeInHostFile = InjectedLanguageManager.getInstance(myProject).injectedToHost(element, element.getTextRange());
                selectBlockNode(findBlockNode(rangeInHostFile, true));
            }
            myPsiTreeBuilder.select(element);
        }

        @Override
        public void caretAdded(CaretEvent e) {

        }

        @Override
        public void caretRemoved(CaretEvent e) {

        }

        @Override
        @RequiredUIAccess
        public void selectionChanged(@Nonnull SelectionEvent e) {
            if (!available() || !myEditor.getSelectionModel().hasSelection()) {
                return;
            }
            ViewerTreeStructure treeStructure = (ViewerTreeStructure)myPsiTreeBuilder.getTreeStructure();
            if (treeStructure == null) {
                return;
            }
            PsiElement rootElement = treeStructure.getRootPsiElement();
            if (rootElement == null) {
                return;
            }
            SelectionModel selection = myEditor.getSelectionModel();
            TextRange textRange = rootElement.getTextRange();
            int baseOffset = textRange != null ? textRange.getStartOffset() : 0;
            int start = selection.getSelectionStart() + baseOffset;
            int end = selection.getSelectionEnd() + baseOffset - 1;
            PsiElement element = findCommonParent(
                InjectedLanguageUtil.findElementAtNoCommit(rootElement.getContainingFile(), start),
                InjectedLanguageUtil.findElementAtNoCommit(rootElement.getContainingFile(), end)
            );
            if (element != null && myBlockTreeBuilder != null && myEditor.getContentComponent().hasFocus()) {
                TextRange rangeInHostFile = InjectedLanguageManager.getInstance(myProject).injectedToHost(element, element.getTextRange());
                selectBlockNode(findBlockNode(rangeInHostFile, true));
                updateIntersectHighlighter(myHighlighter.getStartOffset(), myHighlighter.getEndOffset());
            }
            myPsiTreeBuilder.select(element);
        }

        @Nullable
        @RequiredReadAction
        private PsiElement findCommonParent(PsiElement start, PsiElement end) {
            if (end == null || start == end) {
                return start;
            }
            TextRange endRange = end.getTextRange();
            PsiElement parent = start.getContext();
            while (parent != null && !parent.getTextRange().contains(endRange)) {
                parent = parent.getContext();
            }
            return parent;
        }

        private boolean available() {
            return myLastParsedTextHashCode == myNewDocumentHashCode && myEditor.getContentComponent().hasFocus();
        }

        @Nullable
        private PsiFile getPsiFile() {
            ViewerTreeStructure treeStructure = (ViewerTreeStructure)myPsiTreeBuilder.getTreeStructure();
            PsiElement root = treeStructure != null ? treeStructure.getRootPsiElement() : null;
            return root instanceof PsiFile file ? file : null;
        }

        @Override
        public void beforeDocumentChange(DocumentEvent event) {

        }

        @Override
        public void documentChanged(DocumentEvent event) {
            myNewDocumentHashCode = event.getDocument().getText().hashCode();
        }
    }

    private static class AutoExpandFocusListener extends FocusAdapter {
        private final JComboBox myComboBox;
        private final Component myParent;

        private AutoExpandFocusListener(JComboBox comboBox) {
            myComboBox = comboBox;
            myParent = UIUtil.findUltimateParent(myComboBox);
        }

        @Override
        public void focusGained(FocusEvent e) {
            Component from = e.getOppositeComponent();
            if (!e.isTemporary() && from != null && !myComboBox.isPopupVisible() && isUnder(from, myParent)) {
                myComboBox.setPopupVisible(true);
            }
        }

        private static boolean isUnder(Component component, Component parent) {
            while (component != null) {
                if (component == parent) {
                    return true;
                }
                component = component.getParent();
            }
            return false;
        }
    }

    @Nullable
    private BlockTreeNode findBlockNode(TextRange range, boolean selectParentIfNotFound) {
        BlockTreeBuilder builder = myBlockTreeBuilder;
        if (builder == null || !myBlockStructurePanel.isVisible()) {
            return null;
        }

        AbstractTreeStructure treeStructure = builder.getTreeStructure();
        if (treeStructure == null) {
            return null;
        }
        BlockTreeNode node = (BlockTreeNode)treeStructure.getRootElement();
        main_loop:
        while (true) {
            if (node.getBlock().getTextRange().equals(range)) {
                return node;
            }

            for (BlockTreeNode child : node.getChildren()) {
                if (child.getBlock().getTextRange().contains(range)) {
                    node = child;
                    continue main_loop;
                }
            }
            return selectParentIfNotFound ? node : null;
        }
    }
}
