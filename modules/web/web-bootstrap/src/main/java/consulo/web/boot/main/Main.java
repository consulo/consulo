/*
 * Copyright 2013-2020 consulo.io
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
package consulo.web.boot.main;

import consulo.container.boot.ContainerStartup;
import consulo.container.impl.SystemContainerLogger;
import consulo.container.impl.classloader.BootstrapClassLoaderUtil;
import consulo.container.impl.classloader.Java9ModuleProcessor;
import consulo.container.util.StatCollector;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2020-06-01
 */
public class Main {
  public static void main(String[] args) throws Exception {
    // FIXME [VISTALL] we ned this?
    System.setProperty("java.awt.headless", "true");

    // enable separate write thread
    System.setProperty("idea.use.separate.write.thread", "true");

    StatCollector stat = new StatCollector();

    File modulesDirectory = BootstrapClassLoaderUtil.getModulesDirectory();

    Map<String, Object> map = new HashMap<String, Object>();
    map.put(ContainerStartup.ARGS, args);
    map.put(ContainerStartup.STAT_COLLECTOR, stat);

    ContainerStartup containerStartup = BootstrapClassLoaderUtil.buildContainerStartup(map, modulesDirectory, SystemContainerLogger.INSTANCE, Java9ModuleProcessor.EMPTY);

    containerStartup.run(map);
  }
}
