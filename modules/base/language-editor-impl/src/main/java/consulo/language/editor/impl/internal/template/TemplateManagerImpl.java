/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.language.editor.impl.internal.template;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.completion.OffsetKey;
import consulo.language.editor.impl.internal.completion.CompletionUtil;
import consulo.language.editor.impl.internal.completion.OffsetsInFile;
import consulo.language.editor.template.*;
import consulo.language.editor.template.context.TemplateActionContext;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.language.editor.template.event.TemplateEditingListener;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiCompiledElement;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.function.PairProcessor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiPredicate;

@Singleton
@ServiceImpl
public class TemplateManagerImpl extends TemplateManager implements Disposable {
    private final Project myProject;
    private boolean myTemplateTesting;

    private static final Key<TemplateStateImpl> TEMPLATE_STATE_KEY = Key.create("TEMPLATE_STATE_KEY");

    @Inject
    public TemplateManagerImpl(@Nonnull Project project) {
        myProject = project;
    }

    @Override
    public void dispose() {
    }

    /**
     * @deprecated Use {@link #setTemplateTesting(Project, Disposable)} instead
     */
    @TestOnly
    @Deprecated
    public void setTemplateTesting(final boolean templateTesting) {
        myTemplateTesting = templateTesting;
    }

    @TestOnly
    public static void setTemplateTesting(Project project, Disposable parentDisposable) {
        final TemplateManagerImpl instance = (TemplateManagerImpl)getInstance(project);
        instance.myTemplateTesting = true;
        Disposer.register(parentDisposable, () -> instance.myTemplateTesting = false);
    }

    private static void disposeState(@Nonnull TemplateStateImpl state) {
        Disposer.dispose(state);
    }

    @Nullable
    @Override
    public TemplateState getTemplateState(@Nonnull Editor editor) {
        return getTemplateStateImpl(editor);
    }

    @Nullable
    public static TemplateStateImpl getTemplateStateImpl(@Nonnull Editor editor) {
        TemplateStateImpl templateState = editor.getUserData(TEMPLATE_STATE_KEY);
        if (templateState != null && templateState.isDisposed()) {
            editor.putUserData(TEMPLATE_STATE_KEY, null);
            return null;
        }
        return templateState;
    }

    static void clearTemplateState(@Nonnull Editor editor) {
        TemplateStateImpl prevState = getTemplateStateImpl(editor);
        if (prevState != null) {
            disposeState(prevState);
        }
        editor.putUserData(TEMPLATE_STATE_KEY, null);
    }

    private TemplateStateImpl initTemplateState(@Nonnull Editor editor) {
        clearTemplateState(editor);
        TemplateStateImpl state = new TemplateStateImpl(myProject, editor);
        Disposer.register(this, state);
        editor.putUserData(TEMPLATE_STATE_KEY, state);
        return state;
    }

    @Override
    public boolean startTemplate(@Nonnull Editor editor, char shortcutChar) {
        Runnable runnable = prepareTemplate(editor, shortcutChar, null);
        if (runnable != null) {
            PsiDocumentManager.getInstance(myProject).commitDocument(editor.getDocument());
            runnable.run();
        }
        return runnable != null;
    }

    @Override
    public void startTemplate(@Nonnull final Editor editor, @Nonnull Template template) {
        startTemplate(editor, template, null);
    }

    @Override
    public void startTemplate(@Nonnull Editor editor, String selectionString, @Nonnull Template template) {
        startTemplate(editor, selectionString, template, true, null, null, null);
    }

    @Override
    public void startTemplate(
        @Nonnull Editor editor,
        @Nonnull Template template,
        TemplateEditingListener listener,
        final BiPredicate<String, String> processor
    ) {
        startTemplate(editor, null, template, true, listener, processor, null);
    }

