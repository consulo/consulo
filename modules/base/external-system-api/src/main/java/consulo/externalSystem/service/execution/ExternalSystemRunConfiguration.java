package consulo.externalSystem.service.execution;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.util.DateFormatUtil;
import consulo.document.FileDocumentManager;
import consulo.execution.DefaultExecutionResult;
import consulo.execution.ExecutionBundle;
import consulo.execution.ExecutionResult;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.LocatableConfigurationBase;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.log.ui.LogConfigurationPanel;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.configuration.ui.SettingsEditorGroup;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.externalSystem.ExternalSystemBundle;
import consulo.externalSystem.internal.ExternalSystemInternalHelper;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.externalSystem.model.execution.ExternalTaskExecutionInfo;
import consulo.externalSystem.model.execution.ExternalTaskPojo;
import consulo.externalSystem.model.task.ExternalSystemTask;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.process.NopProcessHandler;
import consulo.process.ProcessOutputTypes;
import consulo.project.Project;
import consulo.util.io.NetUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.StringUtil;
import consulo.util.nodep.collection.ContainerUtilRt;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.util.xml.serializer.XmlSerializer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 23.05.13 18:30
 */
public class ExternalSystemRunConfiguration extends LocatableConfigurationBase {

  private static final Logger LOG = Logger.getInstance(ExternalSystemRunConfiguration.class);

  private ExternalSystemTaskExecutionSettings mySettings = new ExternalSystemTaskExecutionSettings();

  public ExternalSystemRunConfiguration(@Nonnull ProjectSystemId externalSystemId, Project project, ConfigurationFactory factory, String name) {
    super(project, factory, name);
    mySettings.setExternalSystemIdString(externalSystemId.getId());
  }

  @Override
  public String suggestedName() {
    return AbstractExternalSystemTaskConfigurationType.generateName(getProject(), mySettings);
  }

  @Override
  public RunConfiguration clone() {
    ExternalSystemRunConfiguration result = (ExternalSystemRunConfiguration)super.clone();
    result.mySettings = mySettings.clone();
    return result;
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    super.readExternal(element);
    Element e = element.getChild(ExternalSystemTaskExecutionSettings.TAG_NAME);
    if (e != null) {
      mySettings = XmlSerializer.deserialize(e, ExternalSystemTaskExecutionSettings.class);
    }
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    super.writeExternal(element);
    element.addContent(XmlSerializer.serialize(mySettings));
  }

  @Nonnull
  public ExternalSystemTaskExecutionSettings getSettings() {
    return mySettings;
  }

