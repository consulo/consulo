/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.find.impl;

import consulo.find.FindSearchContext;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.find.FindResult;
import consulo.document.Document;
import consulo.util.dataholder.Key;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiFileRange;
import consulo.usage.UsageInfo;
import jakarta.annotation.Nonnull;

public class FindResultUsageInfo extends UsageInfo {
    private final FindManager myFindManager;
    private final FindModel myFindModel;
    private SmartPsiFileRange myAnchor;

    private Boolean myCachedResult;
    private long myTimestamp = 0;

    private static final Key<Long> DOCUMENT_TIMESTAMP_KEY = Key.create("FindResultUsageInfo.DOCUMENT_TIMESTAMP_KEY");

    @Override
    public boolean isValid() {
        if (!super.isValid()) {
            return false;
        }

        PsiFile psiFile = getPsiFile();
        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(psiFile);
        if (document == null) {
            myCachedResult = null;
            return false;
        }

        Boolean cachedResult = myCachedResult;
        if (document.getModificationStamp() == myTimestamp && cachedResult != null) {
            return cachedResult;
        }
        myTimestamp = document.getModificationStamp();

        Segment segment = getSegment();
        boolean isFileOrBinary = isFileOrBinary();
        if (segment == null && !isFileOrBinary) {
            myCachedResult = false;
            return false;
        }

        VirtualFile file = psiFile.getVirtualFile();
        if (isFileOrBinary) {
            myCachedResult = file.isValid();
            return myCachedResult;
        }

        Segment searchOffset;
        if (myAnchor != null) {
            searchOffset = myAnchor.getRange();
            if (searchOffset == null) {
                myCachedResult = false;
                return false;
            }
        }
        else {
            searchOffset = segment;
        }

        int offset = searchOffset.getStartOffset();
        Long data = myFindModel.getUserData(DOCUMENT_TIMESTAMP_KEY);
        if (data == null || data != myTimestamp) {
            data = myTimestamp;
            FindManagerImpl.clearPreviousFindData(myFindModel);
        }
        myFindModel.putUserData(DOCUMENT_TIMESTAMP_KEY, data);
        FindResult result;
        do {
            result = myFindManager.findString(document.getCharsSequence(), offset, myFindModel, file);
            offset = result.getEndOffset() == offset ? offset + 1 : result.getEndOffset();
            if (!result.isStringFound()) {
                myCachedResult = false;
                return false;
            }
        }
        while (result.getStartOffset() < segment.getStartOffset());

        boolean ret = segment.getStartOffset() == result.getStartOffset() && segment.getEndOffset() == result.getEndOffset();
        myCachedResult = ret;
        return ret;
    }

    private PsiFile getPsiFile() {
        return (PsiFile)getElement();
    }

    public FindResultUsageInfo(
        @Nonnull FindManager finder,
        @Nonnull PsiFile file,
        int offset,
        @Nonnull FindModel findModel,
        @Nonnull FindResult result
    ) {
        super(file, result.getStartOffset(), result.getEndOffset());

        myFindManager = finder;
        myFindModel = findModel;

        assert result.isStringFound();

        if (myFindModel.isRegularExpressions() || myFindModel.getSearchContext() != FindSearchContext.ANY) {
            myAnchor = SmartPointerManager.getInstance(getProject()).createSmartPsiFileRangePointer(file, TextRange.from(offset, 0));
        }
    }
}
