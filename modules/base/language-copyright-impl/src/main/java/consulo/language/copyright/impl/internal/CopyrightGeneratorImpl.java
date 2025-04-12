/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.copyright.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.language.copyright.impl.internal.pattern.DateInfo;
import consulo.language.copyright.impl.internal.pattern.FileInfo;
import consulo.language.copyright.impl.internal.pattern.ModuleInfo;
import consulo.language.copyright.impl.internal.pattern.ProjectInfo;
import consulo.language.copyright.internal.CopyrightGenerator;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.util.lang.lazy.LazyValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.StringWriter;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 13-Jun-22
 */
@Singleton
@ServiceImpl
public class CopyrightGeneratorImpl implements CopyrightGenerator {
  private static final Logger LOG = Logger.getInstance(CopyrightGeneratorImpl.class);

  private Supplier<VelocityEngine> myVelocityEngine;

  private final Project myProject;

  @Inject
  public CopyrightGeneratorImpl(Project project) {
    myProject = project;
    myVelocityEngine = LazyValue.notNull(() -> {
      try {
        Properties extendedProperties = new Properties();

        extendedProperties.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
        extendedProperties.setProperty(RuntimeConstants.PARSER_POOL_SIZE, "1");

        extendedProperties.setProperty("file.resource.loader.class", ClasspathResourceLoader.class.getName());
        extendedProperties.setProperty("file.resource.loader.path", "resources");

        VelocityEngine engine = new VelocityEngine(extendedProperties);
        engine.init();

        return engine;
      }
      catch (Exception e) {
        LOG.error(e);
        return new VelocityEngine();
      }
    });
  }

  @Nonnull
  @Override
  public String generate(@Nullable PsiFile file, @Nullable Module module, @Nonnull String template) throws Exception {
    VelocityEngine engine = myVelocityEngine.get();

    VelocityContext vc = new VelocityContext();
    vc.put("today", new DateInfo());
    if (file != null) vc.put("file", new FileInfo(file));
    vc.put("project", new ProjectInfo(myProject));
    if (module != null) vc.put("module", new ModuleInfo(module));
    vc.put("username", Platform.current().user().name());

    StringWriter sw = new StringWriter();
    boolean stripLineBreak = false;
    if (template.endsWith("$")) {
      template += getVelocitySuffix();
      stripLineBreak = true;
    }
    engine.evaluate(vc, sw, CopyrightGeneratorImpl.class.getName(), template);
    final String result = sw.getBuffer().toString();
    return stripLineBreak ? StringUtil.trimEnd(result, getVelocitySuffix()) : result;
  }

  private static String getVelocitySuffix() {
    return "\n";
  }
}
