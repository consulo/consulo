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
package consulo.ide.impl.idea.codeInsight.hint;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.*;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorProvider;
import consulo.fileEditor.FileEditorProviderManager;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.ide.impl.idea.find.FindUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.CompositeShortcutSet;
import consulo.language.editor.ImplementationTextSelectioner;
import consulo.language.editor.highlight.HighlighterFactory;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.image.Image;
import consulo.undoRedo.CommandProcessor;
import consulo.usage.UsageView;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public class ImplementationViewComponent extends JPanel {
    private static final String TEXT_PAGE_KEY = "Text";
    private static final String BINARY_PAGE_KEY = "Binary";

    private PsiElement[] myElements;
    private int myIndex;

    private final Editor myEditor;
    private final JPanel myViewingPanel;
    private final JLabel myLocationLabel;
    private final JLabel myCountLabel;
    private final CardLayout myBinarySwitch;
    private final JPanel myBinaryPanel;
    private ComboBox<FileDescriptor> myFileChooser;
    private FileEditor myNonTextEditor;
    private FileEditorProvider myCurrentNonTextEditorProvider;
    private JBPopup myHint;
    private String myTitle;
    private final ActionToolbar myToolbar;
    private JLabel myLabel;

    public void setHint(JBPopup hint, @Nonnull LocalizeValue title) {
        myHint = hint;
        myTitle = title.get();
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void setHint(JBPopup hint, String title) {
        myHint = hint;
        myTitle = title;
    }

    public boolean hasElementsToShow() {
        return myElements != null && myElements.length > 0;
    }

    private static class FileDescriptor {
        public final PsiFile myFile;
        public final String myElementPresentation;

        @RequiredReadAction
        public FileDescriptor(PsiFile file, PsiElement element) {
            myFile = file;
            myElementPresentation = getPresentation(element);
        }

        @Nullable
        @RequiredReadAction
        private static String getPresentation(PsiElement element) {
            if (element instanceof NavigationItem navigationItem) {
                ItemPresentation presentation = navigationItem.getPresentation();
                if (presentation != null) {
                    return presentation.getPresentableText();
                }
            }

            if (element instanceof PsiNamedElement namedElement) {
                return namedElement.getName();
            }
            return null;
        }

        public String getPresentableName(VirtualFile vFile) {
            String presentableName = vFile.getPresentableName();
            if (myElementPresentation == null) {
                return presentableName;
            }

            if (Comparing.strEqual(vFile.getName(), myElementPresentation + "." + vFile.getExtension())) {
                return presentableName;
            }

            return presentableName + " (" + myElementPresentation + ")";
        }
    }

    @RequiredUIAccess
    public ImplementationViewComponent(PsiElement[] elements, int index) {
        super(new BorderLayout());

        Project project = elements.length > 0 ? elements[0].getProject() : null;
        EditorFactory factory = EditorFactory.getInstance();
        Document doc = factory.createDocument("");
        doc.setReadOnly(true);
        myEditor = factory.createEditor(doc, project);
        ((EditorEx) myEditor).setBackgroundColor(EditorFragmentComponent.getBackgroundColor(myEditor));

        EditorSettings settings = myEditor.getSettings();
        settings.setAdditionalLinesCount(1);
        settings.setAdditionalColumnsCount(1);
        settings.setLineMarkerAreaShown(false);
        settings.setIndentGuidesShown(false);
        settings.setLineNumbersShown(false);
        settings.setFoldingOutlineShown(false);

        myBinarySwitch = new CardLayout();
        myViewingPanel = new JPanel(myBinarySwitch);
        myEditor.setBorder(null);
        ((EditorEx) myEditor).getScrollPane().setViewportBorder(JBScrollPane.createIndentBorder());
        myViewingPanel.add(myEditor.getComponent(), TEXT_PAGE_KEY);

        myBinaryPanel = new JPanel(new BorderLayout());
        myViewingPanel.add(myBinaryPanel, BINARY_PAGE_KEY);

        add(myViewingPanel, BorderLayout.CENTER);

        myToolbar = createToolbar();
        myLocationLabel = new JLabel();
        myCountLabel = new JLabel();

        JPanel header = new JPanel(new BorderLayout(2, 0));
        header.setBorder(BorderFactory.createCompoundBorder(
            IdeBorderFactory.createBorder(SideBorder.BOTTOM),
            IdeBorderFactory.createEmptyBorder(0, 0, 0, 5)
        ));
        JPanel toolbarPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints(
            GridBagConstraints.RELATIVE,
            0,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            JBUI.insetsLeft(2),
            0,
            0
        );
        toolbarPanel.add(myToolbar.getComponent(), gc);

        setPreferredSize(new Dimension(600, 400));

        update(
            elements,
            (psiElements, fileDescriptors) -> {
                if (psiElements.length == 0) {
                    return false;
                }
                myElements = psiElements;

                myIndex = index < myElements.length ? index : 0;
                PsiFile psiFile = getContainingFile(myElements[myIndex]);

                VirtualFile virtualFile = psiFile.getVirtualFile();
                EditorHighlighter highlighter;
                if (virtualFile != null) {
                    highlighter = HighlighterFactory.createHighlighter(project, virtualFile);
                }
                else {
                    String fileName = psiFile.getName();  // some artificial psi file, lets do best we can
                    highlighter = HighlighterFactory.createHighlighter(project, fileName);
                }

                ((EditorEx) myEditor).setHighlighter(highlighter);

                gc.fill = GridBagConstraints.HORIZONTAL;
                gc.weightx = 1;
                myLabel = new JLabel();
                myFileChooser = new ComboBox<>(fileDescriptors.toArray(new FileDescriptor[fileDescriptors.size()]), 250);
                myFileChooser.addActionListener(e -> {
                    int index1 = myFileChooser.getSelectedIndex();
                    if (myIndex != index1) {
                        myIndex = index1;
                        updateControls();
                    }
                });
                toolbarPanel.add(myFileChooser, gc);

                if (myElements.length > 1) {
                    updateRenderer(project);
                    myLabel.setVisible(false);
                }
                else {
                    myFileChooser.setVisible(false);
                    myCountLabel.setVisible(false);

                    VirtualFile file = psiFile.getVirtualFile();
                    if (file != null) {
                        myLabel.setIcon(getIconForFile(psiFile));
                        myLabel.setForeground(TargetAWT.to(FileStatusManager.getInstance(project).getStatus(file).getColor()));
                        myLabel.setText(file.getPresentableName());
                        myLabel.setBorder(new CompoundBorder(
                            IdeBorderFactory.createRoundedBorder(),
                            IdeBorderFactory.createEmptyBorder(0, 0, 0, 5)
                        ));
                    }
                    toolbarPanel.add(myLabel, gc);
                }

                gc.fill = GridBagConstraints.NONE;
                gc.weightx = 0;
                toolbarPanel.add(myCountLabel, gc);

                header.add(toolbarPanel, BorderLayout.CENTER);
                header.add(myLocationLabel, BorderLayout.EAST);

                add(header, BorderLayout.NORTH);

                updateControls();
                return true;
            }
        );
    }

    private void updateRenderer(final Project project) {
        myFileChooser.setRenderer(new ListCellRendererWrapper<>() {
            @Override
            @RequiredReadAction
            public void customize(JList list, FileDescriptor value, int index, boolean selected, boolean hasFocus) {
                PsiFile file = value.myFile;
                setIcon(getIconForFile(file));
                VirtualFile vFile = file.getVirtualFile();
                setForeground(TargetAWT.to(FileStatusManager.getInstance(project).getStatus(vFile).getColor()));
                //noinspection ConstantConditions
                setText(value.getPresentableName(vFile));
            }
        });
    }

    @TestOnly
    public String[] getVisibleFiles() {
        ComboBoxModel<FileDescriptor> model = myFileChooser.getModel();
        String[] result = new String[model.getSize()];
        for (int i = 0; i < model.getSize(); i++) {
            FileDescriptor fd = model.getElementAt(i);
            result[i] = fd.getPresentableName(fd.myFile.getVirtualFile());
        }
        return result;
    }

    @RequiredUIAccess
    public void update(@Nonnull PsiElement[] elements, int index) {
        update(
            elements,
            (psiElements, fileDescriptors) -> {
                if (psiElements.length == 0) {
                    return false;
                }

                Project project = psiElements[0].getProject();
                myElements = psiElements;

                myIndex = index < myElements.length ? index : 0;
                PsiFile psiFile = getContainingFile(myElements[myIndex]);

                VirtualFile virtualFile = psiFile.getVirtualFile();
                EditorHighlighter highlighter;
                if (virtualFile != null) {
                    highlighter = HighlighterFactory.createHighlighter(project, virtualFile);
                }
                else {
                    String fileName = psiFile.getName();  // some artificial psi file, lets do best we can
                    highlighter = HighlighterFactory.createHighlighter(project, fileName);
                }

                ((EditorEx) myEditor).setHighlighter(highlighter);

                if (myElements.length > 1) {
                    myFileChooser.setVisible(true);
                    myCountLabel.setVisible(true);
                    myLabel.setVisible(false);

                    myFileChooser.setModel(new DefaultComboBoxModel<>(fileDescriptors.toArray(new FileDescriptor[fileDescriptors.size()])));
                    updateRenderer(project);
                }
                else {
                    myFileChooser.setVisible(false);
                    myCountLabel.setVisible(false);

                    VirtualFile file = psiFile.getVirtualFile();
                    if (file != null) {
                        myLabel.setIcon(getIconForFile(psiFile));
                        myLabel.setForeground(TargetAWT.to(FileStatusManager.getInstance(project).getStatus(file).getColor()));
                        myLabel.setText(file.getPresentableName());
                        myLabel.setBorder(new CompoundBorder(
                            IdeBorderFactory.createRoundedBorder(),
                            IdeBorderFactory.createEmptyBorder(0, 0, 0, 5)
                        ));
                        myLabel.setVisible(true);
                    }
                }

                updateControls();

                revalidate();
                repaint();

                return true;
            }
        );
    }

    @RequiredUIAccess
    @SuppressWarnings("ReturnValueIgnored") // ignored result of fun call
    private static void update(
        @Nonnull PsiElement[] elements,
        @RequiredUIAccess @Nonnull BiFunction<PsiElement[], List<FileDescriptor>, Boolean> fun
    ) {
        List<PsiElement> candidates = new ArrayList<>(elements.length);
        List<FileDescriptor> files = new ArrayList<>(elements.length);
        Set<String> names = new HashSet<>();
        for (PsiElement element : elements) {
            if (element instanceof PsiNamedElement namedElement) {
                names.add(namedElement.getName());
            }
        }
        for (PsiElement element : elements) {
            PsiFile file = getContainingFile(element);
            if (file == null) {
                continue;
            }
            PsiElement parent = element.getParent();
            files.add(new FileDescriptor(file, names.size() > 1 || parent == file ? element : parent));
            candidates.add(element);
        }

        fun.apply(PsiUtilCore.toPsiElementArray(candidates), files);
    }

    @RequiredReadAction
    private static Icon getIconForFile(PsiFile psiFile) {
        return TargetAWT.to(IconDescriptorUpdaters.getIcon(psiFile.getNavigationElement(), 0));
    }

    public JComponent getPreferredFocusableComponent() {
        return myElements.length > 1 ? myFileChooser : myEditor.getContentComponent();
    }

    @RequiredUIAccess
    private void updateControls() {
        updateLabels();
        updateCombo();
        updateEditorText();
        myToolbar.updateActionsAsync();
    }

    private void updateCombo() {
        if (myFileChooser != null && myFileChooser.isVisible()) {
            myFileChooser.setSelectedIndex(myIndex);
        }
    }

    @RequiredUIAccess
    private void updateEditorText() {
        disposeNonTextEditor();

        PsiElement elt = myElements[myIndex].getNavigationElement();
        Project project = elt.getProject();
        PsiFile psiFile = getContainingFile(elt);
        VirtualFile vFile = psiFile.getVirtualFile();
        if (vFile == null) {
            return;
        }
        FileEditorProvider[] providers = FileEditorProviderManager.getInstance().getProviders(project, vFile);
        for (FileEditorProvider provider : providers) {
            if (provider instanceof TextEditorProvider) {
                updateTextElement(elt);
                myBinarySwitch.show(myViewingPanel, TEXT_PAGE_KEY);
                break;
            }
            else if (provider.accept(project, vFile)) {
                myCurrentNonTextEditorProvider = provider;
                myNonTextEditor = myCurrentNonTextEditorProvider.createEditor(project, vFile);
                myBinaryPanel.removeAll();
                myBinaryPanel.add(myNonTextEditor.getComponent());
                myBinarySwitch.show(myViewingPanel, BINARY_PAGE_KEY);
                break;
            }
        }
    }

    private void disposeNonTextEditor() {
        if (myNonTextEditor != null) {
            myCurrentNonTextEditorProvider.disposeEditor(myNonTextEditor);
            myNonTextEditor = null;
            myCurrentNonTextEditorProvider = null;
        }
    }

    @RequiredUIAccess
    private void updateTextElement(PsiElement elt) {
        String newText = getNewText(elt);
        if (newText == null || Comparing.strEqual(newText, myEditor.getDocument().getText())) {
            return;
        }
        CommandProcessor.getInstance().runUndoTransparentAction(() -> Application.get().runWriteAction(() -> {
            Document fragmentDoc = myEditor.getDocument();
            fragmentDoc.setReadOnly(false);

            fragmentDoc.replaceString(0, fragmentDoc.getTextLength(), newText);
            fragmentDoc.setReadOnly(true);
            myEditor.getCaretModel().moveToOffset(0);
            myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        }));
    }


    @Nullable
    @RequiredReadAction
    public static String getNewText(PsiElement elt) {
        Project project = elt.getProject();
        PsiFile psiFile = getContainingFile(elt);

        Document doc = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (doc == null) {
            return null;
        }

        if (elt.getTextRange() == TextRange.EMPTY_RANGE) {
            return null;
        }

        ImplementationTextSelectioner implementationTextSelectioner = ImplementationTextSelectioner.forLanguage(elt.getLanguage());
        int start = implementationTextSelectioner.getTextStartOffset(elt);
        int end = implementationTextSelectioner.getTextEndOffset(elt);

        int lineStart = doc.getLineStartOffset(doc.getLineNumber(start));
        int lineEnd = end < doc.getTextLength() ? doc.getLineEndOffset(doc.getLineNumber(end)) : doc.getTextLength();
        return doc.getCharsSequence().subSequence(lineStart, lineEnd).toString();
    }

    private static PsiFile getContainingFile(PsiElement elt) {
        PsiFile psiFile = elt.getContainingFile();
        if (psiFile == null) {
            return null;
        }
        return psiFile.getOriginalFile();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (!ScreenUtil.isStandardAddRemoveNotify(this)) {
            return;
        }
        EditorFactory.getInstance().releaseEditor(myEditor);
        disposeNonTextEditor();
    }

    private void updateLabels() {
        //TODO: Move from JavaDoc to somewhere more appropriate place.
        ElementLocationUtil.customizeElementLabel(myElements[myIndex], myLocationLabel);
        //noinspection AutoBoxing
        myCountLabel.setText(CodeInsightLocalize.nOfM(myIndex + 1, myElements.length).get());
    }

    private ActionToolbar createToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();

        BackAction back = new BackAction();
        back.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)), this);
        group.add(back);

        ForwardAction forward = new ForwardAction();
        forward.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)), this);
        group.add(forward);

        EditSourceActionBase edit = new EditSourceAction();
        edit.registerCustomShortcutSet(new CompositeShortcutSet(CommonShortcuts.getEditSource(), CommonShortcuts.ENTER), this);
        group.add(edit);

        edit = new ShowSourceAction();
        edit.registerCustomShortcutSet(new CompositeShortcutSet(CommonShortcuts.getViewSource(), CommonShortcuts.CTRL_ENTER), this);
        group.add(edit);

        return ActionManager.getInstance().createActionToolbar("ImplementationViewToolbar", group, true);
    }

    @RequiredUIAccess
    private void goBack() {
        myIndex--;
        updateControls();
    }

    @RequiredUIAccess
    private void goForward() {
        myIndex++;
        updateControls();
    }

    public int getIndex() {
        return myIndex;
    }

    public PsiElement[] getElements() {
        return myElements;
    }

    @RequiredReadAction
    public UsageView showInUsageView() {
        return FindUtil.showInUsageView(null, collectNonBinaryElements(), myTitle, myEditor.getProject());
    }

    private class BackAction extends AnAction implements HintManagerImpl.ActionToIgnore {
        public BackAction() {
            super(CodeInsightLocalize.quickDefinitionBack(), LocalizeValue.empty(), PlatformIconGroup.actionsBack());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            goBack();
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setEnabled(myIndex > 0);
        }
    }

    private class ForwardAction extends AnAction implements HintManagerImpl.ActionToIgnore {
        public ForwardAction() {
            super(CodeInsightLocalize.quickDefinitionForward(), LocalizeValue.empty(), PlatformIconGroup.actionsForward());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            goForward();
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setEnabled(myElements != null && myIndex < myElements.length - 1);
        }
    }

    private class EditSourceAction extends EditSourceActionBase {
        public EditSourceAction() {
            super(true, PlatformIconGroup.actionsEditsource(), CodeInsightLocalize.quickDefinitionEditSource());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            super.actionPerformed(e);
            if (myHint.isVisible()) {
                myHint.cancel();
            }
        }
    }

    private class ShowSourceAction extends EditSourceActionBase implements HintManagerImpl.ActionToIgnore {
        public ShowSourceAction() {
            super(false, PlatformIconGroup.actionsPreview(), CodeInsightLocalize.quickDefinitionShowSource());
        }
    }

    private class EditSourceActionBase extends AnAction {
        private final boolean myFocusEditor;

        public EditSourceActionBase(boolean focusEditor, Image icon, LocalizeValue text) {
            super(text, LocalizeValue.empty(), icon);
            myFocusEditor = focusEditor;
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(myFileChooser == null || !myFileChooser.isPopupVisible());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            PsiElement element = myElements[myIndex];
            PsiElement navigationElement = element.getNavigationElement();
            PsiFile file = getContainingFile(navigationElement);
            if (file == null) {
                return;
            }
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile == null) {
                return;
            }
            Project project = element.getProject();
            FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
            OpenFileDescriptorImpl descriptor = new OpenFileDescriptorImpl(project, virtualFile, navigationElement.getTextOffset());
            fileEditorManager.openTextEditor(descriptor, myFocusEditor);
        }
    }

    private PsiElement[] collectNonBinaryElements() {
        List<PsiElement> result = new ArrayList<>();
        for (PsiElement element : myElements) {
            if (!(element instanceof PsiBinaryFile)) {
                result.add(element);
            }
        }
        return PsiUtilCore.toPsiElementArray(result);
    }
}
