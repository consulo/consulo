/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.application.util.ClientId;
import consulo.application.util.matcher.PlainPrefixMatcher;
import consulo.codeEditor.Editor;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.document.Document;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.completion.lookup.*;
import consulo.language.editor.completion.lookup.event.LookupAdapter;
import consulo.language.editor.completion.lookup.event.LookupEvent;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.template.*;
import consulo.language.editor.template.context.TemplateActionContext;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.util.AttachmentFactoryUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.MultiMap;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public class ListTemplatesHandler implements CodeInsightActionHandler {

    private static final Logger LOG = Logger.getInstance(ListTemplatesHandler.class);

    @RequiredUIAccess
    @Override
    public void invoke(@Nonnull final Project project, @Nonnull final Editor editor, @Nonnull PsiFile file) {
        EditorModificationUtil.fillVirtualSpaceUntilCaret(editor);

        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
        int offset = editor.getCaretModel().getOffset();
        List<? extends Template> applicableTemplates = TemplateManager.getInstance(project)
            .listApplicableTemplateWithInsertingDummyIdentifier(TemplateActionContext.expanding(file, editor));

        Map<Template, String> matchingTemplates = filterTemplatesByPrefix(applicableTemplates, editor, offset, false, true);
        MultiMap<String, CustomLiveTemplateLookupElement> customTemplatesLookupElements =
            getCustomTemplatesLookupItems(editor, file, offset);

        if (matchingTemplates.isEmpty()) {
            for (Template template : applicableTemplates) {
                matchingTemplates.put(template, null);
            }
        }

        if (matchingTemplates.isEmpty() && customTemplatesLookupElements.isEmpty()) {
            if (!Application.get().isUnitTestMode()) {
                HintManager.getInstance().showErrorHint(editor, CodeInsightLocalize.templatesNoDefined());
            }
            return;
        }

        showTemplatesLookup(project, editor, file, matchingTemplates, customTemplatesLookupElements);
    }

    public static Map<Template, String> filterTemplatesByPrefix(
        @Nonnull Collection<? extends Template> templates,
        @Nonnull Editor editor,
        int offset,
        boolean fullMatch,
        boolean searchInDescription
    ) {
        if (offset > editor.getDocument().getTextLength()) {
            LOG.error(
                "Cannot filter templates, index out of bounds. Offset: " + offset,
                AttachmentFactoryUtil.createAttachment(editor.getDocument())
            );
        }
        CharSequence documentText = editor.getDocument().getCharsSequence().subSequence(0, offset);

        String prefixWithoutDots = computeDescriptionMatchingPrefix(editor.getDocument(), offset);
        Pattern prefixSearchPattern = Pattern.compile(".*\\b" + prefixWithoutDots + ".*");

        Map<Template, String> matchingTemplates = new TreeMap<>(TemplateComparator.INSTANCE);
        for (Template template : templates) {
            ProgressManager.checkCanceled();
            String templateKey = template.getKey();
            if (fullMatch) {
                int startOffset = documentText.length() - templateKey.length();
                if (startOffset <= 0 || !Character.isJavaIdentifierPart(documentText.charAt(startOffset - 1))) {
                    // after non-identifier
                    if (StringUtil.endsWith(documentText, templateKey)) {
                        matchingTemplates.put(template, templateKey);
                    }
                }
            }
            else {
                if (!ClientId.isCurrentlyUnderLocalId() && prefixWithoutDots.isEmpty()) {
                    matchingTemplates.put(template, prefixWithoutDots);
                    continue;
                }

                for (int i = templateKey.length(); i > 0; i--) {
                    ProgressManager.checkCanceled();
                    String prefix = templateKey.substring(0, i);
                    int startOffset = documentText.length() - i;
                    if (startOffset > 0 && Character.isJavaIdentifierPart(documentText.charAt(startOffset - 1))) {
                        // after java identifier
                        continue;
                    }
                    if (StringUtil.endsWith(documentText, prefix)) {
                        matchingTemplates.put(template, prefix);
                        break;
                    }
                }
            }

            if (searchInDescription && !matchingTemplates.containsKey(template)) {
                String templateDescription = template.getDescription();
                if (!prefixWithoutDots.isEmpty()
                    && templateDescription != null
                    && prefixSearchPattern.matcher(templateDescription).matches()) {
                    matchingTemplates.put(template, prefixWithoutDots);
                }
            }
        }

        return matchingTemplates;
    }

    private static void showTemplatesLookup(
        final Project project,
        final Editor editor,
        final PsiFile file,
        @Nonnull Map<Template, String> matchingTemplates,
        @Nonnull MultiMap<String, CustomLiveTemplateLookupElement> customTemplatesLookupElements
    ) {
        LookupEx lookup =
            (LookupEx)LookupManager.getInstance(project).createLookup(editor, LookupElement.EMPTY_ARRAY, "", new TemplatesArranger());
        for (Map.Entry<Template, String> entry : matchingTemplates.entrySet()) {
            Template template = entry.getKey();
            lookup.addItem(createTemplateElement(template), new PlainPrefixMatcher(StringUtil.notNullize(entry.getValue())));
        }

        for (Map.Entry<String, Collection<CustomLiveTemplateLookupElement>> entry : customTemplatesLookupElements.entrySet()) {
            for (CustomLiveTemplateLookupElement lookupElement : entry.getValue()) {
                lookup.addItem(lookupElement, new PlainPrefixMatcher(entry.getKey()));
            }
        }

        showLookup(lookup, file);
    }

    public static MultiMap<String, CustomLiveTemplateLookupElement> getCustomTemplatesLookupItems(
        @Nonnull Editor editor,
        @Nonnull PsiFile file,
        int offset
    ) {
        final MultiMap<String, CustomLiveTemplateLookupElement> result = MultiMap.create();
        CustomTemplateCallback customTemplateCallback = new CustomTemplateCallback(editor, file);
        TemplateActionContext templateActionContext = TemplateActionContext.expanding(file, editor);
        for (CustomLiveTemplate customLiveTemplate : TemplateManagerImpl.listApplicableCustomTemplates(templateActionContext)) {
            if (customLiveTemplate instanceof CustomLiveTemplateBase customLiveTemplateBase) {
                String customTemplatePrefix =
                    customLiveTemplateBase.computeTemplateKeyWithoutContextChecking(customTemplateCallback);
                if (customTemplatePrefix != null) {
                    result.putValues(
                        customTemplatePrefix,
                        customLiveTemplateBase.getLookupElements(file, editor, offset)
                    );
                }
            }
        }
        return result;
    }

    private static LiveTemplateLookupElement createTemplateElement(final Template template) {
        return new LiveTemplateLookupElementImpl(template, false) {
            @Override
            public Set<String> getAllLookupStrings() {
                String description = template.getDescription();
                if (description == null) {
                    return super.getAllLookupStrings();
                }
                return Set.of(getLookupString(), description);
            }
        };
    }

    private static String computePrefix(Template template, String argument) {
        String key = template.getKey();
        if (argument == null) {
            return key;
        }
        if (key.length() > 0 && Character.isJavaIdentifierPart(key.charAt(key.length() - 1))) {
            return key + ' ' + argument;
        }
        return key + argument;
    }

    public static void showTemplatesLookup(final Project project, final Editor editor, Map<Template, String> template2Argument) {
        final LookupEx lookup = (LookupEx)LookupManager.getInstance(project)
            .createLookup(editor, LookupElement.EMPTY_ARRAY, "", new LookupArranger.DefaultArranger());
        for (Template template : template2Argument.keySet()) {
            String prefix = computePrefix(template, template2Argument.get(template));
            lookup.addItem(createTemplateElement(template), new PlainPrefixMatcher(prefix));
        }

        showLookup(lookup, template2Argument);
    }

    private static void showLookup(LookupEx lookup, @Nullable Map<Template, String> template2Argument) {
        Editor editor = lookup.getEditor();
        Project project = editor.getProject();
        lookup.addLookupListener(new MyLookupAdapter(project, editor, template2Argument));
        lookup.refreshUi(false, true);
        lookup.showLookup();
    }

    private static void showLookup(LookupEx lookup, @Nonnull PsiFile file) {
        Editor editor = lookup.getEditor();
        Project project = editor.getProject();
        lookup.addLookupListener(new MyLookupAdapter(project, editor, file));
        lookup.refreshUi(false, true);
        lookup.showLookup();
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    private static String computeDescriptionMatchingPrefix(Document document, int offset) {
        CharSequence chars = document.getCharsSequence();
        int start = offset;
        while (true) {
            if (start == 0) {
                break;
            }
            char c = chars.charAt(start - 1);
            if (!(Character.isJavaIdentifierPart(c))) {
                break;
            }
            start--;
        }
        return chars.subSequence(start, offset).toString();
    }

    private static class MyLookupAdapter extends LookupAdapter {
        private final Project myProject;
        private final Editor myEditor;
        private final Map<Template, String> myTemplate2Argument;
        private final PsiFile myFile;

        public MyLookupAdapter(Project project, Editor editor, @Nullable Map<Template, String> template2Argument) {
            myProject = project;
            myEditor = editor;
            myTemplate2Argument = template2Argument;
            myFile = null;
        }

        public MyLookupAdapter(Project project, Editor editor, @Nullable PsiFile file) {
            myProject = project;
            myEditor = editor;
            myTemplate2Argument = null;
            myFile = file;
        }

        @Override
        @RequiredUIAccess
        public void itemSelected(final LookupEvent event) {
            FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.liveTemplates");
            final LookupElement item = event.getItem();
            if (item instanceof LiveTemplateLookupElementImpl liveTemplateLookupElement) {
                final Template template = liveTemplateLookupElement.getTemplate();
                final String argument = myTemplate2Argument != null ? myTemplate2Argument.get(template) : null;
                CommandProcessor.getInstance().newCommand()
                    .project(myProject)
                    .inWriteAction()
                    .run(
                        () -> ((TemplateManagerImpl)TemplateManager.getInstance(myProject))
                            .startTemplateWithPrefix(myEditor, template, null, argument)
                    );
            }
            else if (item instanceof CustomLiveTemplateLookupElement customLiveTemplateLookupElement && myFile != null) {
                CommandProcessor.getInstance().newCommand()
                    .project(myProject)
                    .inWriteAction()
                    .run(() -> customLiveTemplateLookupElement.expandTemplate(myEditor, myFile));
            }
        }
    }

    private static class TemplatesArranger extends LookupArranger {

        @Override
        public Pair<List<LookupElement>, Integer> arrangeItems(@Nonnull Lookup lookup, boolean onExplicitAction) {
            LinkedHashSet<LookupElement> result = new LinkedHashSet<>();
            List<LookupElement> items = getMatchingItems();
            for (LookupElement item : items) {
                if (item.getLookupString().startsWith(lookup.itemPattern(item))) {
                    result.add(item);
                }
            }
            result.addAll(items);
            ArrayList<LookupElement> list = new ArrayList<>(result);
            int selected = lookup.isSelectionTouched() ? list.indexOf(lookup.getCurrentItem()) : 0;
            return new Pair<>(list, selected >= 0 ? selected : 0);
        }

        @Override
        public LookupArranger createEmptyCopy() {
            return new TemplatesArranger();
        }
    }
}
