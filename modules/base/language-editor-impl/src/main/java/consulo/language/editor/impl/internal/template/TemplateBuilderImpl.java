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
package consulo.language.editor.impl.internal.template;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.template.*;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.logging.Logger;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import java.util.*;

/**
 * @author mike
 */
public class TemplateBuilderImpl implements TemplateBuilder {
    private final RangeMarker myContainerElement;
    private final Map<RangeMarker, Expression> myExpressions = new HashMap<>();
    private final Map<RangeMarker, String> myVariableExpressions = new HashMap<>();
    private final Map<RangeMarker, Boolean> myAlwaysStopAtMap = new HashMap<>();
    private final Map<RangeMarker, Boolean> mySkipOnStartMap = new HashMap<>();
    private final Map<RangeMarker, String> myVariableNamesMap = new HashMap<>();
    private final Set<RangeMarker> myElements = new TreeSet<>(RangeMarker.BY_START_OFFSET);

    private RangeMarker myEndElement;
    private RangeMarker mySelection;
    private final Document myDocument;
    private final PsiFile myFile;
    private static final Logger LOG = Logger.getInstance(TemplateBuilderImpl.class);

    @RequiredReadAction
    public TemplateBuilderImpl(PsiElement element) {
        myFile = InjectedLanguageManager.getInstance(element.getProject()).getTopLevelFile(element);
        myDocument = myFile.getViewProvider().getDocument();
        myContainerElement = wrapElement(element);
    }

    @Override
    @RequiredReadAction
    public void replaceElement(PsiElement element, Expression expression, boolean alwaysStopAt) {
        RangeMarker key = wrapElement(element);
        myAlwaysStopAtMap.put(key, alwaysStopAt ? Boolean.TRUE : Boolean.FALSE);
        replaceElement(key, expression);
    }

