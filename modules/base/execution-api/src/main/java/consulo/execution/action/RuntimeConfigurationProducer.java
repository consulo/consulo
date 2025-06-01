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

package consulo.execution.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.*;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.language.psi.PsiElement;
import consulo.module.Module;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @deprecated please use {@link RunConfigurationProducer} instead
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class RuntimeConfigurationProducer implements Comparable<RuntimeConfigurationProducer>, Cloneable {
  public static final ExtensionPointName<RuntimeConfigurationProducer> RUNTIME_CONFIGURATION_PRODUCER = ExtensionPointName.create(RuntimeConfigurationProducer.class);

  public static final Comparator<RuntimeConfigurationProducer> COMPARATOR = new ProducerComparator();
  protected static final int PREFERED = -1;
  private final ConfigurationFactory myConfigurationFactory;
  private RunnerAndConfigurationSettings myConfiguration;
  protected boolean isClone;

  public RuntimeConfigurationProducer(final ConfigurationType configurationType) {
    this(configurationType.getConfigurationFactories()[0]);
  }

  protected RuntimeConfigurationProducer(ConfigurationFactory configurationFactory) {
    myConfigurationFactory = configurationFactory;
  }

  public RuntimeConfigurationProducer createProducer(final Location location, final ConfigurationContext context) {
    final RuntimeConfigurationProducer result = clone();
    result.myConfiguration = location != null ? result.createConfigurationByElement(location, context) : null;

    if (result.myConfiguration != null) {
      final PsiElement psiElement = result.getSourceElement();
      final Location<PsiElement> _location = PsiLocation.fromPsiElement(psiElement, location != null ? location.getModule() : null);
      if (_location != null) {
        // replace with existing configuration if any
        final RunManager runManager = RunManager.getInstance(context.getProject());
        final ConfigurationType type = result.myConfiguration.getType();
        final List<RunnerAndConfigurationSettings> configurations = runManager.getConfigurationSettingsList(type);
        final RunnerAndConfigurationSettings configuration = result.findExistingByElement(_location, configurations, context);
        if (configuration != null) {
          result.myConfiguration = configuration;
        }
        else {
          final ArrayList<String> currentNames = new ArrayList<>();
          for (RunnerAndConfigurationSettings configurationSettings : configurations) {
            currentNames.add(configurationSettings.getName());
          }
          result.myConfiguration.setName(RunManager.suggestUniqueName(result.myConfiguration.getName(), currentNames));
        }
      }
    }

    return result;
  }

  @Nullable
  public RunnerAndConfigurationSettings findExistingConfiguration(@Nonnull Location location, ConfigurationContext context) {
    assert isClone;
    final RunManager runManager = RunManager.getInstance(location.getProject());
    final List<RunnerAndConfigurationSettings> configurations = runManager.getConfigurationSettingsList(getConfigurationType());
    return findExistingByElement(location, configurations, context);
  }

  public abstract PsiElement getSourceElement();

  public RunnerAndConfigurationSettings getConfiguration() {
    assert isClone;
    return myConfiguration;
  }

  public void setConfiguration(RunnerAndConfigurationSettings configuration) {
    assert isClone;
    myConfiguration = configuration;
  }

  @Nullable
  protected abstract RunnerAndConfigurationSettings createConfigurationByElement(Location location, ConfigurationContext context);

  @Nullable
  protected RunnerAndConfigurationSettings findExistingByElement(final Location location, @Nonnull final List<RunnerAndConfigurationSettings> existingConfigurations, ConfigurationContext context) {
    assert isClone;
    return null;
  }

  @Override
  public RuntimeConfigurationProducer clone() {
    assert !isClone;
    try {
      RuntimeConfigurationProducer clone = (RuntimeConfigurationProducer)super.clone();
      clone.isClone = true;
      return clone;
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  protected RunnerAndConfigurationSettings cloneTemplateConfiguration(final Project project, @Nullable final ConfigurationContext context) {
    if (context != null) {
      final RunConfiguration original = context.getOriginalConfiguration(myConfigurationFactory.getType());
      if (original != null) {
        final RunConfiguration c = original instanceof DelegatingRuntimeConfiguration ? ((DelegatingRuntimeConfiguration)original).getPeer() : original;
        return RunManager.getInstance(project).createConfiguration(c.clone(), myConfigurationFactory);
      }
    }
    return RunManager.getInstance(project).createRunConfiguration("", myConfigurationFactory);
  }

  protected ConfigurationFactory getConfigurationFactory() {
    return myConfigurationFactory;
  }

  public ConfigurationType getConfigurationType() {
    return myConfigurationFactory.getType();
  }

  public void perform(ConfigurationContext context, Runnable performRunnable) {
    performRunnable.run();
  }

  public static <T extends RuntimeConfigurationProducer> T getInstance(final Class<T> aClass) {
    return RUNTIME_CONFIGURATION_PRODUCER.findExtension(aClass);
  }

  private static class ProducerComparator implements Comparator<RuntimeConfigurationProducer> {
    @Override
    public int compare(final RuntimeConfigurationProducer producer1, final RuntimeConfigurationProducer producer2) {
      final PsiElement psiElement1 = producer1.getSourceElement();
      final PsiElement psiElement2 = producer2.getSourceElement();
      if (doesContain(psiElement1, psiElement2)) return -PREFERED;
      if (doesContain(psiElement2, psiElement1)) return PREFERED;
      return producer1.compareTo(producer2);
    }

    private static boolean doesContain(final PsiElement container, PsiElement element) {
      while ((element = element.getParent()) != null) {
        if (container.equals(element)) return true;
      }
      return false;
    }
  }

  /**
   * @deprecated feel free to pass your configuration to SMTRunnerConsoleProperties directly instead of wrapping in DelegatingRuntimeConfiguration
   */
  public static class DelegatingRuntimeConfiguration<T extends LocatableConfiguration> extends LocatableConfigurationBase implements ModuleRunConfiguration {
    private final T myConfig;

    public DelegatingRuntimeConfiguration(T config) {
      super(config.getProject(), config.getFactory(), config.getName());
      myConfig = config;
    }

    @Nonnull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
      return myConfig.getConfigurationEditor();
    }

    @SuppressWarnings({"CloneDoesntCallSuperClone"})
    @Override
    public DelegatingRuntimeConfiguration<T> clone() {
      return new DelegatingRuntimeConfiguration<>((T)myConfig.clone());
    }

    @Override
    public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment env) throws ExecutionException {
      return myConfig.getState(executor, env);
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
      myConfig.checkConfiguration();
    }

    @Override
    public String suggestedName() {
      return myConfig.suggestedName();
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
      myConfig.readExternal(element);
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
      myConfig.writeExternal(element);
    }

    public T getPeer() {
      return myConfig;
    }

    @Override
    @Nonnull
    public Module[] getModules() {
      return Module.EMPTY_ARRAY;
    }
  }
}
