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
package consulo.language.editor.refactoring.changeSignature;

import consulo.application.util.query.Query;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.highlight.HighlighterFactory;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.Splitter;
import consulo.ui.ex.awt.tree.CheckboxTree;
import consulo.ui.ex.awt.tree.CheckboxTreeBase;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.*;
import java.util.function.Consumer;

public abstract class CallerChooserBase<M extends PsiElement> extends DialogWrapper {
    private final M myMethod;
    private final Alarm myAlarm = new Alarm();
    private MethodNodeBase<M> myRoot;
    protected final Project myProject;
    private Tree myTree;
    private final Consumer<Set<M>> myCallback;
    private TreeSelectionListener myTreeSelectionListener;
    private Editor myCallerEditor;
    private Editor myCalleeEditor;
    private boolean myInitDone;
    private final String myFileName;

    protected abstract MethodNodeBase<M> createTreeNode(M method, HashSet<M> called, Runnable cancelCallback);

    protected abstract M[] findDeepestSuperMethods(M method);

    public CallerChooserBase(M method, Project project, String title, Tree previousTree, String fileName, Consumer<Set<M>> callback) {
        super(true);
        myMethod = method;
        myProject = project;
        myTree = previousTree;
        myFileName = fileName;
        myCallback = callback;
        setTitle(title);
        init();
        myInitDone = true;
    }

    public Tree getTree() {
        return myTree;
    }

    @Override
    protected JComponent createCenterPanel() {
        Splitter splitter = new Splitter(false, (float) 0.6);
        JPanel result = new JPanel(new BorderLayout());
        if (myTree == null) {
            myTree = createTree();
        }
        else {
            CheckedTreeNode root = (CheckedTreeNode) myTree.getModel().getRoot();
            myRoot = (MethodNodeBase) root.getFirstChild();
        }
        myTreeSelectionListener = e -> {
            TreePath path = e.getPath();
            if (path != null) {
                MethodNodeBase<M> node = (MethodNodeBase) path.getLastPathComponent();
                myAlarm.cancelAllRequests();
                myAlarm.addRequest(() -> updateEditorTexts(node), 300);
            }
        };
        myTree.getSelectionModel().addTreeSelectionListener(myTreeSelectionListener);

        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTree);
        splitter.setFirstComponent(scrollPane);
        JComponent callSitesViewer = createCallSitesViewer();
        TreePath selectionPath = myTree.getSelectionPath();
        if (selectionPath == null) {
            selectionPath = new TreePath(myRoot.getPath());
            myTree.getSelectionModel().addSelectionPath(selectionPath);
        }

        MethodNodeBase<M> node = (MethodNodeBase) selectionPath.getLastPathComponent();
        updateEditorTexts(node);

