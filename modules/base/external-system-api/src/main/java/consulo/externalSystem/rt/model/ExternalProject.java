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
package consulo.externalSystem.rt.model;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 * @since 7/14/2014
 */
public interface ExternalProject extends Serializable {
    String getExternalSystemId();

    String getName();

    String getQName();

    String getDescription();

    String getGroup();

    String getVersion();

    Map<String, ExternalProject> getChildProjects();

    File getProjectDir();

    File getBuildDir();

    File getBuildFile();

    Map<String, ExternalTask> getTasks();

    //@NotNull
    //Map<String, ExternalConfiguration> getConfigurations();

    //@NotNull
    //List<ExternalRepository> getRepositories();

    //@NotNull
    //List<ExternalDependency> getDependencies();

    Map<String, ExternalPlugin> getPlugins();

    //@NotNull
    //ExternalProjectBuild getBuild();

    Map<String, ?> getProperties();

    Object getProperty(String name);

    Map<String, ExternalSourceSet> getSourceSets();
}
