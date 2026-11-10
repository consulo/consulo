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
package consulo.language.editor.refactoring.ui;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.fileEditor.FileEditorLocation;
import consulo.language.editor.highlight.ReadWriteAccessDetector;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.ui.image.Image;
import consulo.usage.*;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

/**
 * Shows the conflicts of a refactoring in the usage view, offering to retry the refactoring from there.
 *
 * @author Jeka
 */
public final class ConflictsUsageView {
    private final MultiMap<PsiElement, LocalizeValue> myElementConflictDescription;

    private ConflictsUsageView(MultiMap<PsiElement, LocalizeValue> elementConflictDescription) {
        myElementConflictDescription = elementConflictDescription;
    }

    @RequiredReadAction
    public static void show(
        Project project,
        MultiMap<PsiElement, LocalizeValue> elementConflictDescription,
        @Nullable Runnable doRefactoringRunnable,
        @Nullable LocalizeValue commandName
    ) {
        new ConflictsUsageView(elementConflictDescription).show(
            project,
            doRefactoringRunnable,
            commandName == null ? LocalizeValue.empty() : commandName
        );
    }

    @RequiredReadAction
    private void show(Project project, @Nullable Runnable doRefactoringRunnable, LocalizeValue commandName) {
        UsageViewPresentation presentation = new UsageViewPresentation();
        String codeUsagesString = "Conflicts";
        presentation.setCodeUsagesString(codeUsagesString);
        presentation.setTabName(codeUsagesString);
        presentation.setTabText(codeUsagesString);
        presentation.setShowCancelButton(true);

        Usage[] usages = new Usage[myElementConflictDescription.size()];
        int i = 0;
        ExtensionPoint<ReadWriteAccessDetector> rwDetectors = Application.get().getExtensionPoint(ReadWriteAccessDetector.class);
        for (PsiElement element : myElementConflictDescription.keySet()) {
            if (element == null) {
                usages[i++] = new DescriptionOnlyUsage();
                continue;
            }
            ReadWriteAccessDetector.Access access = rwDetectors.computeSafeIfAny(
                detector -> detector.isReadWriteAccessible(element) ? detector.getExpressionAccess(element) : null
            );
            boolean isRead = false;
            boolean isWrite = false;
            if (access != null) {
                isRead = access != ReadWriteAccessDetector.Access.Write;
                isWrite = access != ReadWriteAccessDetector.Access.Read;
            }

            usages[i++] = isRead || isWrite
                ? new ReadWriteAccessUsageInfo2UsageAdapter(new UsageInfo(element), isRead, isWrite) {

                @Override
                public UsagePresentation getPresentation() {
                    UsagePresentation usagePresentation = super.getPresentation();
                    return ConflictsUsageView.this.getPresentation(usagePresentation, element);
                }
            }
                : new UsageInfo2UsageAdapter(new UsageInfo(element)) {

                @Override
                public UsagePresentation getPresentation() {
                    UsagePresentation usagePresentation = super.getPresentation();
                    return ConflictsUsageView.this.getPresentation(usagePresentation, element);
                }
            };
        }
        UsageView usageView = UsageViewManager.getInstance(project).showUsages(UsageTarget.EMPTY_ARRAY, usages, presentation);
        if (doRefactoringRunnable != null) {
            usageView.addPerformOperationAction(
                doRefactoringRunnable,
                commandName.orIfEmpty(RefactoringLocalize.retryCommand()),
                LocalizeValue.localizeTODO("Unable to perform refactoring. There were changes in code after the usages have been found."),
                RefactoringLocalize.usageviewDoaction()
            );
        }
    }

    private UsagePresentation getPresentation(UsagePresentation usagePresentation, PsiElement element) {
        Collection<LocalizeValue> elementConflicts = new LinkedHashSet<>(myElementConflictDescription.get(element));
        String conflictDescription =
            " (" + Pattern.compile("<[^<>]*>").matcher(StringUtil.join(elementConflicts, "\n")).replaceAll("") + ")";
        return new UsagePresentation() {
            @Override
            public TextChunk[] getText() {
                TextChunk[] chunks = usagePresentation.getText();
                return ArrayUtil.append(
                    chunks,
                    new TextChunk(
                        TextAttributesUtil.toTextAttributes(SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES),
                        conflictDescription
                    )
                );
            }

            @Override
            public String getPlainText() {
                return usagePresentation.getPlainText() + conflictDescription;
            }

            @Override
            public Image getIcon() {
                return usagePresentation.getIcon();
            }

            @Override
            public String getTooltipText() {
                return usagePresentation.getTooltipText();
            }
        };
    }

    private class DescriptionOnlyUsage implements Usage {
        private final String myConflictDescription = Pattern.compile("<[^<>]*>")
            .matcher(StringUtil.join(new LinkedHashSet<>(myElementConflictDescription.get(null)), "\n"))
            .replaceAll("");

        @Override
        public UsagePresentation getPresentation() {
            return new UsagePresentation() {
                @Override
                public TextChunk[] getText() {
                    return new TextChunk[0];
                }

                @Override
                public @Nullable Image getIcon() {
                    return null;
                }

                @Override
                public String getTooltipText() {
                    return myConflictDescription;
                }

                @Override
                public String getPlainText() {
                    return myConflictDescription;
                }
            };
        }

        @Override
        @RequiredReadAction
        public boolean canNavigateToSource() {
            return false;
        }

        @Override
        @RequiredReadAction
        public boolean canNavigate() {
            return false;
        }

        @Override
        @RequiredUIAccess
        public void navigate(boolean requestFocus) {
        }

        @Override
        public FileEditorLocation getLocation() {
            return null;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void selectInEditor() {
        }

        @Override
        public void highlightInEditor() {
        }
    }
}
