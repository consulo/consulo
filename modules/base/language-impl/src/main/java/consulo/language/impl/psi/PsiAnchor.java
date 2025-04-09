// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.impl.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.impl.file.FreeThreadedFileViewProvider;
import consulo.language.impl.internal.psi.WrappedElementAnchor;
import consulo.language.impl.internal.psi.pointer.IdentikitImpl;
import consulo.language.impl.internal.psi.pointer.SelfElementInfo;
import consulo.language.psi.*;
import consulo.language.psi.stub.*;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.Set;

/**
 * @author db
 */
public abstract class PsiAnchor {
    private static final Logger LOG = Logger.getInstance(PsiAnchor.class);

    @Nullable
    public abstract PsiElement retrieve();

    public abstract PsiFile getFile();

    public abstract int getStartOffset();

    public abstract int getEndOffset();

    @Nonnull
    public static PsiAnchor create(@Nonnull PsiElement element) {
        PsiUtilCore.ensureValid(element);

        PsiAnchor anchor = doCreateAnchor(element);
        if (Application.get().isUnitTestMode()) {
            PsiElement restored = anchor.retrieve();
            if (!element.equals(restored)) {
                LOG.error("Cannot restore element " + element + " of " + element.getClass() + " from anchor " + anchor + ", getting " + restored + " instead");
            }
        }
        return anchor;
    }

    @Nonnull
    @RequiredReadAction
    private static PsiAnchor doCreateAnchor(@Nonnull PsiElement element) {
        if (element instanceof PsiFile file) {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
                return new PsiFileReference(virtualFile, file);
            }
            return new HardReference(file);
        }
        if (element instanceof PsiDirectory directory) {
            VirtualFile virtualFile = directory.getVirtualFile();
            return new PsiDirectoryReference(virtualFile, element.getProject());
        }

