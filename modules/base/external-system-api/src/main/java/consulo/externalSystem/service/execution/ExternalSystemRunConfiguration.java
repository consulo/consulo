package consulo.externalSystem.service.execution;

import consulo.application.Application;
import consulo.application.util.DateFormatUtil;
import consulo.document.FileDocumentManager;
import consulo.execution.DefaultExecutionResult;
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
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.externalSystem.internal.ExternalSystemInternalHelper;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.externalSystem.model.execution.ExternalTaskExecutionInfo;
import consulo.externalSystem.model.execution.ExternalTaskPojo;
import consulo.externalSystem.model.task.ExternalSystemTask;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.process.NopProcessHandler;
import consulo.process.ProcessOutputTypes;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.io.NetUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.util.xml.serializer.XmlSerializer;
import org.jspecify.annotations.Nullable;
import org.jdom.Element;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 23.05.13 18:30
 */
public class ExternalSystemRunConfiguration extends LocatableConfigurationBase {

  private static final Logger LOG = Logger.getInstance(ExternalSystemRunConfiguration.class);

  
  private final ProjectSystemId myExternalSystemId;

  private ExternalSystemTaskExecutionSettings mySettings = new ExternalSystemTaskExecutionSettings();

  public ExternalSystemRunConfiguration(ProjectSystemId externalSystemId, Project project, ConfigurationFactory factory, String name) {
    super(project, factory, name);
    myExternalSystemId = externalSystemId;
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

  
  public ExternalSystemTaskExecutionSettings getSettings() {
    return mySettings;
  }

  
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    SettingsEditorGroup<ExternalSystemRunConfiguration> group = new SettingsEditorGroup<>();
    group.addEditor(
      ExecutionLocalize.runConfigurationConfigurationTabTitle(),
      new ExternalSystemRunConfigurationEditor(getProject(), myExternalSystemId)
    );
    group.addEditor(ExecutionLocalize.logsTabTitle(), new LogConfigurationPanel<>());
    return group;
  }

  @Override
  public @Nullable RunProfileState getState(Executor executor, ExecutionEnvironment env) throws ExecutionException {
    return new MyRunnableState(myExternalSystemId, mySettings, getProject(), DefaultDebugExecutor.EXECUTOR_ID.equals(executor.getId()));
  }

  public static class MyRunnableState implements RunProfileState {

    
    private final ProjectSystemId myExternalSystemId;
    
    private final ExternalSystemTaskExecutionSettings mySettings;
    
    private final Project myProject;

    private final int myDebugPort;

    public MyRunnableState(
      ProjectSystemId externalSystemId,
      ExternalSystemTaskExecutionSettings settings,
      Project project,
      boolean debug
    ) {
      myExternalSystemId = externalSystemId;
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

    @Override
    @RequiredUIAccess
    public @Nullable ExecutionResult execute(Executor executor, ProgramRunner runner) throws ExecutionException {
      if (myProject.isDisposed()) return null;

      ExternalSystemApiUtil.updateRecentTasks(new ExternalTaskExecutionInfo(mySettings.clone(), executor.getId()), myProject);
      ConsoleView console = TextConsoleBuilderFactory.getInstance().createBuilder(myProject).getConsole();
      List<ExternalTaskPojo> tasks = new ArrayList<>();
      for (String taskName : mySettings.getTaskNames()) {
        tasks.add(new ExternalTaskPojo(taskName, mySettings.getExternalProjectPath(), null));
      }
      if (tasks.isEmpty()) {
        throw new ExecutionException(ExternalSystemLocalize.runErrorUndefinedTask().get());
      }
      String debuggerSetup = null;
      if (myDebugPort > 0) {
        debuggerSetup = "-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=" + myDebugPort;
      }

      UIAccess.assertIsUIThread();
      FileDocumentManager.getInstance().saveAllDocuments(UIAccess.current());

      ExternalSystemInternalHelper helper = Application.get().getInstance(ExternalSystemInternalHelper.class);
      ExternalSystemTask task = helper.createExecuteSystemTask(myExternalSystemId, myProject, tasks, mySettings.getVmOptions(), mySettings.getScriptParameters(), debuggerSetup);

      final MyProcessHandler processHandler = new MyProcessHandler(task);
      console.attachToProcess(processHandler);

      myProject.getApplication().executeOnPooledThread((Runnable)() -> {
        String startDateTime = DateFormatUtil.formatTimeWithSeconds(System.currentTimeMillis());
        LocalizeValue greeting = mySettings.getTaskNames().size() > 1
          ? ExternalSystemLocalize.runTextStartingMultipleTask(startDateTime, StringUtil.join(mySettings.getTaskNames(), " "))
          : ExternalSystemLocalize.runTextStartingSingleTask(startDateTime, StringUtil.join(mySettings.getTaskNames(), " "));
        processHandler.notifyTextAvailable(greeting.get(), ProcessOutputTypes.SYSTEM);
        task.execute(new ExternalSystemTaskNotificationListenerAdapter() {

          private boolean myResetGreeting = true;

          @Override
          public void onTaskOutput(ExternalSystemTaskId id, String text, boolean stdOut) {
            if (myResetGreeting) {
              processHandler.notifyTextAvailable("\r", ProcessOutputTypes.SYSTEM);
              myResetGreeting = false;
            }
            processHandler.notifyTextAvailable(text, stdOut ? ProcessOutputTypes.STDOUT : ProcessOutputTypes.STDERR);
          }

          @Override
          public void onFailure(ExternalSystemTaskId id, Exception e) {
            String exceptionMessage = ExceptionUtil.getMessage(e);
            String text = exceptionMessage == null ? e.toString() : exceptionMessage;
            processHandler.notifyTextAvailable(text + '\n', ProcessOutputTypes.STDERR);
            processHandler.notifyProcessTerminated(0);
          }

          @Override
          public void onEnd(ExternalSystemTaskId id) {
            String endDateTime = DateFormatUtil.formatTimeWithSeconds(System.currentTimeMillis());
            LocalizeValue farewell = mySettings.getTaskNames().size() > 1
              ? ExternalSystemLocalize.runTextEndedMultipleTask(endDateTime, StringUtil.join(mySettings.getTaskNames(), " "))
              : ExternalSystemLocalize.runTextEndedSingleTask(endDateTime, StringUtil.join(mySettings.getTaskNames(), " "));
            processHandler.notifyTextAvailable(farewell.get(), ProcessOutputTypes.SYSTEM);
            processHandler.notifyProcessTerminated(0);
          }
        });
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
        public void onTaskOutput(ExternalSystemTaskId id, String text, boolean stdOut) {
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

    @Override
    public @Nullable OutputStream getProcessInput() {
      return null;
    }

    @Override
    public void notifyProcessTerminated(int exitCode) {
      super.notifyProcessTerminated(exitCode);
    }
  }
}
