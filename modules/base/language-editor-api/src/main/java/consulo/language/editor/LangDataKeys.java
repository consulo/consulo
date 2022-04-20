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

package consulo.language.editor;

import consulo.content.library.Library;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.dataholder.Key;
import consulo.util.lang.function.Condition;

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

  Key<Boolean> NO_NEW_ACTION = Key.create("IDEview.no.create.element.action");
  Key<Condition<AnAction>> PRESELECT_NEW_ACTION_CONDITION = Key.create("newElementAction.preselect.id");

  Key<PsiElement> TARGET_PSI_ELEMENT = Key.create("psi.TargetElement");
  Key<Module> TARGET_MODULE = Key.create("module.TargetModule");
  Key<PsiElement> PASTE_TARGET_PSI_ELEMENT = Key.create("psi.pasteTargetElement");

  Key<JBPopup> POSITION_ADJUSTER_POPUP = Key.create("chooseByNameDropDown");
  Key<JBPopup> PARENT_POPUP = Key.create("chooseByNamePopup");


  Key<Library> LIBRARY = Key.create("project.model.library");
}
