// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
// Copyright 2013-2026 consulo.io
package consulo.ide.impl.idea.codeInsight.folding.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.internal.FoldingUtil;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.fileEditor.text.CodeFoldingState;
import consulo.language.ast.ASTNode;
import consulo.language.editor.folding.FoldingBuilder;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.language.editor.folding.LanguageFolding;
import consulo.language.psi.PsiCompiledFile;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.*;

class DocumentFoldingInfo implements CodeFoldingState {
    private static final Logger LOG = Logger.getInstance(DocumentFoldingInfo.class);
    private static final Key<FoldingInfo> FOLDING_INFO_KEY = Key.create("FOLDING_INFO");

    private final Project myProject;
    private final VirtualFile myFile;

    private final List<SignatureInfo> myInfos = Lists.newLockFreeCopyOnWriteList();
    private final List<RangeMarker> myRangeMarkers = Lists.newLockFreeCopyOnWriteList();

    /**
     * null means {@link #computeExpandRanges()} was not called yet
     */
    private volatile @Nullable List<FoldingInfo> myComputedInfos;

    private static final String DEFAULT_PLACEHOLDER = "...";
    private static final String ELEMENT_TAG = "element";
    private static final String SIGNATURE_ATT = "signature";
    private static final String EXPANDED_ATT = "expanded";
    private static final String MARKER_TAG = "marker";
    private static final String DATE_ATT = "date";
    private static final String PLACEHOLDER_ATT = "ph";

    DocumentFoldingInfo(Project project, Document document) {
        myProject = project;
        myFile = FileDocumentManager.getInstance().getFile(document);
    }

    @RequiredUIAccess
    void loadFromEditor(Editor editor) {
        UIAccess.assertIsUIThread();
        LOG.assertTrue(!editor.isDisposed());
        clear();

        int caretOffset = editor.getCaretModel().getOffset();
        FoldRegion[] foldRegions = editor.getFoldingModel().getAllFoldRegions();
        List<FoldingInfo> computedInfos = new ArrayList<>(foldRegions.length);
        for (FoldRegion region : foldRegions) {
            if (!region.isValid() || region.shouldNeverExpand()) {
                continue;
            }
            boolean expanded = region.isExpanded();
            String signature = region.getUserData(UpdateFoldRegionsOperation.SIGNATURE);
            if (signature == UpdateFoldRegionsOperation.NO_SIGNATURE) {
                continue;
            }
            Boolean storedCollapseByDefault = region.getUserData(UpdateFoldRegionsOperation.COLLAPSED_BY_DEFAULT);
            TextRange regionRange = TextRange.create(region);
            boolean caretInsideRange = regionRange.contains(caretOffset) && regionRange.getStartOffset() != caretOffset;
            boolean collapseByDefault = storedCollapseByDefault != null && storedCollapseByDefault && !caretInsideRange;
            if (collapseByDefault == expanded || signature == null) {
                if (signature != null) {
                    myInfos.add(new SignatureInfo(signature, expanded));
                }
                else {
                    addRangeMarker(editor.getDocument(), regionRange, expanded, region.getPlaceholderText());
                }
            }
            computedInfos.add(new FoldingInfo(region.getPlaceholderText(), regionRange, expanded));
        }
        myComputedInfos = Collections.unmodifiableList(computedInfos);
    }

    private void addRangeMarker(Document document, TextRange range, boolean expanded, String placeholderText) {
        RangeMarker marker = document.createRangeMarker(range);
        myRangeMarkers.add(marker);
        marker.putUserData(FOLDING_INFO_KEY, new FoldingInfo(placeholderText, range, expanded));
    }

