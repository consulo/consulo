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
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2022-06-25
 */
public class InjectingBindingActionStub extends AnAction implements InjectingBindingActionStubBase {
  private final ActionImpl myActionImpl;
  private final InjectingBinding myInjectingBinding;

  public InjectingBindingActionStub(ActionImpl actionImpl, InjectingBinding binding) {
    myActionImpl = actionImpl;
    myInjectingBinding = binding;
  }

  @Override
  protected Presentation createTemplatePresentation() {
    Presentation presentation = super.createTemplatePresentation();

    PluginDescriptor plugin = PluginManager.getPlugin(myInjectingBinding.getClass());
    LocalizeHelper helper = LocalizeHelper.build(plugin);

    presentation.setText(helper.getValue("action." + myActionImpl.id() + ".text"));
    presentation.setDescription(helper.getValue("action." + myActionImpl.id() + ".description"));
    return presentation;
  }

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

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable AnAction initialize(Application application, ActionManager manager) {
    AnAction target = (AnAction)application.getUnbindedInstance(myInjectingBinding.getImplClass(), myInjectingBinding.getParameterTypes(), myInjectingBinding::create);

    XmlActionStub.copyTemplatePresentation(getTemplatePresentation(), target.getTemplatePresentation());
    target.setShortcutSet(getShortcutSet());
    return target;
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    throw new UnsupportedOperationException();
  }
}
