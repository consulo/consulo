/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.awt.ui.dialog;

import consulo.annotation.component.ServiceImpl;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.action.ButtonToolbarImpl;
import consulo.platform.Platform;
import consulo.ui.Component;
import consulo.ui.Size;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.ui.ex.awt.CustomLineBorder;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.util.concurrent.AsyncResult;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * @author VISTALL
 * @since 13/12/2021
 */
@Singleton
@ServiceImpl
public class DesktopAwtDialogService implements DialogService {
  private static class DialogImpl<V> implements Dialog<V> {
    private final DialogWrapperImpl myDialogWrapper;
    private final DialogDescriptor<V> myDescriptor;

    private V myValue;

    public DialogImpl(java.awt.Component component, DialogDescriptor<V> descriptor) {
      myDescriptor = descriptor;
      if (component != null) {
        myDialogWrapper = new DialogWrapperImpl(component, descriptor, this);
      }
      else {
        myDialogWrapper = new DialogWrapperImpl(descriptor, this);
      }
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public AsyncResult<V> showAsync() {
      AsyncResult<V> result = AsyncResult.undefined();

      AsyncResult<Void> showAsync = myDialogWrapper.showAsync();
      showAsync.doWhenDone(() -> result.setDone(myValue));

      showAsync.doWhenRejected((Runnable)result::setRejected);
      return result;
    }

    @Override
    public void doOkAction(@Nullable V value) {
      myValue = value;

      myDialogWrapper.close(DialogWrapper.OK_EXIT_CODE);
    }

    @Override
    public void doCancelAction() {
      myDialogWrapper.close(DialogWrapper.CANCEL_EXIT_CODE);
    }

    @Nonnull
    @Override
    public DialogDescriptor<V> getDescriptor() {
      return myDescriptor;
    }
  }

  private static class DialogWrapperImpl extends DialogWrapper {
    private JButton myDefaultButton;

    private final DialogDescriptor myDescriptor;
    private final Dialog myDialog;

    protected DialogWrapperImpl(DialogDescriptor descriptor, Dialog dialog) {
      super(true);
      myDescriptor = descriptor;
      myDialog = dialog;

      initImpl();
    }

    protected DialogWrapperImpl(java.awt.Component parent, DialogDescriptor descriptor, Dialog dialog) {
      super(parent, true);
      myDescriptor = descriptor;
      myDialog = dialog;

      initImpl();
    }

    private void initImpl() {
      setTitle(myDescriptor.getTitle().get());

      init();

      Size size = myDescriptor.getInitialSize();
      if (size != null) {
        setScalableSize(size.getWidth(), size.getHeight());
      }
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
      throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    protected JComponent createSouthPanel() {
      JPanel panel = new JPanel(new BorderLayout());
      DataManager.registerDataProvider(panel, dataId -> {
        if (dataId == Dialog.KEY) {
          return myDialog;
        }

        return null;
      });

      AnAction[] actions = myDescriptor.createActions(Platform.current().os().isMac());

      ActionGroup group = ActionGroup.newImmutableBuilder().addAll(actions).build();

      ButtonToolbarImpl buttonToolbar = new ButtonToolbarImpl(ActionPlaces.UNKNOWN, group) {
        @Override
        protected JButton createButton(AnAction action) {
          JButton button = super.createButton(action);

          if (myDescriptor.isDefaultAction(action)) {
            myDefaultButton = button;
          }
          return button;
        }
      };

      panel.add(buttonToolbar.getComponent(), BorderLayout.EAST);

      panel.setBorder(JBUI.Borders.empty(ourDefaultBorderInsets));

      BorderLayoutPanel borderLayoutPanel = JBUI.Panels.simplePanel(panel);
      borderLayoutPanel.setBorder(new CustomLineBorder(JBUI.scale(1), 0, 0, 0));
      return borderLayoutPanel;
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public AsyncResult<Void> showAsync() {
      getPeer().getRootPane().setDefaultButton(myDefaultButton);
      return super.showAsync();
    }

    @Nullable
    @Override
    protected Border createContentPaneBorder() {
      if (!myDescriptor.isSetDefaultContentBorder()) {
        return JBUI.Borders.empty();
      }
      return super.createContentPaneBorder();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      return (JComponent)TargetAWT.to(myDescriptor.createCenterComponent(getDisposable()));
    }
  }

  @Nonnull
  @Override
  public <V> Dialog<V> build(@Nullable Component parent, @Nonnull DialogDescriptor<V> descriptor) {
    return new DialogImpl<>(TargetAWT.to(parent), descriptor);
  }
}