  @Nonnull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    SettingsEditorGroup<ExternalSystemRunConfiguration> group = new SettingsEditorGroup<>();
    group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"), new ExternalSystemRunConfigurationEditor(getProject(), mySettings.getExternalSystemId()));
    group.addEditor(ExecutionBundle.message("logs.tab.title"), new LogConfigurationPanel<>());
    return group;
  }

  @Nullable
  @Override
  public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment env) throws ExecutionException {
    return new MyRunnableState(mySettings, getProject(), DefaultDebugExecutor.EXECUTOR_ID.equals(executor.getId()));
  }

  public static class MyRunnableState implements RunProfileState {

    @Nonnull
    private final ExternalSystemTaskExecutionSettings mySettings;
    @Nonnull
    private final Project myProject;

    private final int myDebugPort;

    public MyRunnableState(@Nonnull ExternalSystemTaskExecutionSettings settings, @Nonnull Project project, boolean debug) {
      mySettings = settings;
      myProject = project;
      int port;
      if (debug) {
        try {
          port = NetUtil.findAvailableSocketPort();
        }
        catch (IOException e) {
          LOG.warn("Unexpected I/O exception occurred on attempt to find a free port to use for external system task debugging", e);
          port = 0;
        }
      }
      else {
        port = 0;
      }
      myDebugPort = port;
    }

    public int getDebugPort() {
      return myDebugPort;
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @Nonnull ProgramRunner runner) throws ExecutionException {
      if (myProject.isDisposed()) return null;

      ExternalSystemApiUtil.updateRecentTasks(new ExternalTaskExecutionInfo(mySettings.clone(), executor.getId()), myProject);
      ConsoleView console = TextConsoleBuilderFactory.getInstance().createBuilder(myProject).getConsole();
      final List<ExternalTaskPojo> tasks = ContainerUtilRt.newArrayList();
      for (String taskName : mySettings.getTaskNames()) {
        tasks.add(new ExternalTaskPojo(taskName, mySettings.getExternalProjectPath(), null));
      }
      if (tasks.isEmpty()) {
        throw new ExecutionException(ExternalSystemBundle.message("run.error.undefined.task"));
      }
      String debuggerSetup = null;
      if (myDebugPort > 0) {
        debuggerSetup = "-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=" + myDebugPort;
      }

      ApplicationManager.getApplication().assertIsDispatchThread();
      FileDocumentManager.getInstance().saveAllDocuments();

      ExternalSystemInternalHelper helper = Application.get().getInstance(ExternalSystemInternalHelper.class);
      final ExternalSystemTask task = helper.createExecuteSystemTask(mySettings.getExternalSystemId(), myProject, tasks, mySettings.getVmOptions(), mySettings.getScriptParameters(), debuggerSetup);

      final MyProcessHandler processHandler = new MyProcessHandler(task);
      console.attachToProcess(processHandler);

      ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
        @Override
        public void run() {
          final String startDateTime = DateFormatUtil.formatTimeWithSeconds(System.currentTimeMillis());
          final String greeting;
          if (mySettings.getTaskNames().size() > 1) {
            greeting = ExternalSystemBundle.message("run.text.starting.multiple.task", startDateTime, StringUtil.join(mySettings.getTaskNames(), " "));
          }
          else {
            greeting = ExternalSystemBundle.message("run.text.starting.single.task", startDateTime, StringUtil.join(mySettings.getTaskNames(), " "));
          }
          processHandler.notifyTextAvailable(greeting, ProcessOutputTypes.SYSTEM);
          task.execute(new ExternalSystemTaskNotificationListenerAdapter() {

            private boolean myResetGreeting = true;

            @Override
            public void onTaskOutput(@Nonnull ExternalSystemTaskId id, @Nonnull String text, boolean stdOut) {
              if (myResetGreeting) {
                processHandler.notifyTextAvailable("\r", ProcessOutputTypes.SYSTEM);
                myResetGreeting = false;
              }
              processHandler.notifyTextAvailable(text, stdOut ? ProcessOutputTypes.STDOUT : ProcessOutputTypes.STDERR);
            }

            @Override
            public void onFailure(@Nonnull ExternalSystemTaskId id, @Nonnull Exception e) {
              String exceptionMessage = ExceptionUtil.getMessage(e);
              String text = exceptionMessage == null ? e.toString() : exceptionMessage;
              processHandler.notifyTextAvailable(text + '\n', ProcessOutputTypes.STDERR);
              processHandler.notifyProcessTerminated(0);
            }

            @Override
            public void onEnd(@Nonnull ExternalSystemTaskId id) {
              final String endDateTime = DateFormatUtil.formatTimeWithSeconds(System.currentTimeMillis());
              final String farewell;
              if (mySettings.getTaskNames().size() > 1) {
                farewell = ExternalSystemBundle.message("run.text.ended.multiple.task", endDateTime, StringUtil.join(mySettings.getTaskNames(), " "));
              }
              else {
                farewell = ExternalSystemBundle.message("run.text.ended.single.task", endDateTime, StringUtil.join(mySettings.getTaskNames(), " "));
              }
              processHandler.notifyTextAvailable(farewell, ProcessOutputTypes.SYSTEM);
              processHandler.notifyProcessTerminated(0);
            }
          });
        }
      });
      return new DefaultExecutionResult(console, processHandler);
    }
  }

  private static class MyProcessHandler extends NopProcessHandler {
    private final ExternalSystemTask myTask;

    public MyProcessHandler(ExternalSystemTask task) {
      myTask = task;
    }

    @Override
    protected void destroyProcessImpl() {
    }

    @Override
    protected void detachProcessImpl() {
      myTask.cancel(new ExternalSystemTaskNotificationListenerAdapter() {

        private boolean myResetGreeting = true;

        @Override
        public void onTaskOutput(@Nonnull ExternalSystemTaskId id, @Nonnull String text, boolean stdOut) {
          if (myResetGreeting) {
            notifyTextAvailable("\r", ProcessOutputTypes.SYSTEM);
            myResetGreeting = false;
          }
          notifyTextAvailable(text, stdOut ? ProcessOutputTypes.STDOUT : ProcessOutputTypes.STDERR);
        }
      });
      notifyProcessDetached();
    }

    @Override
    public boolean detachIsDefault() {
      return true;
    }

    @Nullable
    @Override
    public OutputStream getProcessInput() {
      return null;
    }

    @Override
    public void notifyProcessTerminated(int exitCode) {
      super.notifyProcessTerminated(exitCode);
    }
  }
}