    /**
     * Resolves signature-based infos to concrete {@link TextRange} instances using PSI.
     * Must be called under a read lock, preferably on a background thread,
     * before {@link #setToEditor(Editor)} or {@link #applyFoldingExpandedState(Editor)}.
     */
    @RequiredReadAction
    void computeExpandRanges() {
        PsiManager psiManager;
        PsiFile psiFile;
        Document document;
        if (myProject.isDisposed()
            || (psiManager = PsiManager.getInstance(myProject)).isDisposed()
            || !myFile.isValid()
            || (psiFile = psiManager.findFile(myFile)) == null
            || !(psiFile = psiFile instanceof PsiCompiledFile compiled ? compiled.getDecompiledPsiFile() : psiFile).isValid()
            || (document = PsiDocumentManager.getInstance(myProject).getDocument(psiFile)) == null) {
            myComputedInfos = Collections.emptyList();
            return;
        }
        List<FoldingInfo> result = new ArrayList<>(myRangeMarkers.size() + myInfos.size());
        Map<PsiElement, FoldingDescriptor> ranges = null;
        for (SignatureInfo info : myInfos) {
            PsiElement element = FoldingPolicy.restoreBySignature(psiFile, info.signature());
            if (element == null || !element.isValid()) {
                continue;
            }
            if (ranges == null) {
                ranges = buildRanges(psiFile, document);
            }
            FoldingDescriptor descriptor = ranges.get(element);
            if (descriptor == null) {
                continue;
            }
            TextRange range = descriptor.getRange();
            result.add(new FoldingInfo(ObjectUtil.notNull(descriptor.getPlaceholderText(), DEFAULT_PLACEHOLDER), range, info.expanded()));
        }
        for (RangeMarker marker : myRangeMarkers) {
            if (!marker.isValid() || marker.getStartOffset() == marker.getEndOffset()) {
                continue;
            }
            FoldingInfo info = marker.getUserData(FOLDING_INFO_KEY);
            if (info != null) {
                result.add(info);
            }
        }
        myComputedInfos = Collections.unmodifiableList(result);
    }

    @Override
    @RequiredUIAccess
    public void setToEditor(Editor editor) {
        UIAccess.assertIsUIThread();

        List<FoldingInfo> computedInfos = myComputedInfos;
        if (computedInfos != null) {
            // Fast path: ranges were pre-computed by loadFromEditor or computeExpandRanges.
            for (RangeMarker marker : myRangeMarkers) {
                if (!marker.isValid() || marker.getStartOffset() == marker.getEndOffset()) {
                    continue;
                }
                FoldRegion region = FoldingUtil.findFoldRegion(editor, marker.getStartOffset(), marker.getEndOffset());
                FoldingInfo info = marker.getUserData(FOLDING_INFO_KEY);
                if (region == null) {
                    if (info != null) {
                        region = editor.getFoldingModel().addFoldRegion(marker.getStartOffset(), marker.getEndOffset(), info.placeHolder());
                    }
                    if (region == null) {
                        continue;  // skip invalid marker; don't abort signature-based application
                    }
                }
                expandRegionBlessForNewLife(editor, region, info != null && info.expanded());
            }
            applyFoldingExpandedState(editor);
            return;
        }

        // Fallback path: myComputedInfos not pre-populated (e.g. after readExternal).
        // Resolve signatures and apply ranges inline — preserves old setToEditor behavior.
        PsiManager psiManager = PsiManager.getInstance(myProject);
        if (psiManager.isDisposed() || !myFile.isValid()) {
            return;
        }
        PsiFile psiFile = psiManager.findFile(myFile);
        if (psiFile == null) {
            return;
        }

        Map<PsiElement, FoldingDescriptor> ranges = null;
        for (SignatureInfo info : myInfos) {
            PsiElement element = FoldingPolicy.restoreBySignature(psiFile, info.signature());
            if (element == null || !element.isValid()) {
                continue;
            }
            if (ranges == null) {
                ranges = buildRanges(psiFile, editor.getDocument());
            }
            FoldingDescriptor descriptor = ranges.get(element);
            if (descriptor == null) {
                continue;
            }
            TextRange range = descriptor.getRange();
            FoldRegion region = FoldingUtil.findFoldRegion(editor, range.getStartOffset(), range.getEndOffset());
            if (region != null) {
                expandRegionBlessForNewLife(editor, region, info.expanded());
            }
        }
        for (RangeMarker marker : myRangeMarkers) {
            if (!marker.isValid() || marker.getStartOffset() == marker.getEndOffset()) {
                continue;
            }
            FoldRegion region = FoldingUtil.findFoldRegion(editor, marker.getStartOffset(), marker.getEndOffset());
            FoldingInfo info = marker.getUserData(FOLDING_INFO_KEY);
            if (region == null) {
                if (info != null) {
                    region = editor.getFoldingModel().addFoldRegion(marker.getStartOffset(), marker.getEndOffset(), info.placeHolder());
                }
                if (region == null) {
                    return;
                }
            }
            expandRegionBlessForNewLife(editor, region, info != null && info.expanded());
        }
    }

