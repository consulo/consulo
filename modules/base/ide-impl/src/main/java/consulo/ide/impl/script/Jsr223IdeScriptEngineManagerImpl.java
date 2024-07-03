/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.script;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.concurrent.PooledThreadExecutor;
import consulo.application.util.function.Computable;
import consulo.application.util.function.ThrowableComputable;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.util.lang.ClassLoaderUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringHash;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@Singleton
@ServiceImpl
public class Jsr223IdeScriptEngineManagerImpl extends IdeScriptEngineManager {
  private static final Logger LOG = Logger.getInstance(IdeScriptEngineManager.class);

  private final Future<ScriptEngineManager> myManagerFuture = PooledThreadExecutor.getInstance().submit(() -> {
    long start = System.currentTimeMillis();
    try {
      return new ScriptEngineManager();
    }
    finally {
      long end = System.currentTimeMillis();
      LOG.info(ScriptEngineManager.class.getName() + " initialized in " + (end - start) + " ms");
    }
  });

  @Nonnull
  @Override
  public List<String> getLanguages() {
    return ContainerUtil.map(getScriptEngineManager().getEngineFactories(), ScriptEngineFactory::getLanguageName);
  }

  @Nonnull
  @Override
  public List<String> getFileExtensions(@Nullable String language) {
    List<String> extensions = new ArrayList<>();
    List<ScriptEngineFactory> factories = getScriptEngineManager().getEngineFactories();
    for (ScriptEngineFactory factory : factories) {
      if (language == null || factory.getLanguageName().equals(language)) {
        extensions.addAll(factory.getExtensions());
      }
    }
    return extensions;
  }

  @Nullable
  @Override
  public IdeScriptEngine getEngineForLanguage(@Nonnull final String language, @Nullable ClassLoader loader) {
    ClassLoader l = ObjectUtil.notNull(loader, AllPluginsLoader.INSTANCE);
    return ClassLoaderUtil.runWithClassLoader(
      l,
      (Computable<IdeScriptEngine>)() -> createIdeScriptEngine(getScriptEngineManager().getEngineByName(language))
    );
  }

  @Nullable
  @Override
  public IdeScriptEngine getEngineForFileExtension(@Nonnull final String extension, @Nullable ClassLoader loader) {
    ClassLoader l = ObjectUtil.notNull(loader, AllPluginsLoader.INSTANCE);
    return ClassLoaderUtil.runWithClassLoader(
      l,
      (Computable<IdeScriptEngine>)()  -> createIdeScriptEngine(getScriptEngineManager().getEngineByExtension(extension))
    );
  }

  @Override
  public boolean isInitialized() {
    return myManagerFuture.isDone();
  }

  @Nonnull
  private ScriptEngineManager getScriptEngineManager() {
    ScriptEngineManager manager = null;
    try {
      manager = myManagerFuture.get();
    }
    catch (Exception e) {
      LOG.error(e);
    }
    return ObjectUtil.assertNotNull(manager);
  }

  @Nullable
  private static IdeScriptEngine createIdeScriptEngine(@Nullable ScriptEngine engine) {
    return engine == null ? null : redirectOutputToLog(new Jsr223IdeScriptEngine(engine));
  }

  private static IdeScriptEngine redirectOutputToLog(IdeScriptEngine engine) {
    engine.setStdOut(new MyAbstractWriter() {
      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        LOG.info(new String(cbuf, off, len));
      }
    });
    engine.setStdErr(new MyAbstractWriter() {
      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        LOG.warn(new String(cbuf, off, len));
      }
    });
    return engine;
  }

  static class Jsr223IdeScriptEngine implements IdeScriptEngine {
    private final ScriptEngine myEngine;
    private final ClassLoader myLoader;

    Jsr223IdeScriptEngine(ScriptEngine engine) {
      myEngine = engine;
      myLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public Object getBinding(@Nonnull String name) {
      return myEngine.get(name);
    }

    @Override
    public void setBinding(@Nonnull String name, Object value) {
      myEngine.put(name, value);
    }

    @Nonnull
    @Override
    public Writer getStdOut() {
      return myEngine.getContext().getWriter();
    }

    @Override
    public void setStdOut(@Nonnull Writer writer) {
      myEngine.getContext().setWriter(writer);
    }

    @Nonnull
    @Override
    public Writer getStdErr() {
      return myEngine.getContext().getErrorWriter();
    }

    @Override
    public void setStdErr(@Nonnull Writer writer) {
      myEngine.getContext().setErrorWriter(writer);
    }

    @Nonnull
    @Override
    public Reader getStdIn() {
      return myEngine.getContext().getReader();
    }

    @Override
    public void setStdIn(@Nonnull Reader reader) {
      myEngine.getContext().setReader(reader);
    }

    @Nonnull
    @Override
    public String getLanguage() {
      return myEngine.getFactory().getLanguageName();
    }

    @Nonnull
    @Override
    public List<String> getFileExtensions() {
      return myEngine.getFactory().getExtensions();
    }

    @Override
    public Object eval(@Nonnull final String script) throws IdeScriptException {
      return ClassLoaderUtil.runWithClassLoader(myLoader, (ThrowableComputable<Object, IdeScriptException>)()  -> {
        try {
          return myEngine.eval(script);
        }
        catch (Throwable e) {
          throw new IdeScriptException(e);
        }
      });
    }
  }

  private static abstract class MyAbstractWriter extends Writer {
    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
  }

  static class AllPluginsLoader extends ClassLoader {
    static final AllPluginsLoader INSTANCE = new AllPluginsLoader();

    final Map<Long, ClassLoader> myLuckyGuess = ContainerUtil.newConcurrentMap();

    public AllPluginsLoader() {
      // Groovy performance: do not specify parent loader to enable our luckyGuesser
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      //long ts = System.currentTimeMillis();

      int p0 = name.indexOf("$");
      int p1 = p0 > 0 ? name.indexOf("$", p0 + 1) : -1;
      String base = p0 > 0 ? name.substring(0, Math.max(p0, p1)) : name;
      long hash = StringHash.calc(base);

      ClassLoader loader = myLuckyGuess.get(hash);
      if (loader == this) throw new ClassNotFoundException(name);

      Class<?> c = null;
      if (loader != null) {
        try {
          c = loader.loadClass(name);
        }
        catch (ClassNotFoundException ignored) {
        }
      }
      if (c == null) {
        boolean first = true;
        for (PluginDescriptor descriptor : PluginManager.getPlugins()) {
          ClassLoader l = descriptor.getPluginClassLoader();
          if (l == null || l == loader) continue;
          try {
            l.loadClass(base);

            if (first) {
              myLuckyGuess.put(hash, l);
            }
            first = false;
            try {
              c = l.loadClass(name);
              break;
            }
            catch (ClassNotFoundException e) {
              if (p0 > 0) break;
              if (name.startsWith("java.") || name.startsWith("groovy.")) break;
            }
          }
          catch (ClassNotFoundException ignored) {
          }
        }
        if (first && loader == null) {
          myLuckyGuess.put(hash, this);
        }
      }

      //LOG.info("AllPluginsLoader [" + StringUtil.formatDuration(System.currentTimeMillis() - ts) + "]: " + (c != null ? "+" : "-") + name);
      if (c != null) return c;
      myLuckyGuess.put(StringHash.calc(name), this);

      throw new ClassNotFoundException(name);
    }

    @Override
    protected URL findResource(String name) {
      return getClass().getClassLoader().getResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
      return getClass().getClassLoader().getResources(name);
    }
  }
}
