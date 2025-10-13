/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.editor.inspection.scheme;

import consulo.annotation.DeprecationInfo;
import consulo.component.persist.ComponentSerializationUtil;
import consulo.language.Language;
import consulo.language.editor.inspection.CleanupLocalInspectionTool;
import consulo.language.editor.inspection.GlobalInspectionContext;
import consulo.language.editor.inspection.InspectionTool;
import consulo.language.editor.inspection.InspectionToolState;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.internal.inspection.DummyInspectionToolState;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.io.ResourceUtil;
import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.SkipDefaultValuesSerializationFilters;
import consulo.util.xml.serializer.XmlSerializer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Attribute;
import org.jdom.Element;

import java.io.IOException;
import java.net.URL;

/**
 * @author Dmitry Avdeev
 * @since 2011-09-28
 */
public abstract class InspectionToolWrapper<T extends InspectionTool> {
    private static final Logger LOG = Logger.getInstance(InspectionToolWrapper.class);
    private static final SkipDefaultValuesSerializationFilters ourFilter = new SkipDefaultValuesSerializationFilters();

    @Nonnull
    protected final T myTool;
    @Nonnull
    private final HighlightDisplayKey myHighlightDisplayKey;
    protected InspectionToolState<Object> myStateProvider;

    protected InspectionToolWrapper(@Nonnull T tool, @Nonnull HighlightDisplayKey highlightDisplayKey) {
        myTool = tool;
        myHighlightDisplayKey = highlightDisplayKey;
    }

    protected InspectionToolWrapper(@Nonnull InspectionToolWrapper<T> other) {
        myTool = other.myTool;
        myHighlightDisplayKey = other.myHighlightDisplayKey;

        // reset state - it will inited again and loaded
        myStateProvider = null;
    }

    @Nonnull
    public HighlightDisplayKey getHighlightDisplayKey() {
        return myHighlightDisplayKey;
    }

    public void readExternal(Element element) {
        InspectionToolState<Object> provider = getToolState();
        if (provider instanceof DummyInspectionToolState) {
            return;
        }

        Class<Object> stateClass = ComponentSerializationUtil.getStateClass(provider.getClass());

        Object o = XmlSerializer.deserialize(element, stateClass);
        if (o != null) {
            provider.loadState(o);
        }
    }

    public void writeExternal(@Nonnull Element element) {
        InspectionToolState provider = getToolState();
        if (provider instanceof DummyInspectionToolState) {
            return;
        }

        Object state = provider.getState();
        if (state != null) {
            Element serializeElement = XmlSerializer.serialize(state, ourFilter);

            if (!JDOMUtil.isEmpty(serializeElement)) {
                for (Attribute attribute : serializeElement.getAttributes()) {
                    element.setAttribute(attribute.clone());
                }

                for (Element child : serializeElement.getChildren()) {
                    element.addContent(child.clone());
                }
            }
        }
    }

    public void initialize(@Nonnull GlobalInspectionContext context) {
    }

    @Nonnull
    public abstract InspectionToolWrapper<T> createCopy();

    @Nonnull
    @SuppressWarnings("unchecked")
    public InspectionToolState<Object> getToolState() {
        if (myStateProvider == null) {
            myStateProvider = (InspectionToolState<Object>) myTool.createStateProvider();
        }
        return myStateProvider;
    }

    @Nonnull
    public Object getState() {
        return getToolState().getState();
    }

    @Nonnull
    public T getTool() {
        return myTool;
    }

    public boolean isInitialized() {
        // deprecated?
        return true;
    }

    @Nullable
    public Language getLanguage() {
        return getTool().getLanguage();
    }

    public boolean isCleanupTool() {
        return getTool() instanceof CleanupLocalInspectionTool;
    }

    @Nonnull
    public String getShortName() {
        return getTool().getShortName();
    }

    @Nonnull
    public LocalizeValue getDisplayName() {
        return getTool().getDisplayName();
    }

    @Nonnull
    @Deprecated
    @DeprecationInfo("Use #getJoinedGroupPath()")
    public LocalizeValue getGroupDisplayName() {
        return getTool().getGroupDisplayName();
    }

    public boolean isEnabledByDefault() {
        return getTool().isEnabledByDefault();
    }

    @Nonnull
    public HighlightDisplayLevel getDefaultLevel() {
        return getTool().getDefaultLevel();
    }

    @Nonnull
    public LocalizeValue[] getGroupPath() {
        return getTool().getGroupPath();
    }

    @Nonnull
    public LocalizeValue getJoinedGroupPath() {
        LocalizeValue[] groupPath = getGroupPath();
        if (groupPath.length == 0) {
            return InspectionLocalize.inspectionGeneralToolsGroupName();
        }
        return LocalizeValue.join("/", groupPath);
    }

    public String loadDescription() {
        LocalizeValue descriptionValue = getTool().getDescription();
        if (descriptionValue != LocalizeValue.of()) {
            return descriptionValue.get();
        }

        String description = getTool().getStaticDescription();
        if (description != null) {
            return description;
        }
        try {
            URL descriptionUrl = getDescriptionUrl();
            if (descriptionUrl == null) {
                return null;
            }
            return ResourceUtil.loadText(descriptionUrl);
        }
        catch (IOException ignored) {
        }

        return getTool().loadDescription();
    }

    protected URL getDescriptionUrl() {
        return superGetDescriptionUrl();
    }

    @Nullable
    protected URL superGetDescriptionUrl() {
        String fileName = getDescriptionFileName();
        return ResourceUtil.getResource(getDescriptionContextClass(), "/inspectionDescriptions", fileName);
    }

    @Nonnull
    public String getDescriptionFileName() {
        return getShortName() + ".html";
    }

    @Nonnull
    public final String getFolderName() {
        return getShortName();
    }

    @Nonnull
    public Class<? extends InspectionTool> getDescriptionContextClass() {
        return getTool().getClass();
    }

    public String getMainToolId() {
        return getTool().getMainToolId();
    }

    @Override
    public String toString() {
        return getShortName();
    }

    public void cleanup(Project project) {
        T tool = myTool;
        tool.cleanup(project);
    }

    @Nonnull
    public abstract JobDescriptor[] getJobDescriptors(@Nonnull GlobalInspectionContext context);
}
