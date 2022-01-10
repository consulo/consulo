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
package consulo.ide.actions;

import com.intellij.ide.caches.CachesInvalidator;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.gist.GistManager;
import com.intellij.util.indexing.FileBasedIndex;
import consulo.awt.TargetAWT;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.CheckBox;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.collection.ArrayUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 20/03/2021
 */
public class InvalidateCacheDialog extends DialogWrapper {

  private Map<CachesInvalidator, Boolean> myStates = new LinkedHashMap<>();

  private Action myJustRestartAction;

  public InvalidateCacheDialog(@Nullable Project project) {
    super(project);

    boolean restartCapable = Application.get().isRestartCapable();

    setTitle(restartCapable ? "Invalidate Caches / Restart" : "Invalidate Caches");

    setOKButtonText(restartCapable ? "Invalidate & Restart" : "Invalidate");
    setOKButtonIcon(TargetAWT.to(PlatformIconGroup.generalWarning()));

    if(restartCapable) {
      myJustRestartAction = new DialogWrapperAction(LocalizeValue.localizeTODO("Just Restart")) {
        @Override
        protected void doAction(ActionEvent e) {
          close(OK_EXIT_CODE);

          ApplicationEx application = (ApplicationEx)Application.get();
          application.restart(true);
        }
      };
    }

    init();
  }

  @Nullable
  @Override
  @RequiredUIAccess
  protected JComponent createCenterPanel() {
    VerticalLayout root = VerticalLayout.create();
    root.add(HorizontalLayout.create().add(Label.create(LocalizeValue.localizeTODO("The caches will be invalidated and rebuilt on the next startup"))));

    VerticalLayout optionalLayout = VerticalLayout.create();
    root.add(LabeledLayout.create(LocalizeValue.localizeTODO("Optional:"), optionalLayout));

    for (CachesInvalidator invalidator : CachesInvalidator.EP_NAME.getExtensionList()) {
      CheckBox checkBox = CheckBox.create(invalidator.getDescription());
      checkBox.setValue(invalidator.isEnabledByDefault());
      checkBox.addValueListener(event -> myStates.put(invalidator, event.getValue()));

      myStates.put(invalidator, checkBox.getValueOrError());

      optionalLayout.add(checkBox);
    }
    return (JComponent)TargetAWT.to(root);
  }

  @Override
  protected void doOKAction() {
    super.doOKAction();

    FileBasedIndex.getInstance().invalidateCaches();
    GistManager.getInstance().invalidateData();

    for (Map.Entry<CachesInvalidator, Boolean> entry : myStates.entrySet()) {
      if (entry.getValue()) {
        entry.getKey().invalidateCaches();
      }
    }

    ApplicationEx application = (ApplicationEx)Application.get();
    application.restart(true);
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    Action[] actions = super.createActions();
    if(myJustRestartAction == null) {
      return actions;
    }
    
    if(Platform.current().os().isMac()) {
      return ArrayUtil.prepend(myJustRestartAction, actions);
    }
    else {
      return ArrayUtil.append(actions, myJustRestartAction);
    }
  }

  @Nullable
  @Override
  protected String getHelpId() {
    return "platform/dialogs/invalidate_caches/";
  }
}
