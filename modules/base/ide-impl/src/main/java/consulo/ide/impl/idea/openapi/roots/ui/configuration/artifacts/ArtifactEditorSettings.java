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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.compiler.artifact.element.ComplexPackagingElementType;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementType;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
public class ArtifactEditorSettings {
    private static final String SORT_ELEMENTS_KEY = "artifact.editor.sort.elements";
    private static final String SHOW_CONTENT_KEY = "artifact.editor.show.content.types";

    private final Project myProject;
    private boolean mySortElements = true;
    private final List<ComplexPackagingElementType<?>> myTypesToShowContent = new ArrayList<>();

    public ArtifactEditorSettings(Project project) {
        myProject = project;
    }

    public ArtifactEditorSettings(Project project, boolean sortElements, Collection<ComplexPackagingElementType<?>> typesToShowContent) {
        this(project);
        mySortElements = sortElements;
        myTypesToShowContent.addAll(typesToShowContent);
    }

    public void load(ProjectPropertiesComponent properties) {
        mySortElements = properties.getBoolean(SORT_ELEMENTS_KEY, true);
        myTypesToShowContent.clear();

        String[] ids = properties.getValues(SHOW_CONTENT_KEY);
        if (ids == null) {
            return;
        }

        PackagingElementFactory factory = PackagingElementFactory.getInstance(myProject);
        for (String id : ids) {
            PackagingElementType<?> type = factory.findElementType(id);
            if (type instanceof ComplexPackagingElementType<?> complexType) {
                myTypesToShowContent.add(complexType);
            }
        }
    }

    public void save(ProjectPropertiesComponent properties) {
        properties.setValue(SORT_ELEMENTS_KEY, mySortElements, true);

        String[] ids = new String[myTypesToShowContent.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = myTypesToShowContent.get(i).getId();
        }
        properties.setValues(SHOW_CONTENT_KEY, ids);
    }

    public boolean isSortElements() {
        return mySortElements;
    }

    public List<ComplexPackagingElementType<?>> getTypesToShowContent() {
        return myTypesToShowContent;
    }

    public void setSortElements(boolean sortElements) {
        mySortElements = sortElements;
    }

    public void setTypesToShowContent(Collection<ComplexPackagingElementType<?>> typesToShowContent) {
        myTypesToShowContent.clear();
        myTypesToShowContent.addAll(typesToShowContent);
    }
}
