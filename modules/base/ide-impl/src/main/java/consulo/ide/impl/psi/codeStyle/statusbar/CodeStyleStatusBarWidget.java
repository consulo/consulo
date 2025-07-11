// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.codeStyle.statusbar;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import consulo.language.codeStyle.CodeStyleSettingsModifier;
import consulo.language.codeStyle.TransientCodeStyleSettings;
import consulo.language.codeStyle.*;
import consulo.language.codeStyle.event.CodeStyleSettingsChangeEvent;
import consulo.language.codeStyle.event.CodeStyleSettingsListener;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static consulo.language.codeStyle.CommonCodeStyleSettings.IndentOptions;

public class CodeStyleStatusBarWidget extends EditorBasedStatusBarPopup implements CodeStyleSettingsListener {
    private CodeStyleStatusBarPanel myPanel;

    public CodeStyleStatusBarWidget(@Nonnull Project project, @Nonnull StatusBarWidgetFactory factory) {
        super(project, factory, true);
    }

    @Nonnull
    @Override
    protected WidgetState getWidgetState(@Nullable VirtualFile file) {
        if (file == null) {
            return WidgetState.HIDDEN;
        }
        PsiFile psiFile = getPsiFile();
        if (psiFile == null) {
            return WidgetState.HIDDEN;
        }
        CodeStyleSettings settings = CodeStyle.getSettings(psiFile);
        IndentOptions indentOptions = CodeStyle.getIndentOptions(psiFile);
        if (settings instanceof TransientCodeStyleSettings codeStyleSettings) {
            return createWidgetState(psiFile, indentOptions, getUiContributor(codeStyleSettings));
        }
        else {
            return createWidgetState(psiFile, indentOptions, getUiContributor(file, indentOptions));
        }
    }


    @Nullable
    private static CodeStyleStatusBarUIContributor getUiContributor(@Nonnull TransientCodeStyleSettings settings) {
        CodeStyleSettingsModifier modifier = settings.getModifier();
        return modifier != null ? modifier.getStatusBarUiContributor(settings) : null;
    }


    @Nullable
    private static IndentStatusBarUIContributor getUiContributor(@Nonnull VirtualFile file, @Nonnull IndentOptions indentOptions) {
        FileIndentOptionsProvider provider = findProvider(file, indentOptions);
        return provider != null ? provider.getIndentStatusBarUiContributor(indentOptions) : null;
    }

    @Nullable
    private static FileIndentOptionsProvider findProvider(@Nonnull VirtualFile file, @Nonnull IndentOptions indentOptions) {
        FileIndentOptionsProvider optionsProvider = indentOptions.getFileIndentOptionsProvider();
        if (optionsProvider != null) {
            return optionsProvider;
        }
        for (FileIndentOptionsProvider provider : FileIndentOptionsProvider.EP_NAME.getExtensionList()) {
            IndentStatusBarUIContributor uiContributor = provider.getIndentStatusBarUiContributor(indentOptions);
            if (uiContributor != null && uiContributor.areActionsAvailable(file)) {
                return provider;
            }
        }
        return null;
    }

    private static WidgetState createWidgetState(
        @Nonnull PsiFile psiFile,
        @Nonnull IndentOptions indentOptions,
        @Nullable CodeStyleStatusBarUIContributor uiContributor
    ) {
        if (uiContributor != null) {
            return new MyWidgetState(
                uiContributor.getTooltip(),
                uiContributor.getStatusText(psiFile),
                psiFile,
                indentOptions,
                uiContributor
            );
        }
        else {
            LocalizeValue indentInfo = IndentStatusBarUIContributor.getIndentInfo(indentOptions);
            String tooltip = IndentStatusBarUIContributor.createTooltip(indentInfo.get(), null);
            return new MyWidgetState(tooltip, indentInfo.get(), psiFile, indentOptions, null);
        }
    }

    @Nullable
    private PsiFile getPsiFile() {
        Editor editor = getEditor();
        Project project = getProject();
        if (editor != null && !project.isDisposed()) {
            return PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        }
        return null;
    }

