/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.execution.action;

import consulo.annotation.UsedInPlugin;
import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.ConfigurationTypeUtil;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.internal.PreferredProducerFind;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.util.ModuleUtilCore;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.List;

/**
 * Context for creating run configurations from a location in the source code.
 *
 * @see RunConfigurationProducer
 */
public class ConfigurationContext {
    private static final Logger LOG = Logger.getInstance(ConfigurationContext.class);
    private final Location<PsiElement> myLocation;
    private RunnerAndConfigurationSettings myConfiguration;
    private boolean myInitialized = false;
    private boolean myMultipleSelection = false;
    private Ref<RunnerAndConfigurationSettings> myExistingConfiguration;
    private final Module myModule;
    private final RunConfiguration myRuntimeConfiguration;
    private final Component myContextComponent;

    public static Key<ConfigurationContext> SHARED_CONTEXT = Key.create("SHARED_CONTEXT");

    private List<ConfigurationFromContext> myConfigurationsFromContext;

    @Nonnull
    @RequiredUIAccess
    public static ConfigurationContext getFromContext(DataContext dataContext) {
        final ConfigurationContext context = new ConfigurationContext(dataContext);
        final DataManager dataManager = DataManager.getInstance();
        ConfigurationContext sharedContext = dataManager.loadFromDataContext(dataContext, SHARED_CONTEXT);
        if (sharedContext == null ||
            sharedContext.getLocation() == null ||
            context.getLocation() == null ||
            !Comparing.equal(sharedContext.getLocation().getPsiElement(), context.getLocation().getPsiElement())) {
            sharedContext = context;
            dataManager.saveInDataContext(dataContext, SHARED_CONTEXT, sharedContext);
        }
        return sharedContext;
    }

