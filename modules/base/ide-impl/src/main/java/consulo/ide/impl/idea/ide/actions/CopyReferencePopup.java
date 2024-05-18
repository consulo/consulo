// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.language.LangBundle;
import consulo.ide.impl.idea.openapi.actionSystem.*;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.*;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.MnemonicNavigationFilter;
import consulo.dataContext.DataContext;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.awt.ErrorLabel;
import consulo.ide.impl.idea.ui.popup.PopupFactoryImpl;
import consulo.ide.impl.idea.ui.popup.list.PopupListElementRenderer;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

import static consulo.language.editor.CommonDataKeys.*;
import static consulo.language.editor.LangDataKeys.PSI_ELEMENT_ARRAY;
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
    DataContext dataContext = SimpleDataContext.builder().addAll(e.getDataContext(), PSI_ELEMENT, PROJECT, PSI_ELEMENT_ARRAY, VIRTUAL_FILE_ARRAY, EDITOR).build();
    String popupPlace = ActionPlaces.getActionGroupPopupPlace(getClass().getSimpleName());
    ListPopup popup = new PopupFactoryImpl.ActionGroupPopup(LangBundle.message("popup.title.copy"), this, e.getDataContext(), true, true, false, true, null, -1, null, popupPlace) {
      @Override
      protected ListCellRenderer<PopupFactoryImpl.ActionItem> getListElementRenderer() {
        return new PopupListElementRenderer<PopupFactoryImpl.ActionItem>(this) {
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
          protected void customizeComponent(@Nonnull JList<? extends PopupFactoryImpl.ActionItem> list, @Nonnull PopupFactoryImpl.ActionItem actionItem, boolean isSelected) {
            //myNextStepButtonSeparator.setVisible(false);
            AnAction action = actionItem.getAction();
            Editor editor = dataContext.getData(EDITOR);
            java.util.List<PsiElement> elements = CopyReferenceUtil.getElementsToCopy(editor, dataContext);
            String qualifiedName = null;
            if (action instanceof CopyPathProvider) {
              qualifiedName = ((CopyPathProvider)action).getQualifiedName(getProject(), elements, editor, dataContext);
            }

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

  @Nullable
  public ActionGroup getCopyReferenceActionGroup() {
    AnAction popupGroup = ActionManager.getInstance().getAction("CopyReferencePopupGroup");
    if (!(popupGroup instanceof DefaultActionGroup)) {
      LOG.warn("Cannot find 'CopyReferencePopup' action to show popup");
      return null;
    }
    return (ActionGroup)popupGroup;
  }

  private static void updatePopupSize(@Nonnull ListPopup popup) {
    ApplicationManager.getApplication().invokeLater(() -> {
      popup.getContent().setPreferredSize(new Dimension(DEFAULT_WIDTH, popup.getContent().getPreferredSize().height));
      popup.getContent().setSize(new Dimension(DEFAULT_WIDTH, popup.getContent().getPreferredSize().height));
      popup.setSize(popup.getContent().getPreferredSize());
    });
  }
}