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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.ide.impl.idea.ide.CopyPasteManagerEx;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataManager;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.localize.UILocalize;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author max
 */
public class MultiplePasteAction extends AnAction implements DumbAware {
  public MultiplePasteAction() {
    setEnabledInModalContext(true);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    Component focusedComponent = e.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
    Editor editor = e.getData(Editor.KEY);

    if (!(focusedComponent instanceof JComponent)) return;

    final CopyPasteManagerEx copyPasteManager = CopyPasteManagerEx.getInstanceEx();
    final ContentChooser<Transferable> chooser = new ContentChooser<>(project, UILocalize.chooseContentToPasteDialogTitle().get(), true, true){
      @Override
      protected String getStringRepresentationFor(final Transferable content) {
        try {
          return (String)content.getTransferData(DataFlavor.stringFlavor);
        }
        catch (UnsupportedFlavorException e1) {
          return "";
        }
        catch (IOException e1) {
          return "";
        }
      }

      @Override
      protected List<Transferable> getContents() {
        return Arrays.asList(CopyPasteManager.getInstance().getAllContents());
      }

      @Override
      protected void removeContentAt(final Transferable content) {
        copyPasteManager.removeContent(content);
      }
    };

    if (!chooser.getAllContents().isEmpty()) {
      chooser.show();
    }
    else {
      chooser.close(DialogWrapper.CANCEL_EXIT_CODE);
    }

    if (chooser.isOK()) {
      final int[] selectedIndices = chooser.getSelectedIndices();
      if (selectedIndices.length == 1) {
        copyPasteManager.moveContentToStackTop(chooser.getAllContents().get(selectedIndices[0]));
      }
      else {
        copyPasteManager.setContents(new StringSelection(chooser.getSelectedText()));
      }

      if (editor != null) {
        if (editor.isViewer()) return;
        if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)){
          return;
        }

        final AnAction pasteAction = ActionManager.getInstance().getAction(IdeActions.ACTION_PASTE);
        AnActionEvent newEvent = new AnActionEvent(e.getInputEvent(),
                                                   DataManager.getInstance().getDataContext(focusedComponent),
                                                   e.getPlace(), e.getPresentation(),
                                                   ActionManager.getInstance(),
                                                   e.getModifiers());
        pasteAction.actionPerformed(newEvent);
      }
      else {
        final Action pasteAction = ((JComponent)focusedComponent).getActionMap().get(DefaultEditorKit.pasteAction);
        if (pasteAction != null) {
          pasteAction.actionPerformed(new ActionEvent(focusedComponent, ActionEvent.ACTION_PERFORMED, ""));
        }
      }
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    final boolean enabled = isEnabled(e);
    if (ActionPlaces.isPopupPlace(e.getPlace())) {
      e.getPresentation().setVisible(enabled);
    }
    else {
      e.getPresentation().setEnabled(enabled);
    }
  }

  private static boolean isEnabled(@Nonnull AnActionEvent e) {
    if (!(e.getData(UIExAWTDataKey.CONTEXT_COMPONENT) instanceof JComponent component)) return false;
    Editor editor = e.getData(Editor.KEY);
    if (editor != null) return !editor.isViewer();
    return component.getActionMap().get(DefaultEditorKit.pasteAction) != null;
  }
}
