package consulo.execution.coverage;

import com.intellij.rt.coverage.data.ProjectData;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.execution.configuration.ModuleBasedConfiguration;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopesCore;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.project.content.ProjectRootModificationTracker;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SoftReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * @author anna
 * @since 2010-12-14
 */
public class CoverageSuitesBundle {
    private CoverageSuite[] mySuites;
    private CoverageEngine myEngine;

    private Set<Module> myProcessedModules;

    private CachedValue<GlobalSearchScope> myCachedValue;

    private SoftReference<ProjectData> myData = new SoftReference<>(null);
    private static final Logger LOG = Logger.getInstance(CoverageSuitesBundle.class);

    public CoverageSuitesBundle(CoverageSuite suite) {
        this(new CoverageSuite[]{suite});
    }

    public CoverageSuitesBundle(CoverageSuite[] suites) {
        mySuites = suites;
        LOG.assertTrue(mySuites.length > 0);
        myEngine = mySuites[0].getCoverageEngine();
        for (CoverageSuite suite : suites) {
            CoverageEngine engine = suite.getCoverageEngine();
            LOG.assertTrue(Comparing.equal(engine, myEngine));
        }
    }


    public boolean isValid() {
        for (CoverageSuite suite : mySuites) {
            if (!suite.isValid()) {
                return false;
            }
        }
        return true;
    }


    public long getLastCoverageTimeStamp() {
        long max = 0;
        for (CoverageSuite suite : mySuites) {
            max = Math.max(max, suite.getLastCoverageTimeStamp());
        }
        return max;
    }

    public boolean isCoverageByTestApplicable() {
        for (CoverageSuite suite : mySuites) {
            if (suite.isCoverageByTestApplicable()) {
                return true;
            }
        }
        return false;
    }

    public boolean isCoverageByTestEnabled() {
        for (CoverageSuite suite : mySuites) {
            if (suite.isCoverageByTestEnabled()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public ProjectData getCoverageData() {
        ProjectData projectData = myData.get();
        if (projectData != null) {
            return projectData;
        }
        ProjectData data = new ProjectData();
        for (CoverageSuite suite : mySuites) {
            ProjectData coverageData = suite.getCoverageData(null);
            if (coverageData != null) {
                data.merge(coverageData);
            }
        }
        myData = new SoftReference<>(data);
        return data;
    }

    public boolean isTrackTestFolders() {
        for (CoverageSuite suite : mySuites) {
            if (suite.isTrackTestFolders()) {
                return true;
            }
        }
        return false;
    }

    public boolean isTracingEnabled() {
        for (CoverageSuite suite : mySuites) {
            if (suite.isTracingEnabled()) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public CoverageEngine getCoverageEngine() {
        return myEngine;
    }

    public CoverageAnnotator getAnnotator(Project project) {
        return myEngine.getCoverageAnnotator(project);
    }

    public CoverageSuite[] getSuites() {
        return mySuites;
    }

    public boolean contains(CoverageSuite suite) {
        return ArrayUtil.find(mySuites, suite) > -1;
    }

    public void setCoverageData(ProjectData projectData) {
        myData = new SoftReference<>(projectData);
    }

    public void restoreCoverageData() {
        myData = new SoftReference<>(null);
    }

    public String getPresentableName() {
        return StringUtil.join(mySuites, CoverageSuite::getPresentableName, ", ");
    }

    public boolean isModuleChecked(Module module) {
        return myProcessedModules != null && myProcessedModules.contains(module);
    }

    public void checkModule(Module module) {
        if (myProcessedModules == null) {
            myProcessedModules = new HashSet<>();
        }
        myProcessedModules.add(module);
    }

    @Nullable
    public RunConfigurationBase getRunConfiguration() {
        for (CoverageSuite suite : mySuites) {
            if (suite instanceof BaseCoverageSuite baseCoverageSuite) {
                RunConfigurationBase configuration = baseCoverageSuite.getConfiguration();
                if (configuration != null) {
                    return configuration;
                }
            }
        }
        return null;
    }

    public GlobalSearchScope getSearchScope(final Project project) {
        if (myCachedValue == null) {
            myCachedValue = CachedValuesManager.getManager(project).createCachedValue(
                new CachedValueProvider<GlobalSearchScope>() {
                    @Nullable
                    @Override
                    public Result<GlobalSearchScope> compute() {
                        return new Result<>(getSearchScopeInner(project), ProjectRootModificationTracker.getInstance(project));
                    }
                },
                false
            );
        }
        return myCachedValue.getValue();

    }

    private GlobalSearchScope getSearchScopeInner(Project project) {
        RunConfigurationBase configuration = getRunConfiguration();
        if (configuration instanceof ModuleBasedConfiguration moduleBasedConfiguration) {
            Module module = moduleBasedConfiguration.getConfigurationModule().getModule();
            if (module != null) {
                return GlobalSearchScope.moduleRuntimeScope(module, isTrackTestFolders());
            }
        }
        return isTrackTestFolders() ? GlobalSearchScope.projectScope(project) : GlobalSearchScopesCore.projectProductionScope(project);
    }
}
