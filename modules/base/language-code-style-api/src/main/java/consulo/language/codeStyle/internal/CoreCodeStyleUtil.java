// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2024 consulo.io
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
package consulo.language.codeStyle.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.component.ProcessCanceledException;
import consulo.component.util.PluginExceptionUtil;
import consulo.document.util.TextRange;
import consulo.language.CompositeLanguage;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.FormatterTagHandler;
import consulo.language.codeStyle.FormattingModelBuilder;
import consulo.language.codeStyle.FormattingRangesInfo;
import consulo.language.codeStyle.PostFormatProcessor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.logging.Logger;
import consulo.logging.attachment.AttachmentFactory;
import consulo.logging.attachment.RuntimeExceptionWithAttachments;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class CoreCodeStyleUtil {
    private static final Logger LOG = Logger.getInstance(CoreCodeStyleUtil.class);

    private static final ThreadLocal<ProcessingUnderProgressInfo> SEQUENTIAL_PROCESSING_ALLOWED =
        ThreadLocal.withInitial(ProcessingUnderProgressInfo::new);

    
    public static TextRange[] getIndents(CharSequence charsSequence, int shift) {
        List<TextRange> result = new ArrayList<>();
        int whitespaceEnd = -1;
        int lastTextFound = 0;
        for (int i = charsSequence.length() - 1; i >= 0; i--) {
            char charAt = charsSequence.charAt(i);
            boolean isWhitespace = Character.isWhitespace(charAt);
            if (charAt == '\n') {
                result.add(new TextRange(i, (whitespaceEnd >= 0 ? whitespaceEnd : i) + 1).shiftRight(shift));
                whitespaceEnd = -1;
            }
            else if (whitespaceEnd >= 0) {
                if (isWhitespace) {
                    continue;
                }
                lastTextFound = result.size();
                whitespaceEnd = -1;
            }
            else if (isWhitespace) {
                whitespaceEnd = i;
            }
            else {
                lastTextFound = result.size();
            }
        }
        if (whitespaceEnd > 0) {
            result.add(new TextRange(0, whitespaceEnd + 1).shiftRight(shift));
        }
        if (lastTextFound < result.size()) {
            result = result.subList(0, lastTextFound);
        }
        return result.toArray(new TextRange[result.size()]);
    }

    public static PsiElement postProcessElement(PsiFile file, PsiElement element, boolean isWhitespaceOnly) {
        CodeStyleSettings settingsForFile = CodeStyle.getSettings(file);
        List<TextRange> textRanges;
        if (settingsForFile.FORMATTER_TAGS_ENABLED) {
            FormatterTagHandler tagHandler = new FormatterTagHandler(settingsForFile);
            textRanges = tagHandler.getEnabledRanges(file.getNode(), file.getTextRange());
        }
        else {
            textRanges = Collections.singletonList(element.getTextRange());
        }
        for (TextRange range : textRanges) {
            if (range.contains(element.getTextRange())) {
                PsiElement currElement = element;
                for (PostFormatProcessor postFormatProcessor : getPostProcessors(isWhitespaceOnly)) {
                    if (currElement == null) {
                        break;
                    }
                    currElement = processElementOrFail(postFormatProcessor, currElement, settingsForFile);
                }
                return currElement;
            }
            else if (range.intersects(element.getTextRange())) {
                postProcessRange(file, element.getTextRange().intersection(range), settingsForFile, isWhitespaceOnly);
            }
        }
        return element;
    }

    private static @Nullable PsiElement processElementOrFail(
        PostFormatProcessor processor,
        PsiElement element,
        CodeStyleSettings settings
    ) {
        PsiElement result = element;
        try {
            result = processor.processElement(result, settings);
            if (!result.isValid()) {
                LOG.error(new RuntimeExceptionWithAttachments(
                    String.format("PSI crash detected: processor=%s, result=%s", processor, result),
                    AttachmentFactory.get().create("text", result.getText())
                ));
                return null;
            }
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error(PluginExceptionUtil.createByClass(e, processor.getClass()));
        }
        return result;
    }

    private static Collection<PostFormatProcessor> getPostProcessors(boolean isWhitespaceOnly) {
        if (isWhitespaceOnly) {
            return ContainerUtil.filter(PostFormatProcessor.EP_NAME.getExtensionList(), processor -> processor.isWhitespaceOnly());
        }
        else {
            return PostFormatProcessor.EP_NAME.getExtensionList();
        }
    }

    public static List<RangeFormatInfo> getRangeFormatInfoList(PsiFile file, FormattingRangesInfo ranges) {
        List<RangeFormatInfo> infos = new ArrayList<>();
        for (TextRange range : ranges.getTextRanges()) {
            infos.add(new RangeFormatInfo(file, range));
        }
        return infos;
    }

    public static void postProcessRanges(
        List<? extends RangeFormatInfo> rangeFormatInfoList,
        Consumer<? super TextRange> postProcessFormatter
    ) {
        for (RangeFormatInfo info : rangeFormatInfoList) {
            int startOffset = info.getStartOffset();
            int endOffset = info.getEndOffset();
            if (startOffset >= 0 && endOffset >= 0 && endOffset > startOffset) {
                postProcessFormatter.accept(new TextRange(startOffset, endOffset));
            }
            info.disposePointers();
        }
    }

    public static void postProcessText(PsiFile file, TextRange textRange, boolean isWhitespaceOnly) {
        CodeStyleSettings settings = CodeStyle.getSettings(file);
        if (!getSettings(file).FORMATTER_TAGS_ENABLED) {
            postProcessRange(file, textRange, settings, isWhitespaceOnly);
        }
        else {
            postProcessEnabledRanges(file, textRange, settings, isWhitespaceOnly);
        }
    }

    private static void postProcessEnabledRanges(
        PsiFile file,
        TextRange range,
        CodeStyleSettings settings,
        boolean isWhitespaceOnly
    ) {
        List<TextRange> enabledRanges = new FormatterTagHandler(getSettings(file)).getEnabledRanges(file.getNode(), range);
        int delta = 0;
        for (TextRange enabledRange : enabledRanges) {
            enabledRange = enabledRange.shiftRight(delta);
            TextRange processedRange = postProcessRange(file, enabledRange, settings, isWhitespaceOnly);
            delta += processedRange.getLength() - enabledRange.getLength();
        }
    }

    private static TextRange postProcessRange(
        PsiFile file,
        TextRange textRange,
        CodeStyleSettings settings,
        boolean isWhitespaceOnly
    ) {
        TextRange currentRange = textRange;
        for (PostFormatProcessor myPostFormatProcessor : getPostProcessors(isWhitespaceOnly)) {
            currentRange = myPostFormatProcessor.processText(file, currentRange, settings);
        }
        return currentRange;
    }

    public static final class RangeFormatInfo {
        private final PsiFile myFile;
        private final @Nullable SmartPsiElementPointer<?> startPointer;
        private final @Nullable SmartPsiElementPointer<?> endPointer;
        private final boolean fromStart;
        private final boolean toEnd;

        RangeFormatInfo(PsiFile file, TextRange range) {
            myFile = file;
            fromStart = range.getStartOffset() == 0;
            toEnd = range.getEndOffset() == file.getTextLength();
            startPointer = fromStart ? null : createPsiPointer(range.getStartOffset());
            endPointer = toEnd ? null : createPsiPointer(range.getEndOffset());
        }

        private @Nullable SmartPsiElementPointer<?> createPsiPointer(int offset) {
            PsiElement element = findElementInTreeWithFormatterEnabled(myFile, offset);
            if (element != null) {
                if (!element.isValid()) {
                    LOG.error("Invalid element " + element + "; file: " + myFile.getName());
                }
                return SmartPointerManager.getInstance(myFile.getProject()).createSmartPsiElementPointer(element);
            }
            return null;
        }

        private int getStartOffset() {
            if (fromStart) {
                return 0;
            }
            TextRange range = getElementRange(startPointer);
            return range != null ? range.getStartOffset() : -1;
        }

        private int getEndOffset() {
            if (toEnd) {
                return myFile.getTextLength();
            }
            TextRange range = getElementRange(endPointer);
            return range != null ? range.getEndOffset() : -1;
        }

        private static @Nullable TextRange getElementRange(@Nullable SmartPsiElementPointer<?> pointer) {
            if (pointer == null) {
                return null;
            }
            PsiElement element = pointer.getElement();
            return element != null ? element.getTextRange() : null;
        }

        private void disposePointers() {
            SmartPointerManager pointerManager = SmartPointerManager.getInstance(myFile.getProject());
            if (startPointer != null) {
                pointerManager.removePointer(startPointer);
            }
            if (endPointer != null) {
                pointerManager.removePointer(endPointer);
            }
        }
    }

    @RequiredReadAction
    public static @Nullable PsiElement findElementInTreeWithFormatterEnabled(PsiFile file, int offset) {
        PsiElement bottommost = file.findElementAt(offset);
        if (bottommost != null && FormattingModelBuilder.forContext(bottommost) != null) {
            return bottommost;
        }

        Language fileLang = file.getLanguage();
        if (fileLang instanceof CompositeLanguage) {
            return file.getViewProvider().findElementAt(offset, fileLang);
        }

        return bottommost;
    }

    public static boolean isSequentialProcessingAllowed() {
        return SEQUENTIAL_PROCESSING_ALLOWED.get().isAllowed();
    }

    /**
     * Allows to define if {@link #isSequentialProcessingAllowed() sequential processing} should be allowed.
     * <p/>
     * Current approach is not allow to stop sequential processing for more than predefine amount of time (couple of seconds).
     * That means that call to this method with {@code 'true'} argument is not mandatory for successful processing even
     * if this method is called with {@code 'false'} argument before.
     *
     * @param allowed flag that defines if {@link #isSequentialProcessingAllowed() sequential processing} should be allowed
     */
    public static void setSequentialProcessingAllowed(boolean allowed) {
        ProcessingUnderProgressInfo info = SEQUENTIAL_PROCESSING_ALLOWED.get();
        if (allowed) {
            info.decrement();
        }
        else {
            info.increment();
        }
    }

    private static CodeStyleSettings getSettings(PsiFile file) {
        return CodeStyle.getSettings(file);
    }
}