    @RequiredUIAccess
    void applyFoldingExpandedState(Editor editor) {
        UIAccess.assertIsUIThread();
        assert !editor.isDisposed();

        List<FoldingInfo> computedInfos = myComputedInfos;
        if (computedInfos == null) {
            throw new IllegalStateException("Must call computeExpandRanges() before calling applyFoldingExpandedState()");
        }
        for (FoldingInfo foldingInfo : computedInfos) {
            TextRange range = foldingInfo.textRange();
            FoldRegion region = FoldingUtil.findFoldRegion(editor, range.getStartOffset(), range.getEndOffset());
            if (region != null) {
                expandRegionBlessForNewLife(editor, region, foldingInfo.expanded());
            }
        }
        myComputedInfos = Collections.emptyList();
    }

    private static void expandRegionBlessForNewLife(Editor editor, FoldRegion region, boolean expanded) {
        if (!CodeFoldingManagerImpl.isFoldingsInitializedInEditor(editor)) {
            int offset = editor.getCaretModel().getOffset();
            if (offset > region.getStartOffset() && offset < region.getEndOffset()) {
                expanded = true;
            }
        }
        region.setExpanded(expanded);
    }

    @RequiredReadAction
    private static Map<PsiElement, FoldingDescriptor> buildRanges(PsiFile psiFile, Document document) {
        FoldingBuilder foldingBuilder = FoldingBuilder.forLanguageComposite(psiFile.getLanguage());
        ASTNode node = psiFile.getNode();
        if (node == null) {
            return Collections.emptyMap();
        }
        FoldingDescriptor[] descriptors = LanguageFolding.buildFoldingDescriptors(foldingBuilder, psiFile, document, true);
        Map<PsiElement, FoldingDescriptor> ranges = new HashMap<>(descriptors.length);
        for (FoldingDescriptor descriptor : descriptors) {
            ASTNode ast = descriptor.getElement();
            PsiElement psi = ast.getPsi();
            if (psi != null) {
                ranges.put(psi, descriptor);
            }
        }
        return ranges;
    }

    void clear() {
        myInfos.clear();
        myComputedInfos = null;
        for (RangeMarker marker : myRangeMarkers) {
            marker.dispose();
        }
        myRangeMarkers.clear();
    }

    void writeExternal(Element element) {
        if (myInfos.isEmpty() && myRangeMarkers.isEmpty()) {
            return;
        }

        for (SignatureInfo info : myInfos) {
            Element e = new Element(ELEMENT_TAG);
            e.setAttribute(SIGNATURE_ATT, info.signature());
            if (info.expanded()) {
                e.setAttribute(EXPANDED_ATT, Boolean.toString(true));
            }
            element.addContent(e);
        }

        String date = null;
        for (RangeMarker marker : myRangeMarkers) {
            FoldingInfo fi = marker.getUserData(FOLDING_INFO_KEY);
            boolean state = fi != null && fi.expanded();

            Element e = new Element(MARKER_TAG);
            if (date == null) {
                date = getTimeStamp();
            }
            if (date.isEmpty()) {
                continue;
            }

            e.setAttribute(DATE_ATT, date);
            e.setAttribute(EXPANDED_ATT, Boolean.toString(state));
            String signature = marker.getStartOffset() + ":" + marker.getEndOffset();
            e.setAttribute(SIGNATURE_ATT, signature);
            String placeHolderText = fi == null ? DEFAULT_PLACEHOLDER : fi.placeHolder();
            e.setAttribute(PLACEHOLDER_ATT, XmlStringUtil.escapeIllegalXmlChars(placeHolderText));
            element.addContent(e);
        }
    }