    private void startTemplate(
        final Editor editor,
        final String selectionString,
        final Template template,
        boolean inSeparateCommand,
        TemplateEditingListener listener,
        final BiPredicate<String, String> processor,
        final Map<String, String> predefinedVarValues
    ) {
        final TemplateStateImpl templateState = initTemplateState(editor);

        //noinspection unchecked
        templateState.getProperties().put(ExpressionContext.SELECTION, selectionString);

        if (listener != null) {
            templateState.addTemplateStateListener(listener);
        }
        Runnable r = () -> {
            if (selectionString != null) {
                ApplicationManager.getApplication().runWriteAction(() -> EditorModificationUtil.deleteSelectedText(editor));
            }
            else {
                editor.getSelectionModel().removeSelection();
            }
            templateState.start((TemplateImpl)template, processor, predefinedVarValues);
        };
        if (inSeparateCommand) {
            CommandProcessor.getInstance().executeCommand(myProject, r, CodeInsightBundle.message("insert.code.template.command"), null);
        }
        else {
            r.run();
        }

        if (shouldSkipInTests()) {
            if (!templateState.isFinished()) {
                templateState.gotoEnd(false);
            }
        }
    }

    public boolean shouldSkipInTests() {
        return ApplicationManager.getApplication().isUnitTestMode() && !myTemplateTesting;
    }

    @Override
    public void startTemplate(@Nonnull final Editor editor, @Nonnull final Template template, TemplateEditingListener listener) {
        startTemplate(editor, null, template, true, listener, null, null);
    }

    @Override
    public void startTemplate(
        @Nonnull final Editor editor,
        @Nonnull final Template template,
        boolean inSeparateCommand,
        Map<String, String> predefinedVarValues,
        TemplateEditingListener listener
    ) {
        startTemplate(editor, null, template, inSeparateCommand, listener, null, predefinedVarValues);
    }

    private static int passArgumentBack(CharSequence text, int caretOffset) {
        int i = caretOffset - 1;
        for (; i >= 0; i--) {
            char c = text.charAt(i);
            if (isDelimiter(c)) {
                break;
            }
        }
        return i + 1;
    }

    private static boolean isDelimiter(char c) {
        return !Character.isJavaIdentifierPart(c);
    }

    private static <T, U> void addToMap(@Nonnull Map<T, U> map, @Nonnull Collection<? extends T> keys, U value) {
        for (T key : keys) {
            map.put(key, value);
        }
    }

