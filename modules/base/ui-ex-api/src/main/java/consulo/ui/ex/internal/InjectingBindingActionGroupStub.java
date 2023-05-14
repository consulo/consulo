/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.internal;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.component.bind.InjectingBinding;
import consulo.container.PluginException;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.util.lang.ObjectUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 26-Jun-22
 */
public class InjectingBindingActionGroupStub extends DefaultActionGroup implements InjectingBindingActionStubBase {
  private final ActionImpl myActionImpl;
  private final InjectingBinding myInjectingBinding;

  public InjectingBindingActionGroupStub(@Nonnull ActionImpl actionImpl, @Nonnull InjectingBinding binding) {
    myActionImpl = actionImpl;
    myInjectingBinding = binding;
  }

  @Nonnull
  @Override
  protected Presentation createTemplatePresentation() {
    Presentation presentation = super.createTemplatePresentation();

    PluginDescriptor plugin = PluginManager.getPlugin(myInjectingBinding.getClass());
    LocalizeHelper helper = LocalizeHelper.build(plugin);

    presentation.setTextValue(helper.getValue("action." + myActionImpl.id() + ".text"));
    presentation.setDescriptionValue(helper.getValue("action." + myActionImpl.id() + ".description"));
    return presentation;
  }

  @Nonnull
  @Override
  public ActionImpl getActionImpl() {
    return myActionImpl;
  }

  @Override
  public String getId() {
    return myActionImpl.id();
  }

  @Override
  public PluginId getPluginId() {
    return PluginManager.getPluginId(myInjectingBinding.getClass());
  }

  @Override
  public String getIconPath() {
    return null;
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public AnAction initialize(@Nonnull Application application, @Nonnull ActionManager manager) {
    ActionGroup target = (ActionGroup)application.getUnbindedInstance(myInjectingBinding.getImplClass(), myInjectingBinding.getParameterTypes(), myInjectingBinding::create);

    XmlActionStub.copyTemplatePresentation(getTemplatePresentation(), target.getTemplatePresentation());
    target.setCanUseProjectAsDefault(isCanUseProjectAsDefault());
    target.setModuleExtensionIds(getModuleExtensionIds());
    target.setShortcutSet(getShortcutSet());

    AnAction[] children = getChildren(null, manager);
    if (children.length > 0) {
      DefaultActionGroup dTarget = ObjectUtil.tryCast(target, DefaultActionGroup.class);
      if (dTarget == null) {
        throw new PluginException("Action group class must extend DefaultActionGroup for the group to accept children:" + myInjectingBinding.getClass(), getPluginId());
      }

      for (AnAction action : children) {
        dTarget.addAction(action, Constraints.LAST, manager);
      }
    }
    return target;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    throw new UnsupportedOperationException();
  }
}
