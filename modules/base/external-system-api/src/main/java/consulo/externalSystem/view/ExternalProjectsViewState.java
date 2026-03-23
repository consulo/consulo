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
package consulo.externalSystem.view;

import org.jdom.Element;

/**
 * Persistent state for the external projects tool window view.
 * Stores user preferences such as grouping and visibility options as well as the tree expansion state.
 *
 * @author Vladislav.Soroka
 */
public class ExternalProjectsViewState {
    public boolean showIgnored = false;
    public boolean groupTasks = false;
    public boolean groupModules = true;
    public boolean showInheritedTasks = false;
    public Element treeState = null;
}
