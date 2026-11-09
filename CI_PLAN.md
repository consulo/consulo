# Continuous Integration API

Status 2026-11-09: started. Platform API and impl for CI servers, so that Jenkins (port of
`MCMicS/jenkins-control-plugin`, Apache 2.0), TeamCity and GitHub Actions plugins share one model, one browser and one
build monitor.

## 1. Decisions (user)

- Full names for modules and packages, like `version-control-system-api`: artifacts `consulo-continuous-integration-api`
  and `consulo-continuous-integration-impl`, Java modules `consulo.continuous.integration.api` / `.impl`, packages
  `consulo.continuousIntegration`, `consulo.continuousIntegration.internal`, `consulo.continuousIntegration.impl.internal`.
  Classes keep a short `CI` prefix (like `AbstractVcs` in the VCS package): `CIServerType`, `CIServer`, `CIJob`,
  `CIBuild`.
- Like the new Tasks API: servers are built on `account-management-api` accounts (URL and token live in the account,
  never in CI state), options are `@CompiledJson` POJOs, every remote call returns `CompletableFuture`, UI is uni UI.
- No Jenkins client library: `jenkins-client` 0.3.8 is from 2018 and pulls XStream, dom4j, json-lib, commons-lang 2;
  `jenkins-rest` 1.0.2 (2022) is jclouds + Guice. Own client on `HttpApiHelper` + Gson, like the Jira tracker.
- Reuse in GitHub: the generic build monitor (process handler + build view in the Run tool window) comes from
  `consulo-github/workflow` (`WorkflowRunProcessHandler`, `WorkflowRunView`) and moves into the platform impl.
  GitHub keeps its YAML language support, run configuration and gutter markers, and drives the shared monitor through
  a GitHub `CIServer`.

## 2. Workspace

`/run/media/vistall/repositories/consulo/continuous-integration/`:

- `consulo` — git worktree of the platform on branch `continuous-integration`.
- `consulo-jenkins` — new plugin repo (local only for now).

Builds follow the maven-build rules: only `mvn package -T 1C -Dmaven.test.skip=true` in the platform worktree, never
`install`, `-pl` or cache flags. The plugin is compiled with `javac` against the worktree `target/` jars until the
platform part is pushed. Nothing is committed or pushed without the user asking.

## 3. API (`modules/base/continuous-integration-api`)

- `CIServerType<O extends CIServerOptions>` — `@ExtensionAPI(APPLICATION)`: id, name, icon, account type id,
  `createOptions()`, `createServer(Project, O)`, `detect(Project, Account)`, detection key, `createEditor(...)`.
- `CIServerOptions` — `@CompiledJson`: `accountId`, `serverUrl`.
- `CIServer` — async client of one server:
  - `loadJobs(@Nullable CIJob parent)` (root or folder children), `loadJob`, `loadBuilds(job, limit)`, `loadBuild`;
  - `loadParameters(job)`, `runBuild(job, CIRunRequest)` -> the started (or queued) build, `stopBuild(build)`;
  - `loadLog(build, @Nullable stage, offset)` -> `CILogChunk { text, nextOffset, complete }`;
  - `testConnection()`, `getCapabilities()`, `getSupport(Class)` for optional parts.
- Optional support interfaces: `CIBuildStagesSupport` (pipeline stages / GitHub jobs), `CIRerunSupport`
  (all / failed only), `CIArtifactsSupport`, `CITestReportSupport`, `CIViewsSupport` (Jenkins views),
  `CIPersonalBuildSupport` (Jenkins patch parameter, TeamCity remote run).
- Model (immutable): `CIJob` (id, name, display name, web URL, `CIJobKind` JOB / FOLDER / BRANCH_FOLDER, buildable,
  status, last build), `CIBuild` (id, number, display name, web URL, `CIBuildStatus`, start, duration, branch,
  revision), `CIBuildStatus` (SUCCESS, UNSTABLE, FAILURE, ABORTED, NOT_BUILT, RUNNING, QUEUED, UNKNOWN),
  `CIParameterDefinition` (name, description, `CIParameterKind` STRING / TEXT / BOOLEAN / CHOICE / PASSWORD / FILE /
  OTHER, default, choices, provider type id), `CIRunRequest` (ref, parameter values), `CIBuildStage`.
- `CIParameterEditorProvider` — `@ExtensionAPI`: editors for provider-specific parameter types (the 13 Jenkins plugin
  parameter types); the standard kinds are handled by the impl.
- `CIManager` — `@ServiceAPI(PROJECT)`: configured + detected servers, listeners.
- `CIBuildMonitor` — `@ServiceAPI(PROJECT)`: run or attach a build and show it in the Run tool window (shared with
  GitHub's run configuration).

## 4. Impl (`modules/base/continuous-integration-impl`)

- Server store in the workspace file (format like `TaskRepositoryStore`), merged with servers detected from accounts.
- Settings page (master/details, account chooser, test connection).
- Tool window "Continuous Integration": tree servers -> folders -> jobs -> builds, toolbar run / stop / log /
  open in browser / refresh, parameters dialog.
- Build monitor: generic process handler (poll build, stream logs, stop, rerun, artifacts) and build view on
  `build-ui-api`, from the GitHub workflow code.
- Notifications on finished builds (polling), later status bar widget, test report view, favourites, changelist
  decorator.

Test server: `https://ci.consulo.io` (Jenkins 2.576, anonymous read). Only read requests from tests; nothing is started
or stopped there without the user asking.

Plugin: id `consulo.jenkins` (like `consulo.gitflow`), Java module `consulo.jenkins`, root artifact
`consulo.jenkins.parent`, plugin artifact `consulo.jenkins`. LICENSE Apache 2.0 and NOTICE crediting Jenkins Control.

## 5. Steps

1. API + impl skeleton; module registration (root pom, sandboxes awt/qt/web, distribution, platform.base(_mac).xml);
   platform build green. **(done 2026-11-09)**
   In progress: impl UI (tool window, actions, run dialog, settings, build monitor, notifications) and the Jenkins plugin.
2. `consulo-jenkins`: account type, server type, REST client (jobs, folders, multibranch, builds, parameters, run,
   stop, progressive log, crumb when needed), standard parameter mapping.
3. Build monitor in the impl (port of the GitHub workflow monitor), Jenkins pipeline stages.
4. Jenkins plugin parameter editors, views, favourites, test reports, notifications, status bar.
5. `consulo-github`: GitHub Actions `CIServerType` + switch the workflow run configuration to `CIBuildMonitor`.
6. TeamCity plugin.