    @RequiredUIAccess
    private ConfigurationContext(final DataContext dataContext) {
        myRuntimeConfiguration = dataContext.getData(RunConfiguration.KEY);
        myContextComponent = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        myModule = dataContext.getData(Module.KEY);
        @SuppressWarnings({"unchecked"}) final Location<PsiElement> location = (Location<PsiElement>) dataContext.getData(Location.DATA_KEY);
        if (location != null) {
            myLocation = location;
            Location<?>[] locations = dataContext.getData(Location.DATA_KEYS);
            myMultipleSelection = locations != null && locations.length > 1;
            return;
        }
        final Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            myLocation = null;
            return;
        }
        final PsiElement element = getSelectedPsiElement(dataContext, project);
        if (element == null) {
            myLocation = null;
            return;
        }
        myLocation = new PsiLocation<>(project, myModule, element);
        final PsiElement[] elements = dataContext.getData(PsiElement.KEY_OF_ARRAY);
        if (elements != null) {
            myMultipleSelection = elements.length > 1;
        }
        else {
            final VirtualFile[] files = dataContext.getData(VirtualFile.KEY_OF_ARRAY);
            myMultipleSelection = files != null && files.length > 1;
        }
    }

    @RequiredReadAction
    public ConfigurationContext(PsiElement element) {
        myModule = ModuleUtilCore.findModuleForPsiElement(element);
        myLocation = new PsiLocation<>(element.getProject(), myModule, element);
        myRuntimeConfiguration = null;
        myContextComponent = null;
    }

    @UsedInPlugin
    public boolean containsMultipleSelection() {
        return myMultipleSelection;
    }

    /**
     * Returns the configuration created from this context.
     *
     * @return the configuration, or null if none of the producers were able to create a configuration from this context.
     */
    @Nullable
    public synchronized RunnerAndConfigurationSettings getConfiguration() {
        if (myConfiguration == null && !myInitialized) {
            createConfiguration();
        }
        return myConfiguration;
    }

    private void createConfiguration() {
        LOG.assertTrue(myConfiguration == null);
        final Location location = getLocation();
        myConfiguration = location != null && !DumbService.isDumb(location.getProject()) ? PreferredProducerFind.createConfiguration(location, this) : null;
        myInitialized = true;
    }

    public synchronized void setConfiguration(@Nonnull RunnerAndConfigurationSettings configuration) {
        myConfiguration = configuration;
        myInitialized = true;
    }

    /**
     * Returns the source code location for this context.
     *
     * @return the source code location, or null if no source code fragment is currently selected.
     */
    @Nullable
    public Location getLocation() {
        return myLocation;
    }

    /**
     * Returns the PSI element at caret for this context.
     *
     * @return the PSI element, or null if no source code fragment is currently selected.
     */
    @Nullable
    public PsiElement getPsiLocation() {
        return myLocation != null ? myLocation.getPsiElement() : null;
    }

    /**
     * Finds an existing run configuration matching the context.
     *
     * @return an existing configuration, or null if none was found.
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public RunnerAndConfigurationSettings findExisting() {
        if (myExistingConfiguration != null) {
            return myExistingConfiguration.get();
        }
        myExistingConfiguration = new Ref<>();
        if (myLocation == null) {
            return null;
        }

        final PsiElement psiElement = myLocation.getPsiElement();
        if (!psiElement.isValid()) {
            return null;
        }

        final List<RuntimeConfigurationProducer> producers = findPreferredProducers();
        if (myRuntimeConfiguration != null) {
            if (producers != null) {
                for (RuntimeConfigurationProducer producer : producers) {
                    final RunnerAndConfigurationSettings configuration = producer.findExistingConfiguration(myLocation, this);
                    if (configuration != null && configuration.getConfiguration() == myRuntimeConfiguration) {
                        myExistingConfiguration.set(configuration);
                    }
                }
            }
            for (RunConfigurationProducer producer : RunConfigurationProducer.getProducers(getProject())) {
                RunnerAndConfigurationSettings configuration = producer.findExistingConfiguration(this);
                if (configuration != null && configuration.getConfiguration() == myRuntimeConfiguration) {
                    myExistingConfiguration.set(configuration);
                }
            }
        }
        if (producers != null) {
            for (RuntimeConfigurationProducer producer : producers) {
                final RunnerAndConfigurationSettings configuration = producer.findExistingConfiguration(myLocation, this);
                if (configuration != null) {
                    myExistingConfiguration.set(configuration);
                }
            }
        }
        for (RunConfigurationProducer producer : RunConfigurationProducer.getProducers(getProject())) {
            RunnerAndConfigurationSettings configuration = producer.findExistingConfiguration(this);
            if (configuration != null) {
                myExistingConfiguration.set(configuration);
            }
        }
        return myExistingConfiguration.get();
    }

    @Nullable
    @RequiredUIAccess
    private static PsiElement getSelectedPsiElement(final DataContext dataContext, final Project project) {
        PsiElement element = null;
        final Editor editor = dataContext.getData(Editor.KEY);
        if (editor != null) {
            final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (psiFile != null) {
                final int offset = editor.getCaretModel().getOffset();
                element = psiFile.findElementAt(offset);
                if (element == null && offset > 0 && offset == psiFile.getTextLength()) {
                    element = psiFile.findElementAt(offset - 1);
                }
            }
        }
        if (element == null) {
            final PsiElement[] elements = dataContext.getData(PsiElement.KEY_OF_ARRAY);
            element = elements != null && elements.length > 0 ? elements[0] : null;
        }
        if (element == null) {
            final VirtualFile[] files = dataContext.getData(VirtualFile.KEY_OF_ARRAY);
            if (files != null && files.length > 0) {
                element = PsiManager.getInstance(project).findFile(files[0]);
            }
        }
        return element;
    }

    public RunManager getRunManager() {
        return RunManager.getInstance(getProject());
    }

    public Project getProject() {
        return myLocation.getProject();
    }

    public Module getModule() {
        return myModule;
    }

    public DataContext getDataContext() {
        return DataManager.getInstance().getDataContext(myContextComponent);
    }

    /**
     * Returns original {@link RunConfiguration} from this context.
     * For example, it could be some test framework runtime configuration that had been launched
     * and that had brought a result test tree on which a right-click action was performed.
     *
     * @param type {@link ConfigurationType} instance to filter original runtime configuration by its type
     * @return {@link RunConfiguration} instance, it could be null
     */
    @Nullable
    public RunConfiguration getOriginalConfiguration(@Nullable ConfigurationType type) {
        if (type == null) {
            return myRuntimeConfiguration;
        }
        if (myRuntimeConfiguration != null && ConfigurationTypeUtil.equals(myRuntimeConfiguration.getType(), type)) {
            return myRuntimeConfiguration;
        }
        return null;
    }

    /**
     * Checks if the original run configuration matches the passed type.
     * If the original run configuration is undefined, the check is passed too.
     * An original run configuration is a run configuration associated with given context.
     * For example, it could be a test framework run configuration that had been launched
     * and that had brought a result test tree on which a right-click action was performed (and this context was created). In this case, other run configuration producers might want to not work on such elements.
     *
     * @param type {@link ConfigurationType} instance to match the original run configuration
     * @return true if the original run configuration is of the same type or it's undefined; false otherwise
     */
    public boolean isCompatibleWithOriginalRunConfiguration(@Nonnull ConfigurationType type) {
        return myRuntimeConfiguration == null || ConfigurationTypeUtil.equals(myRuntimeConfiguration.getType(), type);
    }

    @Nullable
    public List<ConfigurationFromContext> getConfigurationsFromContext() {
        if (myConfigurationsFromContext == null) {
            myConfigurationsFromContext = PreferredProducerFind.getConfigurationsFromContext(myLocation, this, true);
        }
        return myConfigurationsFromContext;
    }

    /**
     * The same as {@link #getConfigurationsFromContext()} but this method doesn't search among existing run configurations
     */
    public @Nullable List<ConfigurationFromContext> createConfigurationsFromContext() {
        // At the moment of writing, caching is not needed here, the result is cached outside.
        return PreferredProducerFind.getConfigurationsFromContext(myLocation, this, true, false);
    }

    // region deprecated stuff
    @SuppressWarnings("deprecation")
    private List<RuntimeConfigurationProducer> myPreferredProducers;

    @Deprecated
    @Nullable
    @SuppressWarnings({"deprecation", "unused"})
    public RunnerAndConfigurationSettings updateConfiguration(final RuntimeConfigurationProducer producer) {
        myConfiguration = producer.getConfiguration();
        return myConfiguration;
    }

    @Deprecated
    @Nullable
    @SuppressWarnings("deprecation")
    public List<RuntimeConfigurationProducer> findPreferredProducers() {
        if (myPreferredProducers == null) {
            myPreferredProducers = PreferredProducerFind.findPreferredProducers(myLocation, this, true);
        }
        return myPreferredProducers;
    }
    // endregion
}
