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

/**
 * created at Sep 12, 2001
 * @author Jeka
 */
package consulo.language.editor.refactoring.ui;

import consulo.fileEditor.FileEditorLocation;
import consulo.language.editor.highlight.ReadWriteAccessDetector;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.ui.image.Image;
import consulo.usage.*;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

public class ConflictsDialog extends DialogWrapper{
  private static final int SHOW_CONFLICTS_EXIT_CODE = 4;

  private String[] myConflictDescriptions;
  private MultiMap<PsiElement, String> myElementConflictDescription;
  private final Project myProject;
  private Runnable myDoRefactoringRunnable;
  private final boolean myCanShowConflictsInView;
  private String myCommandName;

  public ConflictsDialog(@Nonnull Project project, @Nonnull MultiMap<PsiElement, String> conflictDescriptions) {
    this(project, conflictDescriptions, null, true, true);
  }

  public ConflictsDialog(@Nonnull Project project,
                         @Nonnull MultiMap<PsiElement, String> conflictDescriptions,
                         @Nullable Runnable doRefactoringRunnable) {
    this(project, conflictDescriptions, doRefactoringRunnable, true, true);
  }

  public ConflictsDialog(@Nonnull Project project,
                         @Nonnull MultiMap<PsiElement, String> conflictDescriptions,
                         @Nullable Runnable doRefactoringRunnable,
                         boolean alwaysShowOkButton,
                         boolean canShowConflictsInView) {
    super(project, true);
    myProject = project;
    myDoRefactoringRunnable = doRefactoringRunnable;
    myCanShowConflictsInView = canShowConflictsInView;
    final LinkedHashSet<String> conflicts = new LinkedHashSet<String>();

    for (String conflict : conflictDescriptions.values()) {
      conflicts.add(conflict);
    }
    myConflictDescriptions = ArrayUtil.toStringArray(conflicts);
    myElementConflictDescription = conflictDescriptions;
    setTitle(RefactoringLocalize.problemsDetectedTitle());
    setOKButtonText(RefactoringLocalize.continueButton());
    setOKActionEnabled(alwaysShowOkButton || myDoRefactoringRunnable != null);
    init();
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  public ConflictsDialog(Project project, Collection<String> conflictDescriptions) {
    this(project, ArrayUtil.toStringArray(conflictDescriptions));
  }

  @Deprecated
  public ConflictsDialog(Project project, String... conflictDescriptions) {
    super(project, true);
    myProject = project;
    myConflictDescriptions = conflictDescriptions;
    myCanShowConflictsInView = true;
    setTitle(RefactoringLocalize.problemsDetectedTitle());
    setOKButtonText(RefactoringLocalize.continueButton());
    init();
  }

  @Override
  @Nonnull
  protected Action[] createActions(){
    final Action okAction = getOKAction();
    boolean showUsagesButton = myElementConflictDescription != null && myCanShowConflictsInView;

    if (showUsagesButton || !okAction.isEnabled()) {
      okAction.putValue(DEFAULT_ACTION, null);
    }

    if (!showUsagesButton) {
      return new Action[]{okAction,new CancelAction()};
    }
    return new Action[]{okAction, new MyShowConflictsInUsageViewAction(), new CancelAction()};
  }

  public boolean isShowConflicts() {
    return getExitCode() == SHOW_CONFLICTS_EXIT_CODE;
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout(0, 2));

    panel.add(new JLabel(RefactoringLocalize.theFollowingProblemsWereFound().get()), BorderLayout.NORTH);

    @NonNls StringBuilder buf = new StringBuilder();
    for (String description : myConflictDescriptions) {
      buf.append(description);
      buf.append("<br><br>");
    }
    JEditorPane messagePane = new JEditorPane(UIUtil.HTML_MIME, buf.toString());
    messagePane.setEditable(false);
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(
      messagePane,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    );
    scrollPane.setPreferredSize(new Dimension(500, 400));
    panel.add(scrollPane, BorderLayout.CENTER);

    if (getOKAction().isEnabled()) {
      panel.add(new JLabel(RefactoringLocalize.doYouWishToIgnoreThemAndContinue().get()), BorderLayout.SOUTH);
    }

    return panel;
  }

  public void setCommandName(String commandName) {
    myCommandName = commandName;
  }

