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

/*
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Aug 20, 2002
 * Time: 5:04:04 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package consulo.ide.impl.idea.codeInsight.template.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.component.util.pointer.NamedPointer;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.template.impl.LiveTemplatesConfigurable;
import consulo.ide.impl.idea.codeInsight.template.impl.TemplateListPanel;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.Language;
import consulo.language.LanguagePointerUtil;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.completion.OffsetKey;
import consulo.language.editor.impl.internal.completion.CompletionUtil;
import consulo.language.editor.impl.internal.completion.OffsetsInFile;
import consulo.language.editor.impl.internal.template.TemplateImpl;
import consulo.language.editor.impl.internal.template.TemplateManagerImpl;
import consulo.language.editor.impl.internal.template.TemplateSettingsImpl;
import consulo.language.editor.template.TemplateManager;
import consulo.language.editor.template.context.TemplateActionContext;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import java.util.*;

public class SaveAsTemplateAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(SaveAsTemplateAction.class);
    //FIXME [VISTALL] how remove this depend?
    private static final NamedPointer<Language> ourXmlLanguagePointer = LanguagePointerUtil.createPointer("XML");

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Editor editor = Objects.requireNonNull(dataContext.getData(Editor.KEY));
        PsiFile file = Objects.requireNonNull(dataContext.getData(PsiFile.KEY));

        final Project project = file.getProject();
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        final TextRange selection =
            new TextRange(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd());
        PsiElement current = file.findElementAt(selection.getStartOffset());
        int startOffset = selection.getStartOffset();
        while (current instanceof PsiWhiteSpace) {
            current = current.getNextSibling();
            if (current == null) {
                break;
            }
            startOffset = current.getTextRange().getStartOffset();
        }

        if (startOffset >= selection.getEndOffset()) {
            startOffset = selection.getStartOffset();
        }

        final PsiElement[] psiElements =
            PsiTreeUtil.collectElements(file, element -> selection.contains(element.getTextRange()) && element.getReferences().length > 0);

        final Document document = EditorFactory.getInstance().createDocument(editor.getDocument().getText().
            substring(startOffset, selection.getEndOffset()));
        final boolean isXml = file.getLanguage().is(ourXmlLanguagePointer.get());
        final int offsetDelta = startOffset;
        new WriteCommandAction.Simple(project, (String)null) {
            @Override
            @RequiredReadAction
            protected void run() throws Throwable {
                Map<RangeMarker, String> rangeToText = new HashMap<>();

                for (PsiElement element : psiElements) {
                    for (PsiReference reference : element.getReferences()) {
                        if (!(reference instanceof PsiQualifiedReference qualifiedReference) || qualifiedReference.getQualifier() == null) {
                            String canonicalText = reference.getCanonicalText();
                            TextRange referenceRange = reference.getRangeInElement();
                            final TextRange elementTextRange = element.getTextRange();
                            LOG.assertTrue(elementTextRange != null, elementTextRange);
                            final TextRange range = elementTextRange.cutOut(referenceRange).shiftRight(-offsetDelta);
                            final String oldText = document.getText(range);
                            // workaround for Java references: canonicalText contains generics, and we need to cut them off because otherwise
                            // they will be duplicated
                            int pos = canonicalText.indexOf('<');
                            if (pos > 0 && !oldText.contains("<")) {
                                canonicalText = canonicalText.substring(0, pos);
                            }
                            if (isXml) { //strip namespace prefixes
                                pos = canonicalText.lastIndexOf(':');
                                if (pos >= 0 && pos < canonicalText.length() - 1 && !oldText.contains(":")) {
                                    canonicalText = canonicalText.substring(pos + 1);
                                }
                            }
                            if (!canonicalText.equals(oldText)) {
                                rangeToText.put(document.createRangeMarker(range), canonicalText);
                            }
                        }
                    }
                }

                List<RangeMarker> markers = new ArrayList<>();
                for (RangeMarker m1 : rangeToText.keySet()) {
                    boolean nested = false;
                    for (RangeMarker m2 : rangeToText.keySet()) {
                        if (m1 != m2 && m2.getStartOffset() <= m1.getStartOffset() && m1.getEndOffset() <= m2.getEndOffset()) {
                            nested = true;
                            break;
                        }
                    }

                    if (!nested) {
                        markers.add(m1);
                    }
                }

                for (RangeMarker marker : markers) {
                    final String value = rangeToText.get(marker);
                    document.replaceString(marker.getStartOffset(), marker.getEndOffset(), value);
                }
            }
        }.execute();

        final TemplateImpl template =
            new TemplateImpl(TemplateListPanel.ABBREVIATION, document.getText(), TemplateSettingsImpl.USER_GROUP_NAME);
        template.setToReformat(true);

        OffsetKey startKey = OffsetKey.create("pivot");
        OffsetsInFile offsets = new OffsetsInFile(file);
        offsets.getOffsets().addOffset(startKey, startOffset);
        OffsetsInFile copy = TemplateManagerImpl.copyWithDummyIdentifier(
            offsets,
            editor.getSelectionModel().getSelectionStart(),
            editor.getSelectionModel().getSelectionEnd(),
            CompletionUtil.DUMMY_IDENTIFIER_TRIMMED
        );

        Set<TemplateContextType> applicable = TemplateManager.getInstance(project)
            .getApplicableContextTypes(TemplateActionContext.expanding(copy.getFile(), copy.getOffsets().getOffset(startKey)));

        for (TemplateContextType contextType : TemplateContextType.EP_NAME.getExtensionList()) {
            template.getTemplateContext().setEnabled(contextType, applicable.contains(contextType));
        }

        final LiveTemplatesConfigurable configurable = new LiveTemplatesConfigurable();
        ShowSettingsUtil.getInstance()
            .editConfigurable(project, configurable, () -> configurable.getTemplateListPanel().addTemplate(template));
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Editor editor = dataContext.getData(Editor.KEY);
        PsiFile file = dataContext.getData(PsiFile.KEY);

        if (file == null || editor == null) {
            e.getPresentation().setEnabled(false);
        }
        else {
            e.getPresentation().setEnabled(editor.getSelectionModel().hasSelection());
        }
    }
}