        PsiFile file = element.getContainingFile();
        if (file == null) {
            return new HardReference(element);
        }
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null || virtualFile instanceof VirtualFileWindow) {
            return new HardReference(element);
        }

        PsiAnchor stubRef = createStubReference(element, file);
        if (stubRef != null) {
            return stubRef;
        }

        if (!element.isPhysical()) {
            return wrapperOrHardReference(element);
        }

        TextRange textRange = element.getTextRange();
        if (textRange == null) {
            return wrapperOrHardReference(element);
        }

        Language lang = null;
        FileViewProvider viewProvider = file.getViewProvider();
        for (Language l : viewProvider.getLanguages()) {
            if (viewProvider.getPsi(l) == file) {
                lang = l;
                break;
            }
        }

        if (lang == null) {
            return wrapperOrHardReference(element);
        }

        return new TreeRangeReference(
            file,
            textRange.getStartOffset(),
            textRange.getEndOffset(),
            IdentikitImpl.fromPsi(element, lang),
            virtualFile
        );
    }

    @Nonnull
    private static PsiAnchor wrapperOrHardReference(@Nonnull PsiElement element) {
        for (SmartPointerAnchorProvider provider : SmartPointerAnchorProvider.EP_NAME.getExtensionList()) {
            PsiElement anchorElement = provider.getAnchor(element);
            if (anchorElement != null && anchorElement != element) {
                PsiAnchor wrappedAnchor = create(anchorElement);
                if (!(wrappedAnchor instanceof HardReference)) {
                    return new WrappedElementAnchor(provider, wrappedAnchor);
                }
            }
        }
        return new HardReference(element);
    }

    @Nullable
    @RequiredReadAction
    public static StubIndexReference createStubReference(@Nonnull PsiElement element, @Nonnull PsiFile containingFile) {
        if (element instanceof StubBasedPsiElement elt && element.isPhysical()
            && (element instanceof PsiCompiledElement || canHaveStub(containingFile))) {
            IStubElementType elementType = elt.getElementType();
            if (elt.getStub() != null || elementType.shouldCreateStub(element.getNode())) {
                int index = calcStubIndex(elt);
                if (index != -1) {
                    return new StubIndexReference(containingFile, index, containingFile.getLanguage(), elementType);
                }
            }
        }
        return null;
    }

    private static boolean canHaveStub(@Nonnull PsiFile file) {
        if (!(file instanceof PsiFileImpl)) {
            return false;
        }

        VirtualFile vFile = file.getVirtualFile();

        IStubFileElementType elementType = ((PsiFileImpl)file).getElementTypeForStubBuilder();
        return elementType != null && vFile != null && elementType.shouldBuildStubFor(vFile);
    }

    public static int calcStubIndex(@Nonnull StubBasedPsiElement psi) {
        if (psi instanceof PsiFile) {
            return 0;
        }

        StubElement liveStub = psi.getStub();
        if (liveStub != null) {
            return ((StubBase)liveStub).getStubId();
        }

        return ((PsiFileImpl)psi.getContainingFile()).calcTreeElement().getStubbedSpine().getStubIndex(psi);
    }

    private static class TreeRangeReference extends PsiAnchor {
        private final VirtualFile myVirtualFile;
        private final Project myProject;
        private final IdentikitImpl myInfo;
        private final int myStartOffset;
        private final int myEndOffset;

        private TreeRangeReference(
            @Nonnull PsiFile file,
            int startOffset,
            int endOffset,
            @Nonnull IdentikitImpl info,
            @Nonnull VirtualFile virtualFile
        ) {
            myVirtualFile = virtualFile;
            myProject = file.getProject();
            myStartOffset = startOffset;
            myEndOffset = endOffset;
            myInfo = info;
        }

        @Override
        @Nullable
        public PsiElement retrieve() {
            PsiFile psiFile = getFile();
            if (psiFile == null || !psiFile.isValid()) {
                return null;
            }

            return myInfo.findPsiElement(psiFile, myStartOffset, myEndOffset);
        }

        @Override
        @Nullable
        public PsiFile getFile() {
            Language language = myInfo.getFileLanguage();
            if (language == null) {
                return null;
            }
            return SelfElementInfo.restoreFileFromVirtual(myVirtualFile, myProject, language);
        }

        @Override
        public int getStartOffset() {
            return myStartOffset;
        }

        @Override
        public int getEndOffset() {
            return myEndOffset;
        }

        @Override
        public boolean equals(Object o) {
            return o == this
                || o instanceof TreeRangeReference that
                && myEndOffset == that.myEndOffset
                && myStartOffset == that.myStartOffset
                && myInfo.equals(that.myInfo)
                && myVirtualFile.equals(that.myVirtualFile);
        }

        @Override
        public int hashCode() {
            int result = myInfo.hashCode();
            result = 31 * result + myStartOffset;
            result = 31 * result + myEndOffset;
            result = 31 * result + myVirtualFile.hashCode();

            return result;
        }
    }

    public static class HardReference extends PsiAnchor {
        private final PsiElement myElement;

        public HardReference(@Nonnull PsiElement element) {
            myElement = element;
        }

        @Override
        public PsiElement retrieve() {
            return myElement.isValid() ? myElement : null;
        }

        @Override
        public PsiFile getFile() {
            return myElement.getContainingFile();
        }

        @Override
        @RequiredReadAction
        public int getStartOffset() {
            return myElement.getTextRange().getStartOffset();
        }

        @Override
        @RequiredReadAction
        public int getEndOffset() {
            return myElement.getTextRange().getEndOffset();
        }

        @Override
        public boolean equals(Object o) {
            return o == this
                || o instanceof HardReference that
                && myElement.equals(that.myElement);
        }

        @Override
        public int hashCode() {
            return myElement.hashCode();
        }
    }

    private static class PsiFileReference extends PsiAnchor {
        private final VirtualFile myFile;
        private final Project myProject;
        @Nonnull
        private final Language myLanguage;

        private PsiFileReference(@Nonnull VirtualFile file, @Nonnull PsiFile psiFile) {
            myFile = file;
            myProject = psiFile.getProject();
            myLanguage = findLanguage(psiFile);
        }

        @Nonnull
        private static Language findLanguage(@Nonnull PsiFile file) {
            FileViewProvider vp = file.getViewProvider();
            Set<Language> languages = vp.getLanguages();
            for (Language language : languages) {
                if (file.equals(vp.getPsi(language))) {
                    return language;
                }
            }
            throw new AssertionError("Non-retrievable file: " + file.getClass() + "; " + file.getLanguage() + "; " + languages);
        }

        @Override
        public PsiElement retrieve() {
            return getFile();
        }

        @Override
        @Nullable
        public PsiFile getFile() {
            return SelfElementInfo.restoreFileFromVirtual(myFile, myProject, myLanguage);
        }

        @Override
        public int getStartOffset() {
            return 0;
        }

        @Override
        public int getEndOffset() {
            return (int)myFile.getLength();
        }

        @Override
        public boolean equals(Object o) {
            return o == this
                || o instanceof PsiFileReference reference
                && myFile.equals(reference.myFile)
                && myLanguage.equals(reference.myLanguage)
                && myProject.equals(reference.myProject);
        }

        @Override
        public int hashCode() {
            return 31 * myFile.hashCode() + myLanguage.hashCode();
        }
    }

    private static class PsiDirectoryReference extends PsiAnchor {
        @Nonnull
        private final VirtualFile myFile;
        @Nonnull
        private final Project myProject;

        private PsiDirectoryReference(@Nonnull VirtualFile file, @Nonnull Project project) {
            myFile = file;
            myProject = project;
            assert file.isDirectory() : file;
        }

        @Override
        public PsiElement retrieve() {
            return SelfElementInfo.restoreDirectoryFromVirtual(myFile, myProject);
        }

        @Override
        public PsiFile getFile() {
            return null;
        }

        @Override
        public int getStartOffset() {
            return 0;
        }

        @Override
        public int getEndOffset() {
            return -1;
        }

        @Override
        public boolean equals(Object o) {
            return o == this
                || o instanceof PsiDirectoryReference reference
                && myFile.equals(reference.myFile)
                && myProject.equals(reference.myProject);
        }

        @Override
        public int hashCode() {
            return myFile.hashCode();
        }
    }

    @Nullable
    public static PsiElement restoreFromStubIndex(
        PsiFileWithStubSupport fileImpl,
        int index,
        @Nonnull IStubElementType elementType,
        boolean throwIfNull
    ) {
        if (fileImpl == null) {
            if (throwIfNull) {
                throw new AssertionError("Null file");
            }
            return null;
        }

        if (index == 0) {
            return fileImpl;
        }

        StubbedSpine spine = fileImpl.getStubbedSpine();
        StubBasedPsiElement psi = (StubBasedPsiElement)spine.getStubPsi(index);
        if (psi == null) {
            if (throwIfNull) {
                throw new AssertionError("Too large index: " + index + ">=" + spine.getStubCount());
            }
            return null;
        }

        if (psi.getElementType() != elementType) {
            if (throwIfNull) {
                throw new AssertionError("Element type mismatch: " + psi.getElementType() + "!=" + elementType);
            }
            return null;
        }

        return psi;
    }

    public static class StubIndexReference extends PsiAnchor {
        @Nonnull
        private final VirtualFile myVirtualFile;
        @Nonnull
        private final Project myProject;
        private final int myIndex;
        @Nonnull
        private final Language myLanguage;
        @Nonnull
        private final IStubElementType myElementType;

        private StubIndexReference(
            @Nonnull PsiFile file,
            int index,
            @Nonnull Language language,
            @Nonnull IStubElementType elementType
        ) {
            myLanguage = language;
            myElementType = elementType;
            myVirtualFile = file.getVirtualFile();
            if (file.getViewProvider() instanceof FreeThreadedFileViewProvider) {
                throw new IllegalArgumentException(
                    "Must not use StubIndexReference for injected file; take a closer look at HardReference instead"
                );
            }
            myProject = file.getProject();
            myIndex = index;
        }

        @Override
        @Nullable
        public PsiFile getFile() {
            if (myProject.isDisposed() || !myVirtualFile.isValid()) {
                return null;
            }
            FileViewProvider viewProvider = PsiManager.getInstance(myProject).findViewProvider(myVirtualFile);
            PsiFile file = viewProvider == null ? null : viewProvider.getPsi(myLanguage);
            return file instanceof PsiFileWithStubSupport ? file : null;
        }

        @Override
        public PsiElement retrieve() {
            return ReadAction.compute(() -> restoreFromStubIndex((PsiFileWithStubSupport)getFile(), myIndex, myElementType, false));
        }

        @Nonnull
        public String diagnoseNull() {
            PsiFile file = ReadAction.compute(this::getFile);
            try {
                PsiElement element =
                    ReadAction.compute(() -> restoreFromStubIndex((PsiFileWithStubSupport)file, myIndex, myElementType, true));
                return "No diagnostics, element=" + element + "@" + (element == null ? 0 : System.identityHashCode(element));
            }
            catch (AssertionError e) {
                String msg = e.getMessage();
                msg += file == null ? "\n no PSI file" : "\n current file stamp=" + (short)file.getModificationStamp();
                Document document = FileDocumentManager.getInstance().getCachedDocument(myVirtualFile);
                if (document != null) {
                    msg += "\n committed=" + PsiDocumentManager.getInstance(myProject).isCommitted(document);
                    msg += "\n saved=" + !FileDocumentManager.getInstance().isDocumentUnsaved(document);
                }
                return msg;
            }
        }

        @Override
        public boolean equals(Object o) {
            return o == this
                || o instanceof StubIndexReference that
                && myIndex == that.myIndex
                && myVirtualFile.equals(that.myVirtualFile)
                && Objects.equals(myElementType, that.myElementType)
                && myLanguage == that.myLanguage;
        }

        @Override
        public int hashCode() {
            return ((31 * myVirtualFile.hashCode() + myIndex) * 31 + myElementType.hashCode()) * 31 + myLanguage.hashCode();
        }

        @Override
        public String toString() {
            return "StubIndexReference{" +
                "myVirtualFile=" + myVirtualFile +
                ", myProject=" + myProject +
                ", myIndex=" + myIndex +
                ", myLanguage=" + myLanguage +
                ", myElementType=" + myElementType +
                '}';
        }

        @Override
        @RequiredReadAction
        public int getStartOffset() {
            return getTextRange().getStartOffset();
        }

        @Override
        @RequiredReadAction
        public int getEndOffset() {
            return getTextRange().getEndOffset();
        }

        @Nonnull
        @RequiredReadAction
        private TextRange getTextRange() {
            PsiElement resolved = retrieve();
            if (resolved == null) {
                throw new PsiInvalidElementAccessException(null, "Element type: " + myElementType + "; " + myVirtualFile);
            }
            return resolved.getTextRange();
        }

        @Nonnull
        public VirtualFile getVirtualFile() {
            return myVirtualFile;
        }

        @Nonnull
        public Project getProject() {
            return myProject;
        }
    }
}

