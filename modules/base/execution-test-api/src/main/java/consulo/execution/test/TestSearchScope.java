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
package consulo.execution.test;

import consulo.execution.configuration.ModuleBasedConfiguration;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

/**
 * @author dyoma
 */
public interface TestSearchScope {
  SourceScope getSourceScope(ModuleBasedConfiguration configuration);

  TestSearchScope WHOLE_PROJECT = new TestSearchScope() {
    public SourceScope getSourceScope(ModuleBasedConfiguration configuration) {
      return SourceScope.wholeProject(configuration.getProject());
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public String toString() {
      return "WHOLE_PROJECT";
    }
  };

  TestSearchScope SINGLE_MODULE = new TestSearchScope() {
    public SourceScope getSourceScope(ModuleBasedConfiguration configuration) {
      return SourceScope.modules(configuration.getModules());
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public String toString() {
      return "SINGLE_MODULE";
    }
  };

  TestSearchScope MODULE_WITH_DEPENDENCIES = new TestSearchScope() {
    public SourceScope getSourceScope(ModuleBasedConfiguration configuration) {
      return SourceScope.modulesWithDependencies(configuration.getModules());
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public String toString() {
      return "MODULE_WITH_DEPENDENCIES";
    }
  };



  class Wrapper implements JDOMExternalizable {
    @NonNls private static final String DEFAULT_NAME = "defaultName";
    private TestSearchScope myScope = MODULE_WITH_DEPENDENCIES;
    @NonNls private static final String WHOLE_PROJECT_NAE = "wholeProject";
    @NonNls private static final String SINGLE_MODULE_NAME = "singleModule";
    @NonNls private static final String MODULE_WITH_DEPENDENCIES_NAME = "moduleWithDependencies";

    public void readExternal(Element element) throws InvalidDataException {
      String value = element.getAttributeValue(DEFAULT_NAME);
      if (SINGLE_MODULE_NAME.equals(value)) myScope = SINGLE_MODULE;
      else if (MODULE_WITH_DEPENDENCIES_NAME.equals(value)) myScope = MODULE_WITH_DEPENDENCIES;
      else myScope = WHOLE_PROJECT;
    }

    public void writeExternal(Element element) throws WriteExternalException {
      String name = WHOLE_PROJECT_NAE;
      if (myScope == SINGLE_MODULE) name = SINGLE_MODULE_NAME;
      else if (myScope == MODULE_WITH_DEPENDENCIES) name = MODULE_WITH_DEPENDENCIES_NAME;
      element.setAttribute(DEFAULT_NAME, name);
    }

    public TestSearchScope getScope() {
      return myScope;
    }

    public void setScope(TestSearchScope scope) {
      myScope = scope;
    }
  }
}
