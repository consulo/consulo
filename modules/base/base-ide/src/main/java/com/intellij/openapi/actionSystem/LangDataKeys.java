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

package com.intellij.openapi.actionSystem;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.IdeView;
import com.intellij.lang.Language;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Condition;
import consulo.annotation.DeprecationInfo;
import consulo.execution.ExecutionDataKeys;
import consulo.util.dataholder.Key;
import com.intellij.psi.PsiElement;

/**
 * @author yole
 */
public interface LangDataKeys extends PlatformDataKeys {
  Key<Module> MODULE = CommonDataKeys.MODULE;
  /**
   * Returns module if module node is selected (in module view)
   */
  Key<Module> MODULE_CONTEXT = Key.create("context.Module");
  Key<Module[]> MODULE_CONTEXT_ARRAY = Key.create("context.Module.Array");
  Key<ModifiableModuleModel> MODIFIABLE_MODULE_MODEL = Key.create("modifiable.module.model");

  Key<Language> LANGUAGE = Key.create("Language");
  Key<Language[]> CONTEXT_LANGUAGES = Key.create("context.Languages");
  Key<PsiElement[]> PSI_ELEMENT_ARRAY = CommonDataKeys.PSI_ELEMENT_ARRAY;

  /**
   * Returns {@link com.intellij.ide.IdeView} (one of project, packages, commander or favorites view).
   */
  Key<IdeView> IDE_VIEW = Key.create("IDEView");
  Key<Boolean> NO_NEW_ACTION = Key.create("IDEview.no.create.element.action");
  Key<Condition<AnAction>> PRESELECT_NEW_ACTION_CONDITION = Key.create("newElementAction.preselect.id");

  Key<PsiElement> TARGET_PSI_ELEMENT = Key.create("psi.TargetElement");
  Key<Module> TARGET_MODULE = Key.create("module.TargetModule");
  Key<PsiElement> PASTE_TARGET_PSI_ELEMENT = Key.create("psi.pasteTargetElement");

  Key<ConsoleView> CONSOLE_VIEW = Key.create("consoleView");

  Key<JBPopup> POSITION_ADJUSTER_POPUP = Key.create("chooseByNameDropDown");
  Key<JBPopup> PARENT_POPUP = Key.create("chooseByNamePopup");


  Key<Library> LIBRARY = Key.create("project.model.library");

  @Deprecated
  @DeprecationInfo("Use ExecutionDataKeys")
  Key<RunProfile> RUN_PROFILE = ExecutionDataKeys.RUN_PROFILE;
  @Deprecated
  @DeprecationInfo("Use ExecutionDataKeys")
  Key<ExecutionEnvironment> EXECUTION_ENVIRONMENT = ExecutionDataKeys.EXECUTION_ENVIRONMENT;
  @Deprecated
  @DeprecationInfo("Use ExecutionDataKeys")
  Key<RunContentDescriptor> RUN_CONTENT_DESCRIPTOR = ExecutionDataKeys.RUN_CONTENT_DESCRIPTOR;
}
