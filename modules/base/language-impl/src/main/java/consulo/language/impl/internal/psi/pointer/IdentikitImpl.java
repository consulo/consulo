// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.psi.pointer;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.IFileElementType;
import consulo.language.impl.file.AbstractFileViewProvider;
import consulo.language.impl.psi.pointer.Identikit;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.util.interner.Interner;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * @author peter
 */
public abstract class IdentikitImpl implements Identikit {
    private static final Logger LOG = Logger.getInstance(IdentikitImpl.class);
    private static final Interner<ByTypeImpl> ourPlainInterner = Interner.createWeakInterner();
    private static final Interner<ByAnchor> ourAnchorInterner = Interner.createWeakInterner();

    public static ByTypeImpl fromPsi(PsiElement element, Language fileLanguage) {
        return fromTypes(element.getClass(), PsiUtilCore.getElementType(element), fileLanguage);
    }

    static @Nullable Pair<ByAnchor, PsiElement> withAnchor(PsiElement element, Language fileLanguage) {
        PsiUtilCore.ensureValid(element);
        if (element.isPhysical()) {
            for (SmartPointerAnchorProvider provider : SmartPointerAnchorProvider.EP_NAME.getExtensionList()) {
                PsiElement anchor = provider.getAnchor(element);
                if (anchor != null && anchor.isPhysical() && provider.restoreElement(anchor) == element) {
                    ByAnchor anchorKit = new ByAnchor(fromPsi(element, fileLanguage), fromPsi(anchor, fileLanguage), provider);
                    return Pair.create(ourAnchorInterner.intern(anchorKit), anchor);
                }
            }
        }
        return null;
    }

    
    static ByTypeImpl fromTypes(
        Class<? extends PsiElement> elementClass,
        @Nullable IElementType elementType,
        Language fileLanguage
    ) {
        return ourPlainInterner.intern(new ByTypeImpl(elementClass, elementType, fileLanguage));
    }

    public static class ByTypeImpl extends IdentikitImpl implements Identikit.ByType {
        private final String myElementClassName;
        private final short myElementTypeId;
        private final String myFileLanguageId;

        private ByTypeImpl(
            Class<? extends PsiElement> elementClass,
            @Nullable IElementType elementType,
            Language fileLanguage
        ) {
            myElementClassName = elementClass.getName();
            myElementTypeId = elementType != null ? elementType.getIndex() : -1;
            myFileLanguageId = fileLanguage.getID();
        }

        @Override
        @RequiredReadAction
        public @Nullable PsiElement findPsiElement(PsiFile file, int startOffset, int endOffset) {
            Language fileLanguage = Language.findLanguageByID(myFileLanguageId);
            if (fileLanguage == null) {
                return null;   // plugin has been unloaded
            }
            Language actualLanguage = fileLanguage != Language.ANY ? fileLanguage : file.getViewProvider().getBaseLanguage();
            PsiFile actualLanguagePsi = file.getViewProvider().getPsi(actualLanguage);
            if (actualLanguagePsi == null) {
                return null; // the file has changed its language or dialect, so we can't restore
            }
            return findInside(actualLanguagePsi, startOffset, endOffset);
        }

        @RequiredReadAction
        public PsiElement findInside(PsiElement element, int startOffset, int endOffset) {
            // finds child in this tree only, unlike PsiElement.findElementAt()
            PsiElement anchor = AbstractFileViewProvider.findElementAt(element, startOffset);
            if (anchor == null && startOffset == element.getTextLength()) {
                anchor = PsiTreeUtil.getDeepestLast(element);
            }
            if (anchor == null) {
                return null;
            }

            PsiElement result = findParent(startOffset, endOffset, anchor);
            if (endOffset == startOffset) {
                while ((result == null
                    || result.getTextRange().getStartOffset() != startOffset) && anchor.getTextRange().getStartOffset() == endOffset) {
                    anchor = PsiTreeUtil.prevLeaf(anchor, false);
                    if (anchor == null) {
                        break;
                    }

                    result = findParent(startOffset, endOffset, anchor);
                }
            }
            return result;

        }

        @RequiredReadAction
        private @Nullable PsiElement findParent(int startOffset, int endOffset, PsiElement anchor) {
            TextRange range = anchor.getTextRange();

            if (range.getStartOffset() != startOffset) {
                return null;
            }
            while (range.getEndOffset() < endOffset) {
                anchor = anchor.getParent();
                if (anchor == null || anchor instanceof PsiDirectory) {
                    return null;
                }
                range = anchor.getTextRange();
            }

            while (range.getEndOffset() == endOffset) {
                if (isAcceptable(anchor)) {
                    return anchor;
                }
                anchor = anchor.getParent();
                if (anchor == null || anchor instanceof PsiDirectory) {
                    break;
                }
                range = anchor.getTextRange();
            }

            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ByTypeImpl type = (ByTypeImpl)o;
            return myElementTypeId == type.myElementTypeId && Objects.equals(myElementClassName, type.myElementClassName) && Objects.equals(
                myFileLanguageId,
                type.myFileLanguageId
            );
        }

        @Override
        public int hashCode() {
            return Objects.hash(myElementClassName, myElementTypeId, myFileLanguageId);
        }

        @Override
        public String toString() {
            return Map.of("class", myElementClassName, "elementType", myElementTypeId, "fileLanguage", myFileLanguageId).toString();
        }

        @Override
        public @Nullable Language getFileLanguage() {
            return Language.findLanguageByID(myFileLanguageId);
        }

        @Override
        public boolean isForPsiFile() {
            if (myElementTypeId < 0) {
                return false;
            }
            IElementType elementType = IElementType.find(myElementTypeId);
            return elementType instanceof IFileElementType;
        }

        private boolean isAcceptable(PsiElement element) {
            IElementType type = PsiUtilCore.getElementType(element);
            return myElementClassName.equals(element.getClass().getName()) && type != null && myElementTypeId == type.getIndex();
        }
    }

    static class ByAnchor extends IdentikitImpl {
        private final ByTypeImpl myElementInfo;
        private final ByTypeImpl myAnchorInfo;
        private final SmartPointerAnchorProvider myAnchorProvider;

        ByAnchor(ByTypeImpl elementInfo, ByTypeImpl anchorInfo, SmartPointerAnchorProvider anchorProvider) {
            myElementInfo = elementInfo;
            myAnchorInfo = anchorInfo;
            myAnchorProvider = anchorProvider;
        }

        @Override
        public boolean equals(Object o) {
            return o == this
                || o instanceof ByAnchor anchor
                && myElementInfo.equals(anchor.myElementInfo)
                && myAnchorInfo.equals(anchor.myAnchorInfo)
                && myAnchorProvider.equals(anchor.myAnchorProvider);
        }

        @Override
        public int hashCode() {
            return myElementInfo.hashCode();
        }

        @Override
        @RequiredReadAction
        public @Nullable PsiElement findPsiElement(PsiFile file, int startOffset, int endOffset) {
            PsiElement anchor = myAnchorInfo.findPsiElement(file, startOffset, endOffset);
            PsiElement element = anchor == null ? null : myAnchorProvider.restoreElement(anchor);
            return element != null && myElementInfo.isAcceptable(element) ? element : null;
        }

        @Override
        public @Nullable Language getFileLanguage() {
            return myAnchorInfo.getFileLanguage();
        }

        @Override
        public boolean isForPsiFile() {
            return myAnchorInfo.isForPsiFile();
        }
    }

}
