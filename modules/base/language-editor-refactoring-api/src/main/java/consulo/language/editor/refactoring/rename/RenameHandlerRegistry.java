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

package consulo.language.editor.refactoring.rename;

import consulo.application.ApplicationManager;
import consulo.component.ProcessCanceledException;
import consulo.component.extension.Extensions;
import consulo.dataContext.DataContext;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.rename.inplace.MemberInplaceRenameHandler;
import consulo.language.editor.refactoring.ui.RadioUpDownListener;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * @author dsl
 */
public class RenameHandlerRegistry {
  public static final Key<Boolean> SELECT_ALL = Key.create("rename.selectAll");
  private final Set<RenameHandler> myHandlers  = new HashSet<RenameHandler>();
  private static final RenameHandlerRegistry INSTANCE = new RenameHandlerRegistry();
  private final PsiElementRenameHandler myDefaultElementRenameHandler;

  public static RenameHandlerRegistry getInstance() {
    return INSTANCE;
  }

  private RenameHandlerRegistry() {
    // should be checked last
    myDefaultElementRenameHandler = new PsiElementRenameHandler();
  }

  public boolean hasAvailableHandler(DataContext dataContext) {
    for (RenameHandler renameHandler : Extensions.getExtensions(RenameHandler.EP_NAME)) {
      if (renameHandler.isAvailableOnDataContext(dataContext)) return true;
    }
    for (RenameHandler renameHandler : myHandlers) {
      if (renameHandler.isAvailableOnDataContext(dataContext)) return true;
    }
    return myDefaultElementRenameHandler.isAvailableOnDataContext(dataContext);
  }

  @Nullable
  public RenameHandler getRenameHandler(DataContext dataContext) {
    final Map<String, RenameHandler> availableHandlers = new TreeMap<String, RenameHandler>();
    for (RenameHandler renameHandler : Extensions.getExtensions(RenameHandler.EP_NAME)) {
      if (renameHandler.isRenaming(dataContext)) {
        if (ApplicationManager.getApplication().isUnitTestMode()) return renameHandler;
        availableHandlers.put(getHandlerTitle(renameHandler), renameHandler);
      }
    }
    for (RenameHandler renameHandler : myHandlers) {
      if (renameHandler.isRenaming(dataContext)) {
        if (ApplicationManager.getApplication().isUnitTestMode()) return renameHandler;
        availableHandlers.put(getHandlerTitle(renameHandler), renameHandler);
      }
    }
    if (availableHandlers.size() == 1) return availableHandlers.values().iterator().next();
    for (Iterator<Map.Entry<String, RenameHandler>> iterator = availableHandlers.entrySet().iterator(); iterator.hasNext(); ) {
      Map.Entry<String, RenameHandler> entry = iterator.next();
      if (entry.getValue() instanceof MemberInplaceRenameHandler) {
        iterator.remove();
        break;
      }
    }
    if (availableHandlers.size() == 1) return availableHandlers.values().iterator().next();
    if (availableHandlers.size() > 1) {
      final String[] strings = ArrayUtil.toStringArray(availableHandlers.keySet());
      final HandlersChooser chooser = new HandlersChooser(dataContext.getData(CommonDataKeys.PROJECT), strings);
      chooser.show();
      if (chooser.isOK()) {
        return availableHandlers.get(chooser.getSelection());
      }
      throw new ProcessCanceledException();
    }
    return myDefaultElementRenameHandler.isRenaming(dataContext) ? myDefaultElementRenameHandler : null;
  }

  private static String getHandlerTitle(RenameHandler renameHandler) {
    return StringUtil.capitalize((renameHandler).getActionTitle().toLowerCase());
  }

  /**
   * @deprecated
   * @see RenameHandler#EP_NAME
   */
  public void registerHandler(RenameHandler handler) {
    myHandlers.add(handler);
  }

  private static class HandlersChooser extends DialogWrapper {
    private final String[] myRenamers;
    private String mySelection;
    private final JRadioButton[] myRButtons;

    protected HandlersChooser(Project project, String [] renamers) {
      super(project);
      myRenamers = renamers;
      myRButtons = new JRadioButton[myRenamers.length];
      mySelection = renamers[0];
      setTitle(RefactoringBundle.message("select.refactoring.title"));
      init();
    }

    @Override
    protected JComponent createNorthPanel() {
      final JPanel radioPanel = new JPanel();
      radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
      final JLabel descriptionLabel = new JLabel(RefactoringBundle.message("what.would.you.like.to.do"));
      descriptionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
      radioPanel.add(descriptionLabel);
      final ButtonGroup bg = new ButtonGroup();
      boolean selected = true;
      int rIdx = 0;
      for (final String renamer : myRenamers) {
        final JRadioButton rb = new JRadioButton(renamer, selected);
        myRButtons[rIdx++] = rb;
        final ActionListener listener = new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (rb.isSelected()) {
              mySelection = renamer;
            }
          }
        };
        rb.addActionListener(listener);
        selected = false;
        bg.add(rb);
        radioPanel.add(rb);
      }
      new RadioUpDownListener(myRButtons);
      return radioPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
      return myRButtons[0];
    }

    public String getSelection() {
      return mySelection;
    }

    @Override
    protected JComponent createCenterPanel() {
      return null;
    }
  }
}
