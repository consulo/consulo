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

package consulo.language.copyright.impl.internal.pattern;

import consulo.language.copyright.config.CopyrightManager;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.StringWriter;
import java.util.Properties;

public class VelocityHelper {
  private static VelocityEngine instance;

  private VelocityHelper() {
  }

  @Nonnull
  public static String evaluate(@Nullable PsiFile file, @Nullable Project project, @Nullable Module module, @Nonnull String template) throws Exception {
    VelocityEngine engine = getEngine();

    VelocityContext vc = new VelocityContext();
    vc.put("today", new DateInfo());
    if (file != null) vc.put("file", new FileInfo(file));
    if (project != null) vc.put("project", new ProjectInfo(project));
    if (module != null) vc.put("module", new ModuleInfo(module));
    vc.put("username", System.getProperty("user.name"));


    StringWriter sw = new StringWriter();
    boolean stripLineBreak = false;
    if (template.endsWith("$")) {
      template += getVelocitySuffix();
      stripLineBreak = true;
    }
    engine.evaluate(vc, sw, CopyrightManager.class.getName(), template);
    String result = sw.getBuffer().toString();
    return stripLineBreak ? StringUtil.trimEnd(result, getVelocitySuffix()) : result;
  }

  private static String getVelocitySuffix() {
    return "\n";
  }

  private static synchronized VelocityEngine getEngine() {
    if (instance == null) {
      try {
        Properties extendedProperties = new Properties();

        extendedProperties.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
        extendedProperties.setProperty(RuntimeConstants.PARSER_POOL_SIZE, "1");

        extendedProperties.setProperty("file.resource.loader.class", ClasspathResourceLoader.class.getName());
        extendedProperties.setProperty("file.resource.loader.path", "resources");

        VelocityEngine engine = new VelocityEngine(extendedProperties);
        engine.init();

        instance = engine;
      }
      catch (Exception ignored) {
      }
    }

    return instance;
  }
}