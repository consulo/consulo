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

package consulo.ide.impl.idea.codeInspection.ui;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInspection.ex.GlobalInspectionContextImpl;
import consulo.ide.impl.idea.codeInspection.ex.QuickFixAction;
import consulo.ide.impl.idea.codeInspection.ui.actions.SuppressActionWrapper;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProjectProfileManagerImpl;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.scheme.InspectionProfileWrapper;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.Lists;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class Browser extends JPanel {
    private final List<ClickListener> myClickListeners = Lists.newLockFreeCopyOnWriteList();
    private RefEntity myCurrentEntity;
    private JEditorPane myHTMLViewer;
    private final InspectionResultsView myView;
    @RequiredUIAccess
    private final HyperlinkListener myHyperLinkListener;
    private CommonProblemDescriptor myCurrentDescriptor;

    public static class ClickEvent {
        public static final int REF_ELEMENT = 1;
        public static final int FILE_OFFSET = 2;
        private final VirtualFile myFile;
        private final int myStartPosition;
        private final int myEndPosition;
        private final RefElement refElement;
        private final int myEventType;

        public ClickEvent(VirtualFile myFile, int myStartPosition, int myEndPosition) {
            this.myFile = myFile;
            this.myStartPosition = myStartPosition;
            this.myEndPosition = myEndPosition;
            myEventType = FILE_OFFSET;
            refElement = null;
        }

        public int getEventType() {
            return myEventType;
        }

        public VirtualFile getFile() {
            return myFile;
        }

        public int getStartOffset() {
            return myStartPosition;
        }

        public int getEndOffset() {
            return myEndPosition;
        }

        public RefElement getClickedElement() {
            return refElement;
        }
    }

    public void dispose() {
        removeAll();
        if (myHTMLViewer != null) {
            myHTMLViewer.removeHyperlinkListener(myHyperLinkListener);
            myHTMLViewer = null;
        }
        myClickListeners.clear();
    }

    public interface ClickListener {
        void referenceClicked(ClickEvent e);
    }

    @RequiredReadAction
    private void showPageFromHistory(@Nonnull RefEntity newEntity) {
        InspectionToolWrapper toolWrapper = getToolWrapper(newEntity);
        try {
            String html = generateHTML(newEntity, toolWrapper);
            myHTMLViewer.read(new StringReader(html), null);
            setupStyle();
            myHTMLViewer.setCaretPosition(0);
        }
        catch (Exception e) {
            showEmpty();
        }
        finally {
            myCurrentEntity = newEntity;
            myCurrentDescriptor = null;
        }
    }

    @RequiredReadAction
    public void showPageFor(RefEntity refEntity, CommonProblemDescriptor descriptor) {
        try {
            String html = generateHTML(refEntity, descriptor);
            myHTMLViewer.read(new StringReader(html), null);
            setupStyle();
            myHTMLViewer.setCaretPosition(0);
        }
        catch (Exception e) {
            showEmpty();
        }
        finally {
            myCurrentEntity = refEntity;
            myCurrentDescriptor = descriptor;
        }
    }

    @RequiredReadAction
    public void showPageFor(RefEntity newEntity) {
        if (newEntity == null) {
            showEmpty();
            return;
        }
        //multiple problems for one entity -> refresh browser
        showPageFromHistory(newEntity.getRefManager().getRefinedElement(newEntity));
    }

    public Browser(@Nonnull InspectionResultsView view) {
        super(new BorderLayout());
        myView = view;

        myCurrentEntity = null;
        myCurrentDescriptor = null;

        myHTMLViewer = new JEditorPane(UIUtil.HTML_MIME, InspectionLocalize.inspectionOfflineViewEmptyBrowserText().get());
        myHTMLViewer.setEditable(false);
        myHyperLinkListener = Browser.this::hyperlinkUpdate;
        myHTMLViewer.addHyperlinkListener(myHyperLinkListener);

        JScrollPane pane = ScrollPaneFactory.createScrollPane(myHTMLViewer);
        pane.setBorder(null);
        add(pane, BorderLayout.CENTER);
        setupStyle();
    }

    @RequiredUIAccess
    private void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
            return;
        }
        JEditorPane pane = (JEditorPane) e.getSource();
        if (e instanceof HTMLFrameHyperlinkEvent) {
            HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
            HTMLDocument doc = (HTMLDocument) pane.getDocument();
            doc.processHTMLFrameHyperlinkEvent(evt);
            return;
        }
        try {
            URL url = e.getURL();
            String ref = url.getRef();
            if (ref.startsWith("pos:")) {
                int delimiterPos = ref.indexOf(':', "pos:".length() + 1);
                String startPosition = ref.substring("pos:".length(), delimiterPos);
                String endPosition = ref.substring(delimiterPos + 1);
                int textStartOffset = Integer.parseInt(startPosition);
                int textEndOffset = Integer.parseInt(endPosition);
                String fileURL = url.toExternalForm();
                fileURL = fileURL.substring(0, fileURL.indexOf('#'));
                VirtualFile vFile = VirtualFileManager.getInstance().findFileByUrl(fileURL);
                if (vFile != null) {
                    fireClickEvent(vFile, textStartOffset, textEndOffset);
                }
            }
            else if (ref.startsWith("descr:")) {
                if (myCurrentDescriptor instanceof ProblemDescriptor problemDescriptor) {
                    PsiElement psiElement = problemDescriptor.getPsiElement();
                    if (psiElement == null) {
                        return;
                    }
                    VirtualFile vFile = psiElement.getContainingFile().getVirtualFile();
                    if (vFile != null) {
                        TextRange range = ((ProblemDescriptorBase) myCurrentDescriptor).getTextRange();
                        fireClickEvent(vFile, range.getStartOffset(), range.getEndOffset());
                    }
                }
            }
            else if (ref.startsWith("invoke:")) {
                int actionNumber = Integer.parseInt(ref.substring("invoke:".length()));
                InspectionToolWrapper toolWrapper = getToolWrapper();
                InspectionToolPresentation presentation = myView.getGlobalInspectionContext().getPresentation(toolWrapper);
                QuickFixAction fixAction = presentation.getQuickFixes(new RefElement[]{(RefElement) myCurrentEntity})[actionNumber];
                fixAction.doApplyFix(new RefElement[]{(RefElement) myCurrentEntity}, myView);
            }
            else if (ref.startsWith("invokelocal:")) {
                int actionNumber = Integer.parseInt(ref.substring("invokelocal:".length()));
                if (actionNumber > -1) {
                    invokeLocalFix(actionNumber);
                }
            }
            else if (ref.startsWith("suppress:")) {
                SuppressActionWrapper.SuppressTreeAction[] suppressTreeActions =
                    new SuppressActionWrapper(myView.getProject(), getToolWrapper(), myView.getTree().getSelectionPaths())
                        .getChildren(null);
                List<AnAction> activeActions = new ArrayList<>();
                for (SuppressActionWrapper.SuppressTreeAction suppressTreeAction : suppressTreeActions) {
                    if (suppressTreeAction.isAvailable()) {
                        activeActions.add(suppressTreeAction);
                    }
                }
                if (!activeActions.isEmpty()) {
                    int actionNumber = Integer.parseInt(ref.substring("suppress:".length()));
                    if (actionNumber > -1 && activeActions.size() > actionNumber) {
                        activeActions.get(actionNumber).actionPerformed(null);
                    }
                }
            }
            else {
                int offset = Integer.parseInt(ref);
                String fileURL = url.toExternalForm();
                fileURL = fileURL.substring(0, fileURL.indexOf('#'));
                VirtualFile vFile = VirtualFileManager.getInstance().findFileByUrl(fileURL);
                if (vFile == null) {
                    vFile = VfsUtil.findFileByURL(url);
                }
                if (vFile != null) {
                    fireClickEvent(vFile, offset, offset);
                }
            }
        }
        catch (Throwable t) {
            //???
        }
    }

    private void setupStyle() {
        Document document = myHTMLViewer.getDocument();
        if (!(document instanceof StyledDocument)) {
            return;
        }

        StyledDocument styledDocument = (StyledDocument) document;

        EditorColorsManager colorsManager = EditorColorsManager.getInstance();
        EditorColorsScheme scheme = colorsManager.getGlobalScheme();

        Style style = styledDocument.addStyle("active", null);
        StyleConstants.setFontFamily(style, scheme.getEditorFontName());
        StyleConstants.setFontSize(style, scheme.getEditorFontSize());
        styledDocument.setCharacterAttributes(0, document.getLength(), style, false);
    }

    public void addClickListener(ClickListener listener) {
        myClickListeners.add(listener);
    }

    private void fireClickEvent(VirtualFile file, int startPosition, int endPosition) {
        ClickEvent e = new ClickEvent(file, startPosition, endPosition);

        for (ClickListener listener : myClickListeners) {
            listener.referenceClicked(e);
        }
    }

    @RequiredReadAction
    private String generateHTML(RefEntity refEntity, @Nonnull InspectionToolWrapper toolWrapper) {
        StringBuffer buf = new StringBuffer();
        HTMLComposerBase htmlComposer = getPresentation(toolWrapper).getComposer();
        if (refEntity instanceof RefElement) {
            Application.get().runReadAction(() -> htmlComposer.compose(buf, refEntity));
        }
        else {
            htmlComposer.compose(buf, refEntity);
        }

        uppercaseFirstLetter(buf);

        if (refEntity instanceof RefElement) {
            appendSuppressSection(buf);
        }

        insertHeaderFooter(buf);

        return buf.toString();
    }

    private InspectionToolPresentation getPresentation(@Nonnull InspectionToolWrapper toolWrapper) {
        return myView.getGlobalInspectionContext().getPresentation(toolWrapper);
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    private static void insertHeaderFooter(StringBuffer buf) {
        buf.insert(0, "<HTML><BODY>");
        buf.append("</BODY></HTML>");
    }

    @RequiredReadAction
    private String generateHTML(RefEntity refEntity, CommonProblemDescriptor descriptor) {
        StringBuffer buf = new StringBuffer();
        Runnable action = () -> {
            InspectionToolWrapper toolWrapper = getToolWrapper(refEntity);
            getPresentation(toolWrapper).getComposer().compose(buf, refEntity, descriptor);
        };
        Application.get().runReadAction(action);

        uppercaseFirstLetter(buf);

        if (refEntity instanceof RefElement) {
            appendSuppressSection(buf);
        }

        insertHeaderFooter(buf);
        return buf.toString();
    }

    private InspectionToolWrapper getToolWrapper(RefEntity refEntity) {
        InspectionToolWrapper toolWrapper = getToolWrapper();
        assert toolWrapper != null;
        GlobalInspectionContextImpl context = myView.getGlobalInspectionContext();
        if (refEntity instanceof RefElement refElem) {
            PsiElement element = refElem.getPsiElement();
            if (element == null) {
                return toolWrapper;
            }
            InspectionProfileWrapper profileWrapper =
                InspectionProjectProfileManagerImpl.getInstanceImpl(context.getProject()).getProfileWrapper();
            toolWrapper = profileWrapper.getInspectionTool(toolWrapper.getShortName(), element);
        }
        return toolWrapper;
    }

    @RequiredReadAction
    private void appendSuppressSection(StringBuffer buf) {
        InspectionToolWrapper toolWrapper = getToolWrapper();
        if (toolWrapper != null) {
            HighlightDisplayKey key = toolWrapper.getHighlightDisplayKey();
            SuppressActionWrapper.SuppressTreeAction[] suppressActions =
                new SuppressActionWrapper(myView.getProject(), toolWrapper, myView.getTree().getSelectionPaths()).getChildren(null);
            if (suppressActions.length > 0) {
                List<AnAction> activeSuppressActions = new ArrayList<>();
                for (SuppressActionWrapper.SuppressTreeAction suppressAction : suppressActions) {
                    if (suppressAction.isAvailable()) {
                        activeSuppressActions.add(suppressAction);
                    }
                }
                if (!activeSuppressActions.isEmpty()) {
                    int idx = 0;
                    String br = "<br>";
                    buf.append(br);
                    HTMLComposerBase.appendHeading(buf, InspectionLocalize.inspectionExportResultsSuppress());
                    for (AnAction suppressAction : activeSuppressActions) {
                        buf.append(br);
                        if (idx == activeSuppressActions.size() - 1) {
                            buf.append(br);
                        }
                        HTMLComposer.appendAfterHeaderIndention(buf);
                        String href = "<a HREF=\"file://bred.txt#suppress:" + idx + "\">" +
                            suppressAction.getTemplatePresentation().getText() + "</a>";
                        buf.append(href);
                        idx++;
                    }
                }
            }
        }
    }

    private static void uppercaseFirstLetter(StringBuffer buf) {
        if (buf.length() > 1) {
            char[] firstLetter = new char[1];
            buf.getChars(0, 1, firstLetter, 0);
            buf.setCharAt(0, Character.toUpperCase(firstLetter[0]));
        }
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public void showEmpty() {
        myCurrentEntity = null;
        try {
            myHTMLViewer.read(new StringReader("<html><body></body></html>"), null);
        }
        catch (IOException e) {
            //can't be
        }
    }

    public void showDescription(@Nonnull InspectionToolWrapper toolWrapper) {
        if (toolWrapper.getShortName().isEmpty()) {
            showEmpty();
            return;
        }
        StringBuffer page = new StringBuffer();
        page.append("<table border='0' cellspacing='0' cellpadding='0' width='100%'>");
        page.append("<tr><td colspan='2'>");
        HTMLComposer.appendHeading(page, InspectionLocalize.inspectionToolInBrowserIdTitle());
        page.append("</td></tr>");
        page.append("<tr><td width='37'></td><td>");
        page.append(toolWrapper.getShortName());
        page.append("</td></tr>");
        page.append("<tr height='10'></tr>");
        page.append("<tr><td colspan='2'>");
        HTMLComposer.appendHeading(page, InspectionLocalize.inspectionToolInBrowserDescriptionTitle());
        page.append("</td></tr>");
        page.append("<tr><td width='37'></td><td>");
        String underConstruction =
            "<b>" + InspectionLocalize.inspectionToolDescriptionUnderConstructionText() + "</b></html>";
        try {
            String description = toolWrapper.loadDescription();
            if (description == null) {
                description = underConstruction;
            }
            page.append(UIUtil.getHtmlBody(description));

            page.append("</td></tr></table>");
            myHTMLViewer.setText(XmlStringUtil.wrapInHtml(page));
            setupStyle();
        }
        finally {
            myCurrentEntity = null;
        }
    }

    @Nullable
    private InspectionToolWrapper getToolWrapper() {
        return myView.getTree().getSelectedToolWrapper();
    }

    @RequiredUIAccess
    public void invokeLocalFix(int idx) {
        if (myView.getTree().getSelectionCount() != 1) {
            return;
        }
        InspectionTreeNode node = (InspectionTreeNode) myView.getTree().getSelectionPath().getLastPathComponent();
        if (node instanceof ProblemDescriptionNode problemNode) {
            CommonProblemDescriptor descriptor = problemNode.getDescriptor();
            RefEntity element = problemNode.getElement();
            invokeFix(element, descriptor, idx);
        }
        else if (node instanceof RefElementNode elementNode) {
            RefEntity element = elementNode.getElement();
            CommonProblemDescriptor descriptor = elementNode.getProblem();
            if (descriptor != null) {
                invokeFix(element, descriptor, idx);
            }
        }
    }

    @RequiredUIAccess
    private void invokeFix(RefEntity element, CommonProblemDescriptor descriptor, int idx) {
        QuickFix[] fixes = descriptor.getFixes();
        if (fixes != null && fixes.length > idx && fixes[idx] != null) {
            if (element instanceof RefElement refElem) {
                PsiElement psiElement = refElem.getPsiElement();
                if (psiElement != null && psiElement.isValid()) {
                    if (!FileModificationService.getInstance().preparePsiElementForWrite(psiElement)) {
                        return;
                    }
                    performFix(element, descriptor, idx, fixes[idx]);
                }
            }
            else {
                performFix(element, descriptor, idx, fixes[idx]);
            }
        }
    }

    @RequiredUIAccess
    private void performFix(RefEntity element, CommonProblemDescriptor descriptor, int idx, QuickFix fix) {
        CommandProcessor.getInstance().newCommand()
            .project(myView.getProject())
            .name(fix.getName())
            .inWriteAction()
            .inGlobalUndoAction()
            .run(() -> {
                PsiModificationTracker tracker = PsiManager.getInstance(myView.getProject()).getModificationTracker();
                long startCount = tracker.getModificationCount();
                //CCE here means QuickFix was incorrectly inherited
                fix.applyFix(myView.getProject(), descriptor);
                if (startCount != tracker.getModificationCount()) {
                    InspectionToolWrapper toolWrapper = myView.getTree().getSelectedToolWrapper();
                    if (toolWrapper != null) {
                        InspectionToolPresentation presentation =
                            myView.getGlobalInspectionContext().getPresentation(toolWrapper);
                        presentation.ignoreProblem(element, descriptor, idx);
                    }
                    myView.updateView(false);
                }
            });
    }
}