    void readExternal(Element element) {
        ReadAction.run(() -> {
            clear();

            if (!myFile.isValid()) {
                return;
            }

            Document document = FileDocumentManager.getInstance().getDocument(myFile);
            if (document == null) {
                return;
            }

            String date = null;
            for (Element e : element.getChildren()) {
                String signature = e.getAttributeValue(SIGNATURE_ATT);
                if (signature == null) {
                    continue;
                }

                boolean expanded = Boolean.parseBoolean(e.getAttributeValue(EXPANDED_ATT));
                if (ELEMENT_TAG.equals(e.getName())) {
                    myInfos.add(new SignatureInfo(signature, expanded));
                }
                else if (MARKER_TAG.equals(e.getName())) {
                    if (date == null) {
                        date = getTimeStamp();
                    }
                    if (date.isEmpty()) {
                        continue;
                    }

                    if (!date.equals(e.getAttributeValue(DATE_ATT)) || FileDocumentManager.getInstance().isDocumentUnsaved(document)) {
                        continue;
                    }
                    StringTokenizer tokenizer = new StringTokenizer(signature, ":");
                    try {
                        int start = Integer.parseInt(tokenizer.nextToken());
                        int end = Integer.parseInt(tokenizer.nextToken());
                        if (start < 0 || end >= document.getTextLength() || start > end) {
                            continue;
                        }
                        String placeholderAttributeValue = e.getAttributeValue(PLACEHOLDER_ATT);
                        String placeHolderText = placeholderAttributeValue == null
                            ? DEFAULT_PLACEHOLDER
                            : XmlStringUtil.unescapeIllegalXmlChars(placeholderAttributeValue);
                        addRangeMarker(document, TextRange.create(start, end), expanded, placeHolderText);
                    }
                    catch (NoSuchElementException exc) {
                        LOG.error(exc);
                    }
                }
                else {
                    throw new IllegalStateException("unknown tag: " + e.getName());
                }
            }
        });
    }

    private String getTimeStamp() {
        if (!myFile.isValid()) {
            return "";
        }
        return Long.toString(myFile.getTimeStamp());
    }

    @Override
    public int hashCode() {
        int result = myProject.hashCode();
        result = 31 * result + (myFile != null ? myFile.hashCode() : 0);
        result = 31 * result + myInfos.hashCode();
        result = 31 * result + myRangeMarkers.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DocumentFoldingInfo info = (DocumentFoldingInfo) o;

        if (myFile != null ? !myFile.equals(info.myFile) : info.myFile != null) {
            return false;
        }
        if (!myProject.equals(info.myProject) || !myInfos.equals(info.myInfos)) {
            return false;
        }

        if (myRangeMarkers.size() != info.myRangeMarkers.size()) {
            return false;
        }
        for (int i = 0; i < myRangeMarkers.size(); i++) {
            RangeMarker marker = myRangeMarkers.get(i);
            RangeMarker other = info.myRangeMarkers.get(i);
            if (marker == other || !marker.isValid() || !other.isValid()) {
                continue;
            }
            if (!TextRange.areSegmentsEqual(marker, other)) {
                return false;
            }

            FoldingInfo fi = marker.getUserData(FOLDING_INFO_KEY);
            FoldingInfo ofi = other.getUserData(FOLDING_INFO_KEY);
            if (!Objects.equals(fi, ofi)) {
                return false;
            }
        }
        return true;
    }

    private record SignatureInfo(String signature, boolean expanded) {
    }

    private record FoldingInfo(String placeHolder, TextRange textRange, boolean expanded) {
    }
}