    @Nullable
    @Override
    protected ListPopup createPopup(DataContext context) {
        WidgetState state = getWidgetState(context.getData(VirtualFile.KEY));
        Editor editor = getEditor();
        PsiFile psiFile = getPsiFile();
        if (state instanceof MyWidgetState widgetState && editor != null && psiFile != null) {
            CodeStyleStatusBarUIContributor uiContributor = widgetState.getContributor();
            AnAction[] actions = getActions(uiContributor, psiFile);
            ActionGroup actionGroup = new ActionGroup() {
                @Override
                @Nonnull
                public AnAction[] getChildren(@Nullable AnActionEvent e) {
                    return actions;
                }
            };
            return JBPopupFactory.getInstance().createActionGroupPopup(
                uiContributor != null ? uiContributor.getActionGroupTitle() : null,
                actionGroup,
                context,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
        }
        return null;
    }

    @Nonnull
    private static AnAction[] getActions(@Nullable CodeStyleStatusBarUIContributor uiContributor, @Nonnull PsiFile psiFile) {
        List<AnAction> allActions = new ArrayList<>();
        if (uiContributor != null) {
            AnAction[] actions = uiContributor.getActions(psiFile);
            if (actions != null) {
                allActions.addAll(Arrays.asList(actions));
            }
        }
        if (uiContributor == null
            || uiContributor instanceof IndentStatusBarUIContributor iUIContributor && iUIContributor.isShowFileIndentOptionsEnabled()) {
            allActions.add(CodeStyleStatusBarWidgetFactory.createDefaultIndentConfigureAction(psiFile));
        }
        if (uiContributor != null) {
            AnAction disabledAction = uiContributor.createDisableAction(psiFile.getProject());
            if (disabledAction != null) {
                allActions.add(disabledAction);
            }
            AnAction showAllAction = uiContributor.createShowAllAction(psiFile.getProject());
            if (showAllAction != null) {
                allActions.add(showAllAction);
            }
        }
        return allActions.toArray(AnAction.EMPTY_ARRAY);
    }

    @Override
    protected void registerCustomListeners() {
        Project project = getProject();
        ReadAction.nonBlocking(() -> CodeStyleSettingsManager.getInstance(project))
            .expireWith(project)
            .finishOnUiThread(Application::getAnyModalityState, manager -> manager.addListener(this, this))
            .submitDefault();
    }

    @Override
    public void codeStyleSettingsChanged(@Nonnull CodeStyleSettingsChangeEvent event) {
        update();
    }

    @Nonnull
    @Override
    protected StatusBarWidget createInstance(@Nonnull Project project) {
        return new CodeStyleStatusBarWidget(project, myFactory);
    }

    private static class MyWidgetState extends WidgetState {
        @Nonnull
        private final IndentOptions myIndentOptions;
        @Nullable
        private final CodeStyleStatusBarUIContributor myContributor;
        @Nonnull
        private final PsiFile myPsiFile;

        protected MyWidgetState(
            String toolTip,
            String text,
            @Nonnull PsiFile psiFile,
            @Nonnull IndentOptions indentOptions,
            @Nullable CodeStyleStatusBarUIContributor uiContributor
        ) {
            super(toolTip, text, true);
            myIndentOptions = indentOptions;
            myContributor = uiContributor;
            myPsiFile = psiFile;
            if (uiContributor != null) {
                setIcon(uiContributor.getIcon());
            }
        }

        @Nullable
        public CodeStyleStatusBarUIContributor getContributor() {
            return myContributor;
        }

        @Nonnull
        public IndentOptions getIndentOptions() {
            return myIndentOptions;
        }

        @Nonnull
        public PsiFile getPsiFile() {
            return myPsiFile;
        }
    }

    @Override
    protected JPanel createComponent() {
        myPanel = new CodeStyleStatusBarPanel();
        return myPanel;
    }

    @Override
    protected void updateComponent(@Nonnull WidgetState state) {
        updateToolTip(state);

        myPanel.setIcon(state.getIcon());
        myPanel.setText(state.getText());
    }

    @Override
    protected boolean isEmpty() {
        return StringUtil.isEmpty(myPanel.getText());
    }
}
