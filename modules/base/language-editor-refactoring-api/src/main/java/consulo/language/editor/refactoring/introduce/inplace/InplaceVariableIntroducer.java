/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.introduce.inplace;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Result;
import consulo.codeEditor.Editor;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenSeparatorGenerator;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.refactoring.rename.NameSuggestionProvider;
import consulo.language.editor.refactoring.rename.PreferrableNameSuggestionProvider;
import consulo.language.editor.refactoring.rename.SuggestedNameInfo;
import consulo.language.editor.refactoring.rename.inplace.InplaceRefactoring;
import consulo.language.editor.refactoring.rename.inplace.MyLookupExpression;
import consulo.language.editor.template.ExpressionContext;
import consulo.language.editor.template.TemplateManager;
import consulo.language.editor.template.TemplateState;
import consulo.language.editor.template.TextResult;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.internal.StartMarkAction;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;

import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * User: anna
 * Date: 3/15/11
 */
public abstract class InplaceVariableIntroducer<E extends PsiElement> extends InplaceRefactoring {
    protected E myExpr;
    protected RangeMarker myExprMarker;

    protected E[] myOccurrences;
    protected List<RangeMarker> myOccurrenceMarkers;

    @RequiredUIAccess
    public InplaceVariableIntroducer(
        PsiNamedElement elementToRename,
        Editor editor,
        Project project,
        String title,
        E[] occurrences,
        @Nullable E expr
    ) {
        super(editor, elementToRename, project);
        myTitle = title;
        myOccurrences = occurrences;
        if (expr != null) {
            final ASTNode node = expr.getNode();
            final ASTNode astNode =
                TokenSeparatorGenerator.forLanguage(expr.getLanguage()).generateWhitespaceBetweenTokens(node.getTreePrev(), node);
            if (astNode != null) {
                new WriteCommandAction<Object>(project, "Normalize declaration") {
                    @Override
                    protected void run(Result<Object> result) throws Throwable {
                        node.getTreeParent().addChild(astNode, node);
                    }
                }.execute();
            }
            myExpr = expr;
        }
        myExprMarker = myExpr != null && myExpr.isPhysical() ? createMarker(myExpr) : null;
        initOccurrencesMarkers();
    }

    @Override
    protected boolean shouldSelectAll() {
        return true;
    }

    @Override
    @RequiredUIAccess
    protected StartMarkAction startRename() throws StartMarkAction.AlreadyStartedException {
        return null;
    }

    public void setOccurrenceMarkers(List<RangeMarker> occurrenceMarkers) {
        myOccurrenceMarkers = occurrenceMarkers;
    }

    public void setExprMarker(RangeMarker exprMarker) {
        myExprMarker = exprMarker;
    }

    public E getExpr() {
        return myExpr != null && myExpr.isValid() && myExpr.isPhysical() ? myExpr : null;
    }

    public E[] getOccurrences() {
        return myOccurrences;
    }

    @RequiredUIAccess
    public List<RangeMarker> getOccurrenceMarkers() {
        if (myOccurrenceMarkers == null) {
            initOccurrencesMarkers();
        }
        return myOccurrenceMarkers;
    }

    @RequiredReadAction
    protected void initOccurrencesMarkers() {
        if (myOccurrenceMarkers != null) {
            return;
        }
        myOccurrenceMarkers = new ArrayList<>();
        for (E occurrence : myOccurrences) {
            myOccurrenceMarkers.add(createMarker(occurrence));
        }
    }

    @RequiredReadAction
    protected RangeMarker createMarker(PsiElement element) {
        return myEditor.getDocument().createRangeMarker(element.getTextRange());
    }


    public RangeMarker getExprMarker() {
        return myExprMarker;
    }

    @Override
    protected boolean performRefactoring() {
        return false;
    }

    @Override
    protected void collectAdditionalElementsToRename(List<Pair<PsiElement, TextRange>> stringUsages) {
    }

    @Override
    protected String getCommandName() {
        return myTitle;
    }

    @Override
    protected void moveOffsetAfter(boolean success) {
        super.moveOffsetAfter(success);
        if (myOccurrenceMarkers != null) {
            for (RangeMarker marker : myOccurrenceMarkers) {
                marker.dispose();
            }
        }
        if (myExprMarker != null && !isRestart()) {
            myExprMarker.dispose();
        }
    }


    @Override
    @RequiredReadAction
    protected MyLookupExpression createLookupExpression(PsiElement selectedElement) {
        return new MyIntroduceLookupExpression(
            getInitialName(),
            myNameSuggestions,
            myElementToRename,
            shouldSelectAll(),
            myAdvertisementText
        );
    }

    private static class MyIntroduceLookupExpression extends MyLookupExpression {
        private final SmartPsiElementPointer<PsiNamedElement> myPointer;

        public MyIntroduceLookupExpression(
            final String initialName,
            final LinkedHashSet<String> names,
            final PsiNamedElement elementToRename,
            final boolean shouldSelectAll,
            final String advertisementText
        ) {
            super(initialName, names, elementToRename, elementToRename, shouldSelectAll, advertisementText);
            myPointer = SmartPointerManager.getInstance(elementToRename.getProject()).createSmartPsiElementPointer(elementToRename);
        }

        @Override
        @RequiredReadAction
        public LookupElement[] calculateLookupItems(ExpressionContext context) {
            return createLookupItems(myName, context.getEditor(), getElement());
        }

        @Nullable
        @RequiredReadAction
        public PsiNamedElement getElement() {
            return myPointer.getElement();
        }

        @Nullable
        private LookupElement[] createLookupItems(String name, Editor editor, PsiNamedElement psiVariable) {
            if (psiVariable == null) {
                return myLookupItems;
            }

            TemplateState templateState = TemplateManager.getInstance(psiVariable.getProject()).getTemplateState(editor);
            final TextResult insertedValue = templateState != null ? templateState.getVariableValue(PRIMARY_VARIABLE_NAME) : null;
            if (insertedValue != null) {
                final String text = insertedValue.getText();
                if (!text.isEmpty() && !Comparing.strEqual(text, name)) {
                    final LinkedHashSet<String> names = new LinkedHashSet<>();
                    names.add(text);
                    for (NameSuggestionProvider provider : NameSuggestionProvider.EP_NAME.getExtensionList()) {
                        final SuggestedNameInfo suggestedNameInfo = provider.getSuggestedNames(psiVariable, psiVariable, names);
                        if (suggestedNameInfo != null
                            && provider instanceof PreferrableNameSuggestionProvider nameSuggestionProvider
                            && !nameSuggestionProvider.shouldCheckOthers()) {
                            break;
                        }
                    }
                    final LookupElement[] items = new LookupElement[names.size()];
                    final Iterator<String> iterator = names.iterator();
                    for (int i = 0; i < items.length; i++) {
                        items[i] = LookupElementBuilder.create(iterator.next());
                    }
                    return items;
                }
            }
            return myLookupItems;
        }
    }
}