    private static boolean containsTemplateStartingBefore(
        Map<Template, String> template2argument,
        int offset,
        int caretOffset,
        CharSequence text
    ) {
        for (Template template : template2argument.keySet()) {
            String argument = template2argument.get(template);
            int templateStart = getTemplateStart(template, argument, caretOffset, text);
            if (templateStart < offset) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public Runnable prepareTemplate(final Editor editor, char shortcutChar, @Nullable final PairProcessor<String, String> processor) {
        if (editor.getSelectionModel().hasSelection()) {
            return null;
        }

        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, myProject);
        if (file == null || file instanceof PsiCompiledElement) {
            return null;
        }

        Map<Template, String> template2argument = findMatchingTemplates(file, editor, shortcutChar, TemplateSettingsImpl.getInstanceImpl());
        TemplateActionContext templateActionContext = TemplateActionContext.expanding(file, editor);
        boolean multiCaretMode = editor.getCaretModel().getCaretCount() > 1;
        List<CustomLiveTemplate> customCandidates = ContainerUtil.findAll(
            CustomLiveTemplate.EP_NAME.getExtensionList(),
            customLiveTemplate -> shortcutChar == customLiveTemplate.getShortcut() &&
                (!multiCaretMode ||
                    supportsMultiCaretMode(customLiveTemplate)) &&
                isApplicable(customLiveTemplate, templateActionContext)
        );
        if (!customCandidates.isEmpty()) {
            int caretOffset = editor.getCaretModel().getOffset();
            CustomTemplateCallback templateCallback = new CustomTemplateCallback(editor, file);
            for (CustomLiveTemplate customLiveTemplate : customCandidates) {
                String key = customLiveTemplate.computeTemplateKey(templateCallback);
                if (key != null) {
                    int offsetBeforeKey = caretOffset - key.length();
                    CharSequence text = editor.getDocument().getImmutableCharSequence();
                    if (template2argument == null || !containsTemplateStartingBefore(
                        template2argument,
                        offsetBeforeKey,
                        caretOffset,
                        text
                    )) {
                        return () -> {
                            customLiveTemplate.expand(key, templateCallback);
                            if (multiCaretMode) {
                                PsiDocumentManager.getInstance(templateCallback.getProject()).commitDocument(editor.getDocument());
                            }
                        };
                    }
                }
            }
        }

        return startNonCustomTemplates(template2argument, editor, processor);
    }

    private static boolean supportsMultiCaretMode(CustomLiveTemplate customLiveTemplate) {
        return !(customLiveTemplate instanceof CustomLiveTemplateBase) || ((CustomLiveTemplateBase)customLiveTemplate).supportsMultiCaret();
    }

    private static int getArgumentOffset(int caretOffset, String argument, CharSequence text) {
        int argumentOffset = caretOffset - argument.length();
        if (argumentOffset > 0 && text.charAt(argumentOffset - 1) == ' ') {
            if (argumentOffset - 2 >= 0 && Character.isJavaIdentifierPart(text.charAt(argumentOffset - 2))) {
                argumentOffset--;
            }
        }
        return argumentOffset;
    }

    private static int getTemplateStart(Template template, String argument, int caretOffset, CharSequence text) {
        int templateStart;
        if (argument == null) {
            templateStart = caretOffset - template.getKey().length();
        }
        else {
            int argOffset = getArgumentOffset(caretOffset, argument, text);
            templateStart = argOffset - template.getKey().length();
        }
        return templateStart;
    }

    @Override
    public Map<Template, String> findMatchingTemplates(
        final PsiFile file,
        Editor editor,
        @Nullable Character shortcutChar,
        TemplateSettings templateSettings
    ) {
        final Document document = editor.getDocument();
        CharSequence text = document.getCharsSequence();
        final int caretOffset = editor.getCaretModel().getOffset();

        List<TemplateImpl> candidatesWithoutArgument = findMatchingTemplates(text, caretOffset, shortcutChar, templateSettings, false);

        int argumentOffset = passArgumentBack(text, caretOffset);
        String argument = null;
        if (argumentOffset >= 0) {
            argument = text.subSequence(argumentOffset, caretOffset).toString();
            if (argumentOffset > 0 && text.charAt(argumentOffset - 1) == ' ') {
                if (argumentOffset - 2 >= 0 && Character.isJavaIdentifierPart(text.charAt(argumentOffset - 2))) {
                    argumentOffset--;
                }
            }
        }
        List<TemplateImpl> candidatesWithArgument = findMatchingTemplates(text, argumentOffset, shortcutChar, templateSettings, true);

        if (candidatesWithArgument.isEmpty() && candidatesWithoutArgument.isEmpty()) {
            return null;
        }

        candidatesWithoutArgument =
            filterApplicableCandidates(TemplateActionContext.expanding(file, caretOffset), candidatesWithoutArgument);
        candidatesWithArgument = filterApplicableCandidates(TemplateActionContext.expanding(file, argumentOffset), candidatesWithArgument);
        Map<Template, String> candidate2Argument = new HashMap<>();
        addToMap(candidate2Argument, candidatesWithoutArgument, null);
        addToMap(candidate2Argument, candidatesWithArgument, argument);
        return candidate2Argument;
    }

    @Override
    @Nullable
    public Runnable startNonCustomTemplates(
        final Map<Template, String> template2argument,
        final Editor editor,
        @Nullable final PairProcessor<String, String> processor
    ) {
        final int caretOffset = editor.getCaretModel().getOffset();
        final Document document = editor.getDocument();
        final CharSequence text = document.getCharsSequence();

        if (template2argument == null || template2argument.isEmpty()) {
            return null;
        }

        return () -> {
            if (template2argument.size() == 1) {
                Template template = template2argument.keySet().iterator().next();
                String argument = template2argument.get(template);
                int templateStart = getTemplateStart(template, argument, caretOffset, text);
                startTemplateWithPrefix(editor, template, templateStart, processor, argument);
            }
            else {
                ListTemplatesHandler.showTemplatesLookup(myProject, editor, template2argument);
            }
        };
    }

    private static List<TemplateImpl> findMatchingTemplates(
        CharSequence text,
        int caretOffset,
        @Nullable Character shortcutChar,
        TemplateSettings settings,
        boolean hasArgument
    ) {
        List<TemplateImpl> candidates = Collections.emptyList();
        for (int i = ((TemplateSettingsImpl)settings).getMaxKeyLength(); i >= 1; i--) {
            int wordStart = caretOffset - i;
            if (wordStart < 0) {
                continue;
            }
            String key = text.subSequence(wordStart, caretOffset).toString();
            if (Character.isJavaIdentifierStart(key.charAt(0))) {
                if (wordStart > 0 && Character.isJavaIdentifierPart(text.charAt(wordStart - 1))) {
                    continue;
                }
            }

            candidates = ((TemplateSettingsImpl)settings).collectMatchingCandidates(key, shortcutChar, hasArgument);
            if (!candidates.isEmpty()) {
                break;
            }
        }
        return candidates;
    }

    public void startTemplateWithPrefix(
        final Editor editor,
        final Template template,
        @Nullable final PairProcessor<String, String> processor,
        @Nullable String argument
    ) {
        final int caretOffset = editor.getCaretModel().getOffset();
        String key = template.getKey();
        int startOffset = caretOffset - key.length();
        if (argument != null) {
            if (!isDelimiter(key.charAt(key.length() - 1))) {
                // pass space
                startOffset--;
            }
            startOffset -= argument.length();
        }
        startTemplateWithPrefix(editor, template, startOffset, processor, argument);
    }

    public void startTemplateWithPrefix(
        final Editor editor,
        final Template template,
        final int templateStart,
        @Nullable final PairProcessor<String, String> processor,
        @Nullable final String argument
    ) {
        final int caretOffset = editor.getCaretModel().getOffset();
        final TemplateStateImpl templateState = initTemplateState(editor);
        CommandProcessor commandProcessor = CommandProcessor.getInstance();
        commandProcessor.executeCommand(
            myProject,
            () -> {
                editor.getDocument().deleteString(templateStart, caretOffset);
                editor.getCaretModel().moveToOffset(templateStart);
                editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                editor.getSelectionModel().removeSelection();
                Map<String, String> predefinedVarValues = null;
                if (argument != null) {
                    predefinedVarValues = new HashMap<>();
                    predefinedVarValues.put(TemplateImpl.ARG, argument);
                }
                templateState.start((TemplateImpl)template, processor, predefinedVarValues);
            },
            CodeInsightBundle.message("insert.code.template.command"),
            null
        );
    }

    private List<TemplateImpl> filterApplicableCandidates(
        @Nonnull TemplateActionContext templateActionContext,
        @Nonnull List<TemplateImpl> candidates
    ) {
        if (candidates.isEmpty()) {
            return candidates;
        }

        PsiFile copy = insertDummyIdentifierWithCache(templateActionContext).getFile();

        List<TemplateImpl> result = new ArrayList<>();
        for (TemplateImpl candidate : candidates) {
            if (isApplicable(
                candidate,
                TemplateActionContext.expanding(copy, templateActionContext.getStartOffset() - candidate.getKey().length())
            )) {
                result.add(candidate);
            }
        }
        return result;
    }

    public boolean isApplicable(Template template, @Nonnull TemplateActionContext templateActionContext) {
        return isApplicable(template, getApplicableContextTypes(templateActionContext));
    }

    /**
     * @deprecated use {@link #isApplicable(TemplateImpl, TemplateActionContext)}
     */
    @Deprecated(forRemoval = true)
    public boolean isApplicable(PsiFile file, int offset, Template template) {
        return isApplicable(template, TemplateActionContext.expanding(file, offset));
    }

    private static List<TemplateContextType> getBases(TemplateContextType type) {
        ArrayList<TemplateContextType> list = new ArrayList<>();
        while (true) {
            type = type.getBaseContextType();
            if (type == null) {
                return list;
            }
            list.add(type);
        }
    }

    private static Set<TemplateContextType> getDirectlyApplicableContextTypes(@Nonnull TemplateActionContext templateActionContext) {
        LinkedHashSet<TemplateContextType> set = new LinkedHashSet<>();
        for (TemplateContextType contextType : TemplateContextType.EP_NAME.getExtensionList()) {
            if (contextType.isInContext(templateActionContext)) {
                set.add(contextType);
            }
        }

        removeBases:
        while (true) {
            for (TemplateContextType type : set) {
                if (set.removeAll(getBases(type))) {
                    continue removeBases;
                }
            }

            return set;
        }
    }

    @Override
    @Nullable
    public Template getActiveTemplate(@Nonnull Editor editor) {
        final TemplateStateImpl templateState = getTemplateStateImpl(editor);
        return templateState != null ? templateState.getTemplate() : null;
    }

    @Override
    public boolean finishTemplate(@Nonnull Editor editor) {
        TemplateStateImpl state = getTemplateStateImpl(editor);
        if (state != null) {
            state.gotoEnd();
            return true;
        }
        return false;
    }

    public boolean isApplicable(Template template, Set<TemplateContextType> contextTypes) {
        for (TemplateContextType type : contextTypes) {
            if (template.getTemplateContext().isEnabled(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @implNote custom templates and callbacks require additional work. There is a single place where offset provided externally, instead
     * of using one from the callback and this is probably a mistake. If this is the case, action context may be included into the callback.
     */
    public static boolean isApplicable(
        @Nonnull CustomLiveTemplate customLiveTemplate,
        @Nonnull TemplateActionContext templateActionContext
    ) {
        CustomTemplateCallback callback =
            new CustomTemplateCallback(Objects.requireNonNull(templateActionContext.getEditor()), templateActionContext.getFile());
        return customLiveTemplate.isApplicable(callback, callback.getOffset(), templateActionContext.isSurrounding());
    }

    @Override
    public List<Template> listApplicableTemplates(@Nonnull TemplateActionContext templateActionContext) {
        Set<TemplateContextType> contextTypes = getApplicableContextTypes(templateActionContext);

        final ArrayList<Template> result = new ArrayList<>();
        for (final Template template : TemplateSettingsImpl.getInstanceImpl().getTemplates()) {
            if (!template.isDeactivated() && (!templateActionContext.isSurrounding() || template.isSelectionTemplate()) && isApplicable(
                template,
                contextTypes
            )) {
                result.add(template);
            }
        }
        return result;
    }

    @Override
    public List<Template> listApplicableTemplateWithInsertingDummyIdentifier(@Nonnull TemplateActionContext templateActionContext) {
        OffsetsInFile offsets = insertDummyIdentifierWithCache(templateActionContext);
        return listApplicableTemplates(TemplateActionContext.create(
            offsets.getFile(),
            null,
            getStartOffset(offsets),
            getEndOffset(offsets),
            templateActionContext.isSurrounding()
        ));
    }

    public static List<CustomLiveTemplate> listApplicableCustomTemplates(@Nonnull TemplateActionContext templateActionContext) {
        List<CustomLiveTemplate> result = new ArrayList<>();
        for (CustomLiveTemplate template : CustomLiveTemplate.EP_NAME.getExtensionList()) {
            if ((!templateActionContext.isSurrounding() || template.supportsWrapping()) && isApplicable(template, templateActionContext)) {
                result.add(template);
            }
        }
        return result;
    }

    @RequiredReadAction
    @Nonnull
    @Override
    public Set<TemplateContextType> getApplicableContextTypes(@Nonnull TemplateActionContext templateActionContext) {
        Set<TemplateContextType> result = getDirectlyApplicableContextTypes(templateActionContext);

        PsiFile file = templateActionContext.getFile();
        Language baseLanguage = file.getViewProvider().getBaseLanguage();
        if (baseLanguage != file.getLanguage()) {
            PsiFile basePsi = file.getViewProvider().getPsi(baseLanguage);
            if (basePsi != null) {
                result.addAll(getDirectlyApplicableContextTypes(templateActionContext.withFile(basePsi)));
            }
        }

        // if we have, for example, a Ruby fragment in RHTML selected with its exact bounds, the file language and the base
        // language will be ERb, so we won't match HTML templates for it. but they're actually valid
        Language languageAtOffset = PsiUtilCore.getLanguageAtOffset(file, templateActionContext.getStartOffset());
        if (languageAtOffset != file.getLanguage() && languageAtOffset != baseLanguage) {
            PsiFile basePsi = file.getViewProvider().getPsi(languageAtOffset);
            if (basePsi != null) {
                result.addAll(getDirectlyApplicableContextTypes(templateActionContext.withFile(basePsi)));
            }
        }

        return result;
    }

    private static final OffsetKey START_OFFSET = OffsetKey.create("start", false);
    private static final OffsetKey END_OFFSET = OffsetKey.create("end", true);

    private static int getStartOffset(OffsetsInFile offsets) {
        return offsets.getOffsets().getOffset(START_OFFSET);
    }

    private static int getEndOffset(OffsetsInFile offsets) {
        return offsets.getOffsets().getOffset(END_OFFSET);
    }

    private static OffsetsInFile insertDummyIdentifierWithCache(@Nonnull TemplateActionContext templateActionContext) {
        ProperTextRange editRange = ProperTextRange.create(templateActionContext.getStartOffset(), templateActionContext.getEndOffset());
        PsiFile file = templateActionContext.getFile();
        assertRangeWithinDocument(editRange, Objects.requireNonNull(file.getViewProvider().getDocument()));

        ConcurrentMap<Pair<ProperTextRange, String>, OffsetsInFile> map = LanguageCachedValueUtil.getCachedValue(
            file,
            () -> CachedValueProvider.Result.create(
                ConcurrentFactoryMap.createMap(key -> copyWithDummyIdentifier(
                    new OffsetsInFile(file),
                    key.first.getStartOffset(),
                    key.first.getEndOffset(),
                    key.second
                )),
                file,
                file.getViewProvider().getDocument()
            )
        );
        return map.get(Pair.create(editRange, CompletionUtil.DUMMY_IDENTIFIER_TRIMMED));
    }

    private static void assertRangeWithinDocument(ProperTextRange editRange, Document document) {
        TextRange docRange = TextRange.from(0, document.getTextLength());
        assert docRange.contains(editRange) : docRange + " doesn't contain " + editRange;
    }

    @Nonnull
    public static OffsetsInFile copyWithDummyIdentifier(OffsetsInFile offsetMap, int startOffset, int endOffset, String replacement) {
        offsetMap.getOffsets().addOffset(START_OFFSET, startOffset);
        offsetMap.getOffsets().addOffset(END_OFFSET, endOffset);

        Document document = offsetMap.getFile().getViewProvider().getDocument();
        assert document != null;
        if (replacement.isEmpty() && startOffset == endOffset
            && PsiDocumentManager.getInstance(offsetMap.getFile().getProject()).isCommitted(document)) {
            return offsetMap;
        }

        OffsetsInFile hostOffsets = offsetMap.toTopLevelFile();
        OffsetsInFile hostCopy = hostOffsets.copyWithReplacement(getStartOffset(hostOffsets), getEndOffset(hostOffsets), replacement);
        return hostCopy.toInjectedIfAny(getStartOffset(hostCopy));
    }
}
