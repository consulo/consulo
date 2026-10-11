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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.actions;

import consulo.compiler.artifact.ui.PackagingSourceItem;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.SourceItemsTree;
import consulo.localize.LocalizeValue;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * @author nik
 */
public abstract class PutIntoDefaultLocationActionBase extends DumbAwareAction implements AnActionWithSyncUpdate {
    protected final SourceItemsTree mySourceItemsTree;
    protected final ArtifactEditorEx myArtifactEditor;

    public PutIntoDefaultLocationActionBase(SourceItemsTree sourceItemsTree, ArtifactEditorEx artifactEditor) {
        mySourceItemsTree = sourceItemsTree;
        myArtifactEditor = artifactEditor;
    }

    protected @Nullable String getDefaultPath(PackagingSourceItem item) {
        return myArtifactEditor.getArtifact().getArtifactType().getDefaultPathFor(item);
    }

    protected static LocalizeValue getTargetLocationText(Set<String> paths) {
        if (paths.size() != 1) {
            return ProjectLocalize.artifactLocationDefaultLocations();
        }

        String path = StringUtil.trimStart(StringUtil.trimEnd(paths.iterator().next(), "/"), "/");
        return path.isEmpty() ? ProjectLocalize.artifactLocationOutputRoot() : LocalizeValue.of("/" + path);
    }
}