        splitter.setSecondComponent(callSitesViewer);
        result.add(splitter);
        return result;
    }

    @RequiredUIAccess
    private void updateEditorTexts(MethodNodeBase<M> node) {
        MethodNodeBase<M> parentNode = (MethodNodeBase) node.getParent();
        String callerText = node != myRoot ? getText(node.getMethod()) : "";
        Document callerDocument = myCallerEditor.getDocument();
        String calleeText = node != myRoot ? getText(parentNode.getMethod()) : "";
        Document calleeDocument = myCalleeEditor.getDocument();

        myProject.getApplication().runWriteAction(() -> {
            callerDocument.setText(callerText);
            calleeDocument.setText(calleeText);
        });

        M caller = node.getMethod();
        PsiElement callee = parentNode != null ? parentNode.getElementToSearch() : null;
        if (caller != null && caller.isPhysical() && callee != null) {
            HighlightManager highlighter = HighlightManager.getInstance(myProject);
            int start = getStartOffset(caller);
            for (PsiElement element : findElementsToHighlight(caller, callee)) {
                highlighter.addRangeHighlight(
                    myCallerEditor,
                    element.getTextRange().getStartOffset() - start,
                    element.getTextRange().getEndOffset() - start,
                    EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES,
                    false,
                    null
                );
            }
        }
    }

    protected Collection<PsiElement> findElementsToHighlight(M caller, PsiElement callee) {
        Query<PsiReference> references = ReferencesSearch.search(callee, new LocalSearchScope(caller), false);
        return ContainerUtil.mapNotNull(references, PsiReference::getElement);
    }

    @Override
    public void dispose() {
        if (myTree != null) {
            myTree.removeTreeSelectionListener(myTreeSelectionListener);
            EditorFactory.getInstance().releaseEditor(myCallerEditor);
            EditorFactory.getInstance().releaseEditor(myCalleeEditor);
        }
        super.dispose();
    }

    private String getText(M method) {
        if (method == null) {
            return "";
        }
        PsiFile file = method.getContainingFile();
        Document document = PsiDocumentManager.getInstance(myProject).getDocument(file);
        if (document != null) {
            int start = document.getLineStartOffset(document.getLineNumber(method.getTextRange().getStartOffset()));
            int end = document.getLineEndOffset(document.getLineNumber(method.getTextRange().getEndOffset()));
            return document.getText().substring(start, end);
        }
        return "";
    }

    private int getStartOffset(@Nonnull M method) {
        PsiFile file = method.getContainingFile();
        Document document = PsiDocumentManager.getInstance(myProject).getDocument(file);
        return document.getLineStartOffset(document.getLineNumber(method.getTextRange().getStartOffset()));
    }

    private JComponent createCallSitesViewer() {
        Splitter splitter = new Splitter(true);
        myCallerEditor = createEditor();
        myCalleeEditor = createEditor();
        JComponent callerComponent = myCallerEditor.getComponent();
        callerComponent.setBorder(
            IdeBorderFactory.createTitledBorder(RefactoringLocalize.callerChooserCallerMethod().get(), false)
        );
        splitter.setFirstComponent(callerComponent);
        JComponent calleeComponent = myCalleeEditor.getComponent();
        calleeComponent.setBorder(
            IdeBorderFactory.createTitledBorder(RefactoringLocalize.callerChooserCalleeMethod().get(), false)
        );
        splitter.setSecondComponent(calleeComponent);
        splitter.setBorder(IdeBorderFactory.createRoundedBorder());
        return splitter;
    }

    private Editor createEditor() {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument("");
        Editor editor = editorFactory.createViewer(document, myProject);
        ((EditorEx) editor).setHighlighter(HighlighterFactory.createHighlighter(myProject, myFileName));
        return editor;
    }

    private Tree createTree() {
        Runnable cancelCallback = () -> {
            if (myInitDone) {
                close(CANCEL_EXIT_CODE);
            }
            else {
                throw new ProcessCanceledException();
            }
        };
        CheckedTreeNode root = createTreeNode(null, new HashSet<>(), cancelCallback);
        myRoot = createTreeNode(myMethod, new HashSet<>(), cancelCallback);
        root.add(myRoot);
        CheckboxTree.CheckboxTreeCellRenderer cellRenderer = new CheckboxTree.CheckboxTreeCellRenderer(true, false) {
            @Override
            public void customizeRenderer(
                JTree tree,
                Object value,
                boolean selected,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus
            ) {
                if (value instanceof MethodNodeBase methodNodeBase) {
                    methodNodeBase.customizeRenderer(getTextRenderer());
                }
            }
        };
        Tree tree = new CheckboxTree(cellRenderer, root, new CheckboxTreeBase.CheckPolicy(false, true, true, false));
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.getSelectionModel().setSelectionPath(new TreePath(myRoot.getPath()));

        return tree;
    }

    private void getSelectedMethods(Set<M> methods) {
        MethodNodeBase<M> node = myRoot;
        getSelectedMethodsInner(node, methods);
        methods.remove(node.getMethod());
    }

    private void getSelectedMethodsInner(MethodNodeBase<M> node, Set<M> allMethods) {
        if (node.isChecked()) {
            M method = node.getMethod();
            M[] superMethods = findDeepestSuperMethods(method);
            if (superMethods.length == 0) {
                allMethods.add(method);
            }
            else {
                allMethods.addAll(Arrays.asList(superMethods));
            }

            Enumeration children = node.children();
            while (children.hasMoreElements()) {
                getSelectedMethodsInner((MethodNodeBase) children.nextElement(), allMethods);
            }
        }
    }

    @Override
    protected void doOKAction() {
        Set<M> selectedMethods = new HashSet<>();
        getSelectedMethods(selectedMethods);
        myCallback.accept(selectedMethods);
        super.doOKAction();
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myTree;
    }
}
