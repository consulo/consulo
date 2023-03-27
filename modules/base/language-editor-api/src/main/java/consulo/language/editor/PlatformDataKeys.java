/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.application.HelpManager;
import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.ui.ModalityState;
import consulo.ui.ex.*;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.ExporterToTextFile;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Comparator;

/**
 * @author yole
 */
public interface PlatformDataKeys extends CommonDataKeys {
  @Deprecated
  Key<FileEditor> FILE_EDITOR = FileEditor.KEY;

  /**
   * Returns the text of currently selected file/file revision
   */
  Key<String> FILE_TEXT = Key.create("fileText");

  /**
   * Returns Boolean.TRUE if action is executed in modal context and
   * Boolean.FALSE if action is executed not in modal context. If context
   * is unknown then the value of this data constant is <code>null</code>.
   */
  Key<Boolean> IS_MODAL_CONTEXT = Key.create("isModalContext");

  /**
   * Returns help id (String)
   */
  Key<String> HELP_ID = HelpManager.HELP_ID;

  /**
   * Returns project if project node is selected (in project view)
   */
  Key<Project> PROJECT_CONTEXT = Key.create("context.Project");

  /**
   * Returns java.awt.Component currently in focus, DataContext should be retrieved for
   */
  Key<consulo.ui.Component> CONTEXT_UI_COMPONENT = Key.create("contextUIComponent");

  Key<CopyProvider> COPY_PROVIDER = CopyProvider.KEY;

  Key<CutProvider> CUT_PROVIDER = CutProvider.KEY;

  Key<PasteProvider> PASTE_PROVIDER = PasteProvider.KEY;

  Key<DeleteProvider> DELETE_ELEMENT_PROVIDER = DeleteProvider.KEY;

  Key<Object> SELECTED_ITEM = Key.create("selectedItem");
  Key<Object[]> SELECTED_ITEMS = Key.create("selectedItems");
  Key<ContentManager> CONTENT_MANAGER = Key.create("contentManager");
  Key<ToolWindow> TOOL_WINDOW = ToolWindow.KEY;
  Key<StatusBar> STATUS_BAR = Key.create("STATUS_BAR");
  Key<TreeExpander> TREE_EXPANDER = Key.create("treeExpander");
  Key<ExporterToTextFile> EXPORTER_TO_TEXT_FILE = Key.create("exporterToTextFile");
  Key<VirtualFile> PROJECT_FILE_DIRECTORY = Project.PROJECT_FILE_DIRECTORY;
  Key<Disposable> UI_DISPOSABLE = Key.create("ui.disposable");

  Key<ContentManager> NONEMPTY_CONTENT_MANAGER = Key.create("nonemptyContentManager");
  @Deprecated
  Key<ModalityState> MODALITY_STATE = ModalityState.KEY;
  Key<Boolean> SOURCE_NAVIGATION_LOCKED = Key.create("sourceNavigationLocked");

  Key<String> PREDEFINED_TEXT = Key.create("predefined.text.value");

  Key<Object> SPEED_SEARCH_COMPONENT = Key.create("speed.search.component.value");
  Key<String> SEARCH_INPUT_TEXT = Key.create("search.input.text.value");

  /**
   * It's allowed to assign multiple actions to the same keyboard shortcut. Actions system filters them on the current
   * context basis during processing (e.g. we can have two actions assigned to the same shortcut but one of them is
   * configured to be inapplicable in modal dialog context).
   * <p/>
   * However, there is a possible case that there is still more than one action applicable for particular keyboard shortcut
   * after filtering. The first one is executed then. Hence, actions processing order becomes very important.
   * <p/>
   * Current key allows to specify custom actions sorter to use if any. I.e. every component can define it's custom
   * sorting rule in order to define priorities for target actions (classes of actions).
   *
   * @deprecated use consulo.ide.impl.idea.openapi.actionSystem.ActionPromoter
   */
  @Deprecated
  Key<Comparator<? super AnAction>> ACTIONS_SORTER = Key.create("actionsSorter");
}