    @RequiredReadAction
    private RangeMarker wrapElement(PsiElement element) {
        TextRange range = InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, element.getTextRange());
        return myDocument.createRangeMarker(range);
    }

    @RequiredReadAction
    private RangeMarker wrapReference(PsiReference ref) {
        PsiElement element = ref.getElement();
        return myDocument.createRangeMarker(ref.getRangeInElement()
            .shiftRight(InjectedLanguageManager.getInstance(myFile.getProject())
                .injectedToHost(element, element.getTextRange().getStartOffset())));
    }

    @Override
    @RequiredReadAction
    public void replaceElement(PsiElement element, String varName, Expression expression, boolean alwaysStopAt) {
        replaceElement(element, varName, expression, alwaysStopAt, false);
    }

    @Override
    @RequiredReadAction
    public void replaceElement(PsiReference ref, String varName, Expression expression, boolean alwaysStopAt) {
        RangeMarker key = wrapReference(ref);
        myAlwaysStopAtMap.put(key, alwaysStopAt ? Boolean.TRUE : Boolean.FALSE);
        myVariableNamesMap.put(key, varName);
        replaceElement(key, expression);
    }

    private void replaceElement(RangeMarker key, Expression expression) {
        myExpressions.put(key, expression);
        myElements.add(key);
    }

    @Override
    @RequiredReadAction
    public void replaceElement(PsiElement element, String varName, String dependantVariableName, boolean alwaysStopAt) {
        RangeMarker key = wrapElement(element);
        myAlwaysStopAtMap.put(key, alwaysStopAt ? Boolean.TRUE : Boolean.FALSE);
        myVariableNamesMap.put(key, varName);
        myVariableExpressions.put(key, dependantVariableName);
        myElements.add(key);
    }

    @Override
    @RequiredReadAction
    public void replaceElement(PsiReference ref, String varName, String dependantVariableName, boolean alwaysStopAt) {
        RangeMarker key = wrapReference(ref);
        myAlwaysStopAtMap.put(key, alwaysStopAt ? Boolean.TRUE : Boolean.FALSE);
        myVariableNamesMap.put(key, varName);
        myVariableExpressions.put(key, dependantVariableName);
        myElements.add(key);
    }

    @Override
    @RequiredReadAction
    public void replaceElement(
        PsiElement element,
        TextRange textRange,
        String primaryVariableName,
        String otherVariableName,
        boolean alwaysStopAt
    ) {
        RangeMarker key = myDocument.createRangeMarker(textRange.shiftRight(element.getTextRange().getStartOffset()));
        myAlwaysStopAtMap.put(key, alwaysStopAt ? Boolean.TRUE : Boolean.FALSE);
        myVariableNamesMap.put(key, primaryVariableName);
        myVariableExpressions.put(key, otherVariableName);
        myElements.add(key);
    }

    @Override
    @RequiredReadAction
    public void replaceElement(PsiElement element, TextRange textRange, String varName, Expression expression, boolean alwaysStopAt) {
        TextRange elementTextRange = InjectedLanguageManager.getInstance(element.getProject())
            .injectedToHost(element, element.getTextRange());
        RangeMarker key = myDocument.createRangeMarker(textRange.shiftRight(elementTextRange.getStartOffset()));
        myAlwaysStopAtMap.put(key, alwaysStopAt ? Boolean.TRUE : Boolean.FALSE);
        myVariableNamesMap.put(key, varName);
        replaceElement(key, expression);
    }

    @Override
    @RequiredReadAction
    public void replaceElement(PsiElement element, Expression expression) {
        RangeMarker key = wrapElement(element);
        replaceElement(key, expression);
    }

    @Override
    @RequiredReadAction
    public void replaceElement(PsiElement element, TextRange rangeWithinElement, Expression expression) {
        RangeMarker key = myDocument.createRangeMarker(rangeWithinElement.shiftRight(element.getTextRange().getStartOffset()));
        replaceElement(key, expression);
    }

    @Override
    public void replaceRange(TextRange rangeWithinElement, String replacementText) {
        RangeMarker key = myDocument.createRangeMarker(rangeWithinElement.shiftRight(myContainerElement.getStartOffset()));

        ConstantNode value = new ConstantNode(replacementText);
        replaceElement(key, value);
    }

    @Override
    public void replaceRange(TextRange rangeWithinElement, Expression expression) {
        RangeMarker key = myDocument.createRangeMarker(rangeWithinElement);
        replaceElement(key, expression);
    }

    /**
     * Adds end variable after the specified element
     */
    @Override
    @RequiredReadAction
    public void setEndVariableAfter(PsiElement element) {
        element = element.getNextSibling();
        setEndVariableBefore(element);
    }

    @Override
    @RequiredReadAction
    public void setEndVariableBefore(PsiElement element) {
        if (myEndElement != null) {
            myElements.remove(myEndElement);
        }
        myEndElement = wrapElement(element);
        myElements.add(myEndElement);
    }

    @RequiredReadAction
    public void setSelection(PsiElement element) {
        mySelection = wrapElement(element);
        myElements.add(mySelection);
    }

    @Override
    @RequiredWriteAction
    public Template buildInlineTemplate() {
        Template template = buildTemplate();
        template.setInline(true);

        Application.get().assertWriteAccessAllowed();

        //this is kinda hacky way of doing things, but have not got a better idea
        for (RangeMarker element : myElements) {
            if (element != myEndElement) {
                myDocument.deleteString(element.getStartOffset(), element.getEndOffset());
            }
        }

        return template;
    }

    @Override
    public Template buildTemplate() {
        Template template = TemplateBuilderFactory.getInstance().createRawTemplate("", "");

        String text = getDocumentTextFragment(myContainerElement.getStartOffset(), myContainerElement.getEndOffset());
        int containerStart = myContainerElement.getStartOffset();
        int start = 0;
        for (RangeMarker element : myElements) {
            int offset = element.getStartOffset() - containerStart;
            if (start > offset) {
                LOG.error(
                    "file: " + myFile + " container: " + myContainerElement + " markers: " +
                        StringUtil.join(
                            myElements,
                            rangeMarker -> {
                                String docString = myDocument.getText(new TextRange(
                                    rangeMarker.getStartOffset(),
                                    rangeMarker.getEndOffset()
                                ));
                                return "[[" + docString + "]" + rangeMarker.getStartOffset() + ", " + rangeMarker.getEndOffset() + "]";
                            },
                            ", "
                        )
                );
            }
            template.addTextSegment(text.substring(start, offset));

            if (element == mySelection) {
                template.addSelectionStartVariable();
                template.addTextSegment(getDocumentTextFragment(mySelection.getStartOffset(), mySelection.getEndOffset()));
                template.addSelectionEndVariable();
            }
            else if (element == myEndElement) {
                template.addEndVariable();
                start = offset;
                continue;
            }
            else {
                boolean alwaysStopAt = !Boolean.FALSE.equals(myAlwaysStopAtMap.get(element));
                Expression expression = myExpressions.get(element);
                String variableName = myVariableNamesMap.get(element);
                if (variableName == null) {
                    variableName = String.valueOf(Objects.hashCode(expression));
                }

                if (expression != null) {
                    boolean skipOnStart = Boolean.TRUE.equals(mySkipOnStartMap.get(element));
                    template.addVariable(variableName, expression, expression, alwaysStopAt, skipOnStart);
                }
                else {
                    template.addVariableSegment(variableName);
                }
            }

            start = element.getEndOffset() - containerStart;
        }

        template.addTextSegment(text.substring(start));

        for (RangeMarker element : myElements) {
            String dependantVariable = myVariableExpressions.get(element);
            if (dependantVariable == null) {
                continue;
            }
            boolean alwaysStopAt = !Boolean.FALSE.equals(myAlwaysStopAtMap.get(element));
            Expression expression = myExpressions.get(element);
            String variableName = myVariableNamesMap.get(element);
            if (variableName == null) {
                variableName = String.valueOf(Objects.hashCode(expression));
            }

            template.addVariable(variableName, dependantVariable, dependantVariable, alwaysStopAt);
        }

        template.setToIndent(false);
        template.setToReformat(false);

        return template;
    }

    private String getDocumentTextFragment(int startOffset, int endOffset) {
        return myDocument.getCharsSequence().subSequence(startOffset, endOffset).toString();
    }

    @Override
    @RequiredReadAction
    public void replaceElement(PsiElement element, String replacementText) {
        replaceElement(element, new ConstantNode(replacementText));
    }

    @Override
    @RequiredReadAction
    public void replaceElement(PsiElement element, TextRange rangeWithinElement, String replacementText) {
        RangeMarker key = myDocument.createRangeMarker(rangeWithinElement.shiftRight(element.getTextRange().getStartOffset()));
        ConstantNode value = new ConstantNode(replacementText);
        replaceElement(key, value);
    }

    @Override
    @RequiredWriteAction
    public void run() {
        Project project = myFile.getProject();
        VirtualFile file = myFile.getVirtualFile();
        assert file != null;
        OpenFileDescriptor descriptor = OpenFileDescriptorFactory.getInstance(project).builder(file).build();
        Editor editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true);

        assert editor != null : "Editor is null";
        run(editor, false);
    }

    @Override
    @RequiredWriteAction
    public void run(Editor editor, boolean inline) {
        Template template = inline ? buildInlineTemplate() : buildTemplate();

        editor.getDocument().replaceString(myContainerElement.getStartOffset(), myContainerElement.getEndOffset(), "");
        editor.getCaretModel().moveToOffset(myContainerElement.getStartOffset());

        TemplateManager.getInstance(myFile.getProject()).startTemplate(editor, template);
    }

    @Override
    @RequiredReadAction
    public void replaceElement(PsiElement element, String varName, Expression expression, boolean alwaysStopAt, boolean skipOnStart) {
        RangeMarker key = wrapElement(element);
        myAlwaysStopAtMap.put(key, alwaysStopAt);
        myVariableNamesMap.put(key, varName);
        mySkipOnStartMap.put(key, skipOnStart);
        replaceElement(key, expression);
    }
}
