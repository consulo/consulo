// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.actionSystem.AlwaysPerformingActionGroup;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.popup.PopupFactoryImpl;
import consulo.language.localize.LanguageLocalize;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.ErrorLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.popup.PopupListElementRenderer;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.MnemonicNavigationFilter;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

import static consulo.ui.ex.awt.UIUtil.DEFAULT_HGAP;

public class CopyReferencePopup extends NonTrivialActionGroup implements AlwaysPerformingActionGroup {
    private static final Logger LOG = Logger.getInstance(CopyReferencePopup.class);
    private static final int DEFAULT_WIDTH = JBUIScale.scale(500);

    @Override
    public boolean canBePerformed(@Nonnull DataContext context) {
        return true;
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        DataContext dataContext = DataContext.builder().addAll(
            e.getDataContext(),
            PsiElement.KEY,
            Project.KEY,
            PsiElement.KEY_OF_ARRAY,
            VirtualFile.KEY_OF_ARRAY,
            Editor.KEY
        ).build();
        BasePresentationFactory factory = new BasePresentationFactory();
        ListPopup popup = new PopupFactoryImpl.ActionGroupPopup(
            LanguageLocalize.popupTitleCopy().get(),
            this,
            e.getDataContext(),
            false,
            true,
            false,
            true,
            null,
            -1,
            null,
            ActionPlaces.COPY_REFERENCE_POPUP,
            factory,
            false
        ) {
            @Override
            protected ListCellRenderer<PopupFactoryImpl.ActionItem> getListElementRenderer() {
                return new PopupListElementRenderer<>(this) {
                    private JLabel myInfoLabel;
                    private JLabel myShortcutLabel;

                    @Override
                    protected JComponent createItemComponent() {
                        myTextLabel = new ErrorLabel();
                        myTextLabel.setBorder(JBUI.Borders.empty(1));

                        myInfoLabel = new JLabel();
                        myInfoLabel.setBorder(JBUI.Borders.empty(1, DEFAULT_HGAP, 1, 1));

                        myShortcutLabel = new JLabel();
                        myShortcutLabel.setBorder(JBUI.Borders.emptyLeft(DEFAULT_HGAP));
                        myShortcutLabel.setForeground(UIUtil.getContextHelpForeground());

                        JPanel textPanel = new JPanel(new BorderLayout());
                        JPanel titlePanel = new JPanel(new BorderLayout());
                        titlePanel.add(myTextLabel, BorderLayout.WEST);
                        titlePanel.add(myShortcutLabel, BorderLayout.CENTER);

                        textPanel.add(titlePanel, BorderLayout.WEST);
                        textPanel.add(myInfoLabel, BorderLayout.CENTER);
                        return layoutComponent(textPanel);
                    }

                    @Override
                    @RequiredReadAction
                    protected void customizeComponent(
                        @Nonnull JList<? extends PopupFactoryImpl.ActionItem> list,
                        @Nonnull PopupFactoryImpl.ActionItem actionItem,
                        boolean isSelected
                    ) {
                        Presentation presentation = factory.getPresentation(actionItem.getAction());

                        AnAction action = actionItem.getAction();
                        Editor editor = dataContext.getData(Editor.KEY);
                        String qualifiedName = presentation.getClientProperty(CopyPathProvider.QUALIFIED_NAME);

                        myInfoLabel.setText("");
                        if (qualifiedName != null) {
                            myInfoLabel.setText(qualifiedName);
                        }
                        Color foreground = isSelected ? UIUtil.getListSelectionForeground(true) : UIUtil.getInactiveTextColor();
                        myInfoLabel.setForeground(foreground);
                        myShortcutLabel.setForeground(foreground);

                        MnemonicNavigationFilter<Object> filter = myStep.getMnemonicNavigationFilter();
                        int pos = filter == null ? -1 : filter.getMnemonicPos(actionItem);
                        if (pos != -1) {
                            String text = myTextLabel.getText();
                            text = text.substring(0, pos) + text.substring(pos + 1);
                            myTextLabel.setText(text);
                            myTextLabel.setDisplayedMnemonicIndex(pos);
                        }

                        if (action instanceof CopyPathProvider) {
                            Shortcut shortcut = ArrayUtil.getFirstElement(action.getShortcutSet().getShortcuts());
                            myShortcutLabel.setText(shortcut != null ? KeymapUtil.getShortcutText(shortcut) : null);
                        }
                    }
                };
            }

            @Override
            protected boolean isResizable() {
                return true;
            }
        };

        updatePopupSize(popup);

        popup.showInBestPositionFor(e.getDataContext());
    }

    private static void updatePopupSize(@Nonnull ListPopup popup) {
        Application.get().invokeLater(() -> {
            popup.getContent().setPreferredSize(new Dimension(DEFAULT_WIDTH, popup.getContent().getPreferredSize().height));
            popup.getContent().setSize(new Dimension(DEFAULT_WIDTH, popup.getContent().getPreferredSize().height));
            popup.setSize(popup.getContent().getPreferredSize());
        });
    }
}