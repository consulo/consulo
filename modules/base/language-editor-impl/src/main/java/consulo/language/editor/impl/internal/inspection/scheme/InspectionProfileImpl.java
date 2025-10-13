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

package consulo.language.editor.impl.internal.inspection.scheme;

import consulo.application.ApplicationManager;
import consulo.component.persist.scheme.ExternalInfo;
import consulo.component.persist.scheme.ExternalizableScheme;
import consulo.component.util.graph.DFSTBuilder;
import consulo.component.util.graph.Graph;
import consulo.component.util.graph.GraphGenerator;
import consulo.component.util.graph.InboundSemiGraph;
import consulo.content.scope.NamedScope;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.inspection.InspectionTool;
import consulo.language.editor.inspection.scheme.*;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.SeverityProvider;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.interner.Interner;
import consulo.util.jdom.interner.JDOMInterner;
import consulo.util.lang.Comparing;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import consulo.util.xml.serializer.annotation.Transient;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author max
 */
public class InspectionProfileImpl extends ProfileEx implements ModifiableModel, InspectionProfile, ExternalizableScheme {
    public static final String INSPECTION_TOOL_TAG = "inspection_tool";
    public static final String CLASS_TAG = "class";
    private static final Logger LOG = Logger.getInstance(InspectionProfileImpl.class);
    private static final String VALID_VERSION = "1.0";
    private static final String VERSION_TAG = "version";
    private static final String USED_LEVELS = "used_levels";
    private final Supplier<Collection<InspectionToolWrapper<?>>> myRegistrar;
    @Nonnull
    private final Map<String, Element> myUninstalledInspectionsSettings;
    private final ExternalInfo myExternalInfo = new ExternalInfo();
    protected InspectionProfileImpl mySource;
    private Map<String, ToolsImpl> myTools = new HashMap<>();
    private Map<String, Boolean> myDisplayLevelMap;
    @Attribute("is_locked")
    private boolean myLockedProfile;
    private InspectionProfileImpl myBaseProfile = null;
    private String myEnabledTool = null;
    private String[] myScopesOrder;
    private String myDescription;
    private boolean myModified;
    private volatile boolean myInitialized;

    public InspectionProfileImpl(@Nonnull InspectionProfileImpl inspectionProfile) {
        super(inspectionProfile.getName());

        myRegistrar = inspectionProfile.myRegistrar;
        myUninstalledInspectionsSettings = new LinkedHashMap<>(inspectionProfile.myUninstalledInspectionsSettings);

        myBaseProfile = inspectionProfile.myBaseProfile;
        setProjectLevel(inspectionProfile.isProjectLevel());
        myLockedProfile = inspectionProfile.myLockedProfile;
        mySource = inspectionProfile;
        setProfileManager(inspectionProfile.getProfileManager());
        copyFrom(inspectionProfile);
    }

    public InspectionProfileImpl(@Nonnull String profileName,
                                 @Nonnull Supplier<Collection<InspectionToolWrapper<?>>> registrar,
                                 @Nonnull ProfileManager profileManager) {
        this(profileName, registrar, profileManager, true);
    }

    public InspectionProfileImpl(@Nonnull String profileName,
                                 @Nonnull Supplier<Collection<InspectionToolWrapper<?>>> registrar,
                                 @Nonnull ProfileManager profileManager,
                                 boolean withDefault) {
        super(profileName);
        myRegistrar = registrar;
        myBaseProfile = withDefault ? getDefaultProfile(registrar, profileManager) : null;
        setProfileManager(profileManager);
        myUninstalledInspectionsSettings = new TreeMap<>();
    }

    @Nonnull
    public static InspectionProfileImpl createSimple(@Nonnull String name,
                                                     @Nonnull Project project,
                                                     @Nonnull final InspectionToolWrapper... toolWrappers) {
        InspectionProfileImpl profile = new InspectionProfileImpl(name, () -> Arrays.asList(toolWrappers), InspectionProfileManager.getInstance());
        profile.initInspectionTools(project);
        for (InspectionToolWrapper toolWrapper : toolWrappers) {
            profile.enableTool(toolWrapper.getShortName(), project);
        }
        return profile;
    }

    private static boolean toolSettingsAreEqual(@Nonnull String toolName,
                                                @Nonnull InspectionProfileImpl profile1,
                                                @Nonnull InspectionProfileImpl profile2) {
        Tools toolList1 = profile1.myTools.get(toolName);
        Tools toolList2 = profile2.myTools.get(toolName);

        return Comparing.equal(toolList1, toolList2);
    }

    @Nonnull
    private static InspectionToolWrapper copyToolSettings(@Nonnull InspectionToolWrapper toolWrapper) throws WriteExternalException, InvalidDataException {
        InspectionToolWrapper inspectionTool = toolWrapper.createCopy();
        if (toolWrapper.isInitialized()) {
            @NonNls String tempRoot = "config";
            Element config = new Element(tempRoot);
            toolWrapper.writeExternal(config);
            inspectionTool.readExternal(config);
        }
        return inspectionTool;
    }

    @Nonnull
    public static InspectionProfileImpl getDefaultProfile(Supplier<Collection<InspectionToolWrapper<?>>> registrar, ProfileManager profileManager) {
        return new InspectionProfileImpl("Default", registrar, profileManager, false);
    }

    @Override
    public void setModified(boolean modified) {
        myModified = modified;
    }

    @Override
    public InspectionProfile getParentProfile() {
        return mySource;
    }

    @Override
    public String getBaseProfileName() {
        if (myBaseProfile == null) {
            return null;
        }
        return myBaseProfile.getName();
    }

    @Override
    public void setBaseProfile(InspectionProfile profile) {
        myBaseProfile = (InspectionProfileImpl) profile;
    }

    @Override
    @SuppressWarnings({"SimplifiableIfStatement"})
    public boolean isChanged() {
        if (mySource != null && mySource.myLockedProfile != myLockedProfile) {
            return true;
        }
        return myModified;
    }

    @Override
    public boolean isProperSetting(@Nonnull String toolId) {
        if (myBaseProfile != null) {
            Tools tools = myBaseProfile.getTools(toolId, null);
            Tools currentTools = myTools.get(toolId);
            return !Comparing.equal(tools, currentTools);
        }
        return false;
    }

    @Override
    public void resetToBase(Project project) {
        initInspectionTools(project);

        copyToolsConfigurations(myBaseProfile, project);
        myDisplayLevelMap = null;
    }

    @Override
    public void resetToEmpty(Project project) {
        initInspectionTools(project);
        InspectionToolWrapper[] profileEntries = getInspectionTools(null);
        for (InspectionToolWrapper toolWrapper : profileEntries) {
            disableTool(toolWrapper.getShortName(), project);
        }
    }

    @Override
    public HighlightDisplayLevel getErrorLevel(@Nonnull HighlightDisplayKey inspectionToolKey, PsiElement element) {
        Project project = element == null ? null : element.getProject();
        ToolsImpl tools = getTools(inspectionToolKey.toString(), project);
        HighlightDisplayLevel level = tools != null ? tools.getLevel(element) : HighlightDisplayLevel.WARNING;
        if (!((SeverityProvider) getProfileManager()).getOwnSeverityRegistrar().isSeverityValid(level.getSeverity().getName())) {
            level = HighlightDisplayLevel.WARNING;
            setErrorLevel(inspectionToolKey, level, project);
        }
        return level;
    }

    @Override
    public void readExternal(@Nonnull Element element) throws InvalidDataException {
        super.readExternal(element);

        if (!ApplicationManager.getApplication().isUnitTestMode() || myBaseProfile == null) {
            // todo remove this strange side effect
            myBaseProfile = getDefaultProfile(myRegistrar, myProfileManager);
        }
        String version = element.getAttributeValue(VERSION_TAG);
        if (version == null || !version.equals(VALID_VERSION)) {
            element = InspectionProfileConvertor.convertToNewFormat(element, this);
        }

        Element highlightElement = element.getChild(USED_LEVELS);
        if (highlightElement != null) {
            // from old profiles
            ((SeverityRegistrarImpl) ((SeverityProvider) getProfileManager()).getOwnSeverityRegistrar()).readExternal(highlightElement);
        }

        Interner<String> interner = Interner.createStringInterner();
        for (Element toolElement : element.getChildren(INSPECTION_TOOL_TAG)) {
            // make clone to avoid retaining memory via o.parent pointers
            toolElement = toolElement.clone();
            JDOMInterner.internStringsInElement(toolElement, interner);
            myUninstalledInspectionsSettings.put(toolElement.getAttributeValue(CLASS_TAG), toolElement);
        }
    }

    @Nonnull
    public Set<HighlightSeverity> getUsedSeverities() {
        LOG.assertTrue(myInitialized);
        Set<HighlightSeverity> result = new HashSet<>();
        for (Tools tools : myTools.values()) {
            for (ScopeToolState state : tools.getTools()) {
                result.add(state.getLevel().getSeverity());
            }
        }
        return result;
    }

    @Override
    public void serializeInto(@Nonnull Element element, boolean preserveCompatibility) {
        // must be first - compatibility
        element.setAttribute(VERSION_TAG, VALID_VERSION);

        super.serializeInto(element, preserveCompatibility);

        synchronized (myExternalInfo) {
            if (!myInitialized) {
                for (Element el : myUninstalledInspectionsSettings.values()) {
                    element.addContent(el.clone());
                }
                return;
            }
        }

        Map<String, Boolean> diffMap = getDisplayLevelMap();
        if (diffMap != null) {

            diffMap = new TreeMap<>(diffMap);
            for (String toolName : myUninstalledInspectionsSettings.keySet()) {
                diffMap.put(toolName, false);
            }

            for (String toolName : diffMap.keySet()) {
                if (!myLockedProfile && diffMap.get(toolName).booleanValue()) {
                    continue;
                }

                Element toolElement = myUninstalledInspectionsSettings.get(toolName);
                if (toolElement == null) {
                    ToolsImpl toolList = myTools.get(toolName);
                    LOG.assertTrue(toolList != null);
                    Element inspectionElement = new Element(INSPECTION_TOOL_TAG);
                    inspectionElement.setAttribute(CLASS_TAG, toolName);
                    try {
                        toolList.writeExternal(inspectionElement);
                    }
                    catch (WriteExternalException e) {
                        LOG.error(e);
                        continue;
                    }

                    element.addContent(inspectionElement);
                }
                else {
                    element.addContent(toolElement.clone());
                }
            }
        }
    }

    public void collectDependentInspections(@Nonnull InspectionToolWrapper toolWrapper,
                                            @Nonnull Set<InspectionToolWrapper> dependentEntries,
                                            Project project) {
        String mainToolId = toolWrapper.getMainToolId();

        if (mainToolId != null) {
            InspectionToolWrapper dependentEntryWrapper = getInspectionTool(mainToolId, project);

            if (dependentEntryWrapper == null) {
                LOG.error("Can't find main tool: '" + mainToolId + "' which was specified in " + toolWrapper);
                return;
            }
            if (!dependentEntries.add(dependentEntryWrapper)) {
                collectDependentInspections(dependentEntryWrapper, dependentEntries, project);
            }
        }
    }

    @Override
    @Nullable
    public InspectionToolWrapper getInspectionTool(@Nonnull String shortName, @Nonnull PsiElement element) {
        Tools toolList = getTools(shortName, element.getProject());
        return toolList == null ? null : toolList.getInspectionTool(element);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends InspectionTool> T getUnwrappedTool(@Nonnull String shortName, @Nonnull PsiElement element) {
        InspectionToolWrapper tool = getInspectionTool(shortName, element);
        return tool == null ? null : (T) tool.getTool();
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <S> S getToolState(@Nonnull String shortName, @Nonnull PsiElement element) {
        InspectionToolWrapper tool = getInspectionTool(shortName, element);
        return tool == null ? null : (S) tool.getToolState().getState();
    }

    @Override
    public void modifyProfile(@Nonnull Consumer<ModifiableModel> modelConsumer) {
        ModifiableModel model = getModifiableModel();
        modelConsumer.accept(model);
        try {
            model.commit();
        }
        catch (IOException e) {
            LOG.error(e);
        }
    }

    @Override
    public <T extends InspectionTool, S> void modifyToolSettings(@Nonnull String shortName,
                                                                 @Nonnull PsiElement psiElement,
                                                                 @Nonnull BiConsumer<T, S> toolConsumer) {
        modifyProfile(model -> {
            InspectionToolWrapper wrapper = model.getInspectionTool(shortName, psiElement);
            //noinspection unchecked
            toolConsumer.accept((T) wrapper.getTool(), (S) wrapper.getToolState().getState());
        });
    }

    @Override
    @Nullable
    public InspectionToolWrapper getInspectionTool(@Nonnull String shortName, Project project) {
        ToolsImpl tools = getTools(shortName, project);
        return tools != null ? tools.getTool() : null;
    }

    @Override
    public InspectionToolWrapper getToolById(@Nonnull String id, @Nonnull PsiElement element) {
        initInspectionTools(element.getProject());
        for (Tools toolList : myTools.values()) {
            InspectionToolWrapper tool = toolList.getInspectionTool(element);
            String toolId = tool instanceof LocalInspectionToolWrapper ? ((LocalInspectionToolWrapper) tool).getID() : tool.getShortName();
            if (id.equals(toolId)) {
                return tool;
            }
        }
        return null;
    }

    @Override
    public void save() throws IOException {
        InspectionProfileManager.getInstance().fireProfileChanged(this);
    }

    @Override
    public boolean isEditable() {
        return myEnabledTool == null;
    }

    @Override
    public void setEditable(String displayName) {
        myEnabledTool = displayName;
    }

    @Override
    @Nonnull
    public String getDisplayName() {
        return isEditable() ? getName() : myEnabledTool;
    }

    @Override
    public void scopesChanged() {
        for (ScopeToolState toolState : getAllTools(null)) {
            toolState.scopesChanged();
        }
        InspectionProfileManager.getInstance().fireProfileChanged(this);
    }

    @Override
    @Transient
    public boolean isProfileLocked() {
        return myLockedProfile;
    }

    @Override
    public void lockProfile(boolean isLocked) {
        myLockedProfile = isLocked;
    }

    @Override
    @Nonnull
    public InspectionToolWrapper[] getInspectionTools(@Nullable PsiElement element) {
        initInspectionTools(element == null ? null : element.getProject());
        List<InspectionToolWrapper> result = new ArrayList<>();
        for (Tools toolList : myTools.values()) {
            result.add(toolList.getInspectionTool(element));
        }
        return result.toArray(new InspectionToolWrapper[result.size()]);
    }

    @Override
    @Nonnull
    public List<Tools> getAllEnabledInspectionTools(Project project) {
        initInspectionTools(project);
        List<Tools> result = new ArrayList<>();
        for (ToolsImpl toolList : myTools.values()) {
            if (toolList.isEnabled()) {
                result.add(toolList);
            }
        }
        return result;
    }

    @Override
    public void disableTool(@Nonnull String toolId, @Nonnull PsiElement element) {
        getTools(toolId, element.getProject()).disableTool(element);
    }

    public void disableToolByDefault(@Nonnull List<String> toolIds, Project project) {
        for (String toolId : toolIds) {
            getToolDefaultState(toolId, project).setEnabled(false);
        }
    }

    @Nonnull
    public ScopeToolState getToolDefaultState(@Nonnull String toolId, Project project) {
        return getTools(toolId, project).getDefaultState();
    }

    public void enableToolsByDefault(@Nonnull List<String> toolIds, Project project) {
        for (String toolId : toolIds) {
            getToolDefaultState(toolId, project).setEnabled(true);
        }
    }

    public boolean wasInitialized() {
        return myInitialized;
    }

    public void initInspectionTools(@Nullable Project project) {
        if (myInitialized) {
            return;
        }
        synchronized (myExternalInfo) {
            if (myInitialized) {
                return;
            }
            myInitialized = initialize(project);
        }
    }

    private boolean initialize(@Nullable Project project) {
        if (myBaseProfile != null) {
            myBaseProfile.initInspectionTools(project);
        }

        Collection<InspectionToolWrapper<?>> tools = createTools(project);

        final Map<String, List<String>> dependencies = new HashMap<>();
        for (InspectionToolWrapper toolWrapper : tools) {
            String shortName = toolWrapper.getShortName();
            HighlightDisplayKey highlightDisplayKey = toolWrapper.getHighlightDisplayKey();
            HighlightDisplayLevel level = myBaseProfile != null ? myBaseProfile.getErrorLevel(highlightDisplayKey, project) : toolWrapper.getDefaultLevel();
            boolean enabled = myBaseProfile != null ? myBaseProfile.isToolEnabled(highlightDisplayKey) : toolWrapper.isEnabledByDefault();
            ToolsImpl toolsList = new ToolsImpl(toolWrapper, level, !myLockedProfile && enabled, enabled);
            Element element = myUninstalledInspectionsSettings.remove(shortName);
            try {
                if (element != null) {
                    toolsList.readExternal(element, this, dependencies);
                }
            }
            catch (InvalidDataException e) {
                LOG.error("Can't read settings for " + toolWrapper, e);
            }
            myTools.put(toolWrapper.getShortName(), toolsList);
        }

        Graph<String> graphGenerator = GraphGenerator.generate(new InboundSemiGraph<>() {
            @Override
            public Collection<String> getNodes() {
                return dependencies.keySet();
            }

            @Override
            public Iterator<String> getIn(String n) {
                return dependencies.get(n).iterator();
            }
        });

        DFSTBuilder<String> builder = new DFSTBuilder<>(graphGenerator);
        if (builder.isAcyclic()) {
            List<String> scopes = builder.getSortedNodes();
            myScopesOrder = ArrayUtil.toStringArray(scopes);
        }

        if (mySource != null) {
            copyToolsConfigurations(mySource, project);
        }
        return true;
    }

    @Nullable
    @Transient
    public String[] getScopesOrder() {
        return myScopesOrder;
    }

    public void setScopesOrder(String[] scopesOrder) {
        myScopesOrder = scopesOrder;
    }

    @Nonnull
    private Collection<InspectionToolWrapper<?>> createTools(Project project) {
        if (mySource != null) {
            return ContainerUtil.map(mySource.getDefaultStates(project), ScopeToolState::getTool);
        }
        return myRegistrar.get();
    }

    private HighlightDisplayLevel getErrorLevel(@Nonnull HighlightDisplayKey key, Project project) {
        ToolsImpl tools = getTools(key.toString(), project);
        LOG.assertTrue(tools != null,
            "profile name: " + myName + " base profile: " + (myBaseProfile != null ? myBaseProfile.getName() : "-") + " key: " + key);
        return tools.getLevel();
    }

    @Override
    @Nonnull
    public ModifiableModel getModifiableModel() {
        InspectionProfileImpl modifiableModel = new InspectionProfileImpl(this);
        modifiableModel.myExternalInfo.copy(myExternalInfo);
        return modifiableModel;
    }

    @Override
    public void copyFrom(@Nonnull InspectionProfile profile) {
        super.copyFrom(profile);

        myBaseProfile = ((InspectionProfileImpl) profile).myBaseProfile;
    }

    private void copyToolsConfigurations(@Nonnull InspectionProfileImpl profile, @Nullable Project project) {
        try {
            for (ToolsImpl toolList : profile.myTools.values()) {
                ToolsImpl tools = myTools.get(toolList.getShortName());
                ScopeToolState defaultState = toolList.getDefaultState();
                tools.setDefaultState(copyToolSettings(defaultState.getTool()), defaultState.isEnabled(), defaultState.getLevel());
                tools.removeAllScopes();
                List<ScopeToolState> nonDefaultToolStates = toolList.getNonDefaultTools();
                if (nonDefaultToolStates != null) {
                    for (ScopeToolState state : nonDefaultToolStates) {
                        InspectionToolWrapper toolWrapper = copyToolSettings(state.getTool());
                        NamedScope scope = state.getScope(project);
                        if (scope != null) {
                            tools.addTool(scope, toolWrapper, state.isEnabled(), state.getLevel());
                        }
                        else {
                            tools.addTool(state.getScopeId(), toolWrapper, state.isEnabled(), state.getLevel());
                        }
                    }
                }
                tools.setEnabled(toolList.isEnabled());
            }
        }
        catch (WriteExternalException | InvalidDataException e) {
            LOG.error(e);
        }
    }

    @Override
    public void cleanup(@Nonnull Project project) {
        for (ToolsImpl toolList : myTools.values()) {
            if (toolList.isEnabled()) {
                for (InspectionToolWrapper toolWrapper : toolList.getAllTools()) {
                    toolWrapper.cleanup(project);
                }
            }
        }
    }

    public void enableTool(@Nonnull String toolId, Project project) {
        ToolsImpl tools = getTools(toolId, project);
        tools.setEnabled(true);
        if (tools.getNonDefaultTools() == null) {
            tools.getDefaultState().setEnabled(true);
        }
    }

    @Override
    public void enableTool(@Nonnull String inspectionTool, NamedScope namedScope, Project project) {
        getTools(inspectionTool, project).enableTool(namedScope, project);
    }

    public void enableTools(@Nonnull List<String> inspectionTools, NamedScope namedScope, Project project) {
        for (String inspectionTool : inspectionTools) {
            enableTool(inspectionTool, namedScope, project);
        }
    }

    @Override
    public void disableTool(@Nonnull String inspectionTool, NamedScope namedScope, @Nonnull Project project) {
        getTools(inspectionTool, project).disableTool(namedScope, project);
    }

    public void disableTools(@Nonnull List<String> inspectionTools, NamedScope namedScope, @Nonnull Project project) {
        for (String inspectionTool : inspectionTools) {
            disableTool(inspectionTool, namedScope, project);
        }
    }

    @Override
    public void disableTool(@Nonnull String inspectionTool, Project project) {
        ToolsImpl tools = getTools(inspectionTool, project);
        tools.setEnabled(false);
        if (tools.getNonDefaultTools() == null) {
            tools.getDefaultState().setEnabled(false);
        }
    }

    @Override
    public void setErrorLevel(@Nonnull HighlightDisplayKey key, @Nonnull HighlightDisplayLevel level, Project project) {
        getTools(key.toString(), project).setLevel(level);
    }

    @Override
    public boolean isToolEnabled(HighlightDisplayKey key, PsiElement element) {
        if (key == null) {
            return false;
        }
        Tools toolState = getTools(key.toString(), element == null ? null : element.getProject());
        return toolState != null && toolState.isEnabled(element);
    }

    @Override
    public boolean isToolEnabled(HighlightDisplayKey key) {
        return isToolEnabled(key, null);
    }

    @Override
    public boolean isExecutable(Project project) {
        initInspectionTools(project);
        for (Tools tools : myTools.values()) {
            if (tools.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    //invoke when isChanged() == true
    @Override
    public void commit() throws IOException {
        LOG.assertTrue(mySource != null);
        mySource.commit(this);
        getProfileManager().updateProfile(mySource);
        mySource = null;
    }

    private void commit(@Nonnull InspectionProfileImpl inspectionProfile) {
        setName(inspectionProfile.getName());
        setDescription(inspectionProfile.getDescription());
        setProjectLevel(inspectionProfile.isProjectLevel());
        myLockedProfile = inspectionProfile.myLockedProfile;
        myDisplayLevelMap = inspectionProfile.myDisplayLevelMap;
        myBaseProfile = inspectionProfile.myBaseProfile;
        myTools = inspectionProfile.myTools;
        myProfileManager = inspectionProfile.getProfileManager();

        myExternalInfo.copy(inspectionProfile.getExternalInfo());

        InspectionProfileManager.getInstance().fireProfileChanged(inspectionProfile);
    }

    @Tag
    public String getDescription() {
        return myDescription;
    }

    public void setDescription(String description) {
        myDescription = description;
    }

    @Override
    public void convert(@Nonnull Element element, @Nonnull Project project) {
        initInspectionTools(project);
        Element scopes = element.getChild(DefaultProjectProfileManager.SCOPES);
        if (scopes == null) {
            return;
        }
        List children = scopes.getChildren(SCOPE);
        for (Object s : children) {
            Element scopeElement = (Element) s;
            String profile = scopeElement.getAttributeValue(DefaultProjectProfileManager.PROFILE);
            if (profile != null) {
                InspectionProfileImpl inspectionProfile = (InspectionProfileImpl) getProfileManager().getProfile(profile);
                if (inspectionProfile != null) {
                    NamedScope scope = getProfileManager().getScopesManager().getScope(scopeElement.getAttributeValue(NAME));
                    if (scope != null) {
                        for (InspectionToolWrapper toolWrapper : inspectionProfile.getInspectionTools(null)) {
                            HighlightDisplayKey key = toolWrapper.getHighlightDisplayKey();
                            try {
                                InspectionToolWrapper toolWrapperCopy = copyToolSettings(toolWrapper);
                                HighlightDisplayLevel errorLevel = inspectionProfile.getErrorLevel(key, null, project);
                                getTools(toolWrapper.getShortName(), project).addTool(scope,
                                    toolWrapperCopy,
                                    inspectionProfile.isToolEnabled(key),
                                    errorLevel);
                            }
                            catch (Exception e) {
                                LOG.error(e);
                            }
                        }
                    }
                }
            }
        }
        reduceConvertedScopes();
    }

    private void reduceConvertedScopes() {
        for (ToolsImpl tools : myTools.values()) {
            ScopeToolState toolState = tools.getDefaultState();
            List<ScopeToolState> nonDefaultTools = tools.getNonDefaultTools();
            if (nonDefaultTools != null) {
                boolean equal = true;
                boolean isEnabled = toolState.isEnabled();
                for (ScopeToolState state : nonDefaultTools) {
                    isEnabled |= state.isEnabled();
                    if (!state.equalTo(toolState)) {
                        equal = false;
                    }
                }
                tools.setEnabled(isEnabled);
                if (equal) {
                    tools.removeAllScopes();
                }
            }
        }
    }

    @Override
    @Nonnull
    public ExternalInfo getExternalInfo() {
        return myExternalInfo;
    }

    @Nonnull
    public List<ScopeToolState> getAllTools(Project project) {
        initInspectionTools(project);
        List<ScopeToolState> result = new ArrayList<>();
        for (Tools tools : myTools.values()) {
            result.addAll(tools.getTools());
        }
        return result;
    }

    @Nonnull
    public List<ScopeToolState> getDefaultStates(Project project) {
        initInspectionTools(project);
        List<ScopeToolState> result = new ArrayList<>();
        for (Tools tools : myTools.values()) {
            result.add(tools.getDefaultState());
        }
        return result;
    }

    @Nonnull
    public List<ScopeToolState> getNonDefaultTools(@Nonnull String shortName, Project project) {
        List<ScopeToolState> result = new ArrayList<>();
        List<ScopeToolState> nonDefaultTools = getTools(shortName, project).getNonDefaultTools();
        if (nonDefaultTools != null) {
            result.addAll(nonDefaultTools);
        }
        return result;
    }

    public boolean isToolEnabled(@Nonnull HighlightDisplayKey key, NamedScope namedScope, Project project) {
        return getTools(key.toString(), project).isEnabled(namedScope, project);
    }

    @Deprecated
    public void removeScope(@Nonnull String toolId, int scopeIdx, Project project) {
        getTools(toolId, project).removeScope(scopeIdx);
    }

    public void removeScope(@Nonnull String toolId, @Nonnull String scopeName, Project project) {
        getTools(toolId, project).removeScope(scopeName);
    }

    public void removeScopes(@Nonnull List<String> toolIds, @Nonnull String scopeName, Project project) {
        for (String toolId : toolIds) {
            removeScope(toolId, scopeName, project);
        }
    }

    /**
     * @return null if it has no base profile
     */
    @Nullable
    private Map<String, Boolean> getDisplayLevelMap() {
        if (myBaseProfile == null) {
            return null;
        }
        if (myDisplayLevelMap == null) {
            initInspectionTools(null);
            myDisplayLevelMap = new TreeMap<>();
            for (String toolId : myTools.keySet()) {
                myDisplayLevelMap.put(toolId, toolSettingsAreEqual(toolId, myBaseProfile, this));
            }
        }
        return myDisplayLevelMap;
    }

    @Override
    public void profileChanged() {
        myDisplayLevelMap = null;
    }

    @Nonnull
    @Transient
    public HighlightDisplayLevel getErrorLevel(@Nonnull HighlightDisplayKey key, NamedScope scope, Project project) {
        ToolsImpl tools = getTools(key.toString(), project);
        return tools != null ? tools.getLevel(scope, project) : HighlightDisplayLevel.WARNING;
    }

    public ScopeToolState addScope(@Nonnull InspectionToolWrapper toolWrapper,
                                   NamedScope scope,
                                   @Nonnull HighlightDisplayLevel level,
                                   boolean enabled,
                                   Project project) {
        return getTools(toolWrapper.getShortName(), project).prependTool(scope, toolWrapper, enabled, level);
    }

    public void setErrorLevel(@Nonnull HighlightDisplayKey key, @Nonnull HighlightDisplayLevel level, String scopeName, Project project) {
        getTools(key.toString(), project).setLevel(level, scopeName, project);
    }

    public void setErrorLevel(@Nonnull List<HighlightDisplayKey> keys,
                              @Nonnull HighlightDisplayLevel level,
                              String scopeName,
                              Project project) {
        for (HighlightDisplayKey key : keys) {
            setErrorLevel(key, level, scopeName, project);
        }
    }

    public ToolsImpl getTools(@Nonnull String toolId, Project project) {
        initInspectionTools(project);
        return myTools.get(toolId);
    }

    public void enableAllTools(Project project) {
        for (InspectionToolWrapper entry : getInspectionTools(null)) {
            enableTool(entry.getShortName(), project);
        }
    }

    public void disableAllTools(Project project) {
        for (InspectionToolWrapper entry : getInspectionTools(null)) {
            disableTool(entry.getShortName(), project);
        }
    }

    @Override
    @Nonnull
    public String toString() {
        return mySource == null ? getName() : getName() + " (copy)";
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && ((InspectionProfileImpl) o).getProfileManager() == getProfileManager();
    }
}