  private class CancelAction extends AbstractAction {
    public CancelAction() {
      super(RefactoringLocalize.cancelButton().get());
      putValue(DEFAULT_ACTION,Boolean.TRUE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      doCancelAction();
    }
  }

  private class MyShowConflictsInUsageViewAction extends AbstractAction {


    public MyShowConflictsInUsageViewAction() {
      super("Show conflicts in view");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      final UsageViewPresentation presentation = new UsageViewPresentation();
      final String codeUsagesString = "Conflicts";
      presentation.setCodeUsagesString(codeUsagesString);
      presentation.setTabName(codeUsagesString);
      presentation.setTabText(codeUsagesString);
      presentation.setShowCancelButton(true);

      final Usage[] usages = new Usage[myElementConflictDescription.size()];
      int i = 0;
      for (final PsiElement element : myElementConflictDescription.keySet()) {
        if (element == null) {
          usages[i++] = new DescriptionOnlyUsage();
          continue;
        }
        boolean isRead = false;
        boolean isWrite = false;
        for (ReadWriteAccessDetector detector : ReadWriteAccessDetector.EP_NAME.getExtensionList()) {
          if (detector.isReadWriteAccessible(element)) {
            final ReadWriteAccessDetector.Access access = detector.getExpressionAccess(element);
            isRead = access != ReadWriteAccessDetector.Access.Write;
            isWrite = access != ReadWriteAccessDetector.Access.Read;
            break;
          }
        }

        usages[i++] = isRead || isWrite ? new ReadWriteAccessUsageInfo2UsageAdapter(new UsageInfo(element), isRead, isWrite) {
          @Nonnull
          @Override
          public UsagePresentation getPresentation() {
            final UsagePresentation usagePresentation = super.getPresentation();
            return MyShowConflictsInUsageViewAction.this.getPresentation(usagePresentation, element);
          }
        } : new UsageInfo2UsageAdapter(new UsageInfo(element)) {
          @Nonnull
          @Override
          public UsagePresentation getPresentation() {
            final UsagePresentation usagePresentation = super.getPresentation();
            return MyShowConflictsInUsageViewAction.this.getPresentation(usagePresentation, element);
          }
        };
      }
      final UsageView usageView = UsageViewManager.getInstance(myProject).showUsages(UsageTarget.EMPTY_ARRAY, usages, presentation);
      if (myDoRefactoringRunnable != null) {
        usageView.addPerformOperationAction(
          myDoRefactoringRunnable,
          myCommandName != null
            ? myCommandName
            : RefactoringLocalize.retryCommand().get(),
          "Unable to perform refactoring. There were changes in code after the usages have been found.",
          RefactoringLocalize.usageviewDoaction().get()
        );
      }
      close(SHOW_CONFLICTS_EXIT_CODE);
    }

    private UsagePresentation getPresentation(final UsagePresentation usagePresentation, PsiElement element) {
      final Collection<String> elementConflicts = new LinkedHashSet<String>(myElementConflictDescription.get(element));
      final String conflictDescription = " (" + Pattern.compile("<[^<>]*>").matcher(StringUtil.join(elementConflicts, "\n")).replaceAll("") + ")";
      return new UsagePresentation() {
        @Override
        @Nonnull
        public TextChunk[] getText() {
          final TextChunk[] chunks = usagePresentation.getText();
          return ArrayUtil
            .append(chunks, new TextChunk(TextAttributesUtil.toTextAttributes(SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES), conflictDescription));
        }

        @Override
        @Nonnull
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
      private final String myConflictDescription = Pattern.compile("<[^<>]*>").matcher(StringUtil.join(new LinkedHashSet<String>(myElementConflictDescription.get(null)), "\n")).replaceAll("");

      @Override
      @Nonnull
      public UsagePresentation getPresentation() {
        return new UsagePresentation() {
          @Override
          @Nonnull
          public TextChunk[] getText() {
            return new TextChunk[0];
          }

          @Override
          @Nullable
          public Image getIcon() {
            return null;
          }

          @Override
          public String getTooltipText() {
            return myConflictDescription;
          }

          @Override
          @Nonnull
          public String getPlainText() {
            return myConflictDescription;
          }
        };
      }

      @Override
      public boolean canNavigateToSource() {
        return false;
      }

      @Override
      public boolean canNavigate() {
        return false;
      }
      @Override
      public void navigate(boolean requestFocus) {}

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
      public void selectInEditor() {}
      @Override
      public void highlightInEditor() {}
    }
  }
}
