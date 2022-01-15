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
package com.intellij.execution.testframework.sm.runner;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.testframework.Printer;
import com.intellij.execution.testframework.sm.runner.events.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GeneralIdBasedToSMTRunnerEventsConvertor extends GeneralTestEventsProcessor {

  private final Map<String, Node> myNodeByIdMap = new HashMap<>();
  private final Set<Node> myRunningTestNodes = ContainerUtil.newHashSet();
  private final Set<Node> myRunningSuiteNodes = ContainerUtil.newHashSet();
  private final Node myTestsRootNode;

  private boolean myIsTestingFinished = false;
  private TestProxyPrinterProvider myTestProxyPrinterProvider = null;

  public GeneralIdBasedToSMTRunnerEventsConvertor(Project project,
                                                  @Nonnull SMTestProxy.SMRootTestProxy testsRootProxy,
                                                  @Nonnull String testFrameworkName) {
    super(project, testFrameworkName, testsRootProxy);
    myTestsRootNode = new Node(TreeNodeEvent.ROOT_NODE_ID, null, testsRootProxy);
    myNodeByIdMap.put(myTestsRootNode.getId(), myTestsRootNode);
  }

  @Override
  public void onStartTesting() {
    addToInvokeLater(() -> {
      myTestsRootNode.setState(State.RUNNING, this);
      myTestsRootProxy.setStarted();
      fireOnTestingStarted(myTestsRootProxy);
    });
  }

  @Override
  public void onTestsReporterAttached() {
    addToInvokeLater(() -> fireOnTestsReporterAttached(myTestsRootProxy));
  }

  @Override
  public void onFinishTesting() {
    addToInvokeLater(() -> {
      if (myIsTestingFinished) {
        // has been already invoked!
        return;
      }
      myIsTestingFinished = true;

      // We don't know whether process was destroyed by user
      // or it finished after all tests have been run
      // Lets assume, if at finish all nodes except root suite have final state (passed, failed or ignored),
      // then all is ok otherwise process was terminated by user
      boolean completeTree = isTreeComplete(myRunningTestNodes, myTestsRootProxy);
      if (completeTree) {
        myTestsRootProxy.setFinished();
      } else {
        myTestsRootProxy.setTerminated();
      }
      if (!myRunningTestNodes.isEmpty()) {
        logProblem("Unexpected running nodes: " + myRunningTestNodes);
      }
      myNodeByIdMap.clear();
      myRunningTestNodes.clear();
      myRunningSuiteNodes.clear();

      fireOnTestingFinished(myTestsRootProxy);
    });
    super.onFinishTesting();
  }

  @Override
  public void setPrinterProvider(@Nonnull TestProxyPrinterProvider printerProvider) {
    myTestProxyPrinterProvider = printerProvider;
  }

  @Override
  public void onTestStarted(@Nonnull final TestStartedEvent testStartedEvent) {
    addToInvokeLater(() -> doStartNode(testStartedEvent, false));
  }

  @Override
  public void onSuiteStarted(@Nonnull final TestSuiteStartedEvent suiteStartedEvent) {
    addToInvokeLater(() -> doStartNode(suiteStartedEvent, true));
  }

  private void doStartNode(@Nonnull BaseStartedNodeEvent startedNodeEvent, boolean suite) {
    Node node = findNode(startedNodeEvent);
    if (node != null) {
      if (node.getState() == State.NOT_RUNNING && startedNodeEvent.isRunning()) {
        setNodeAndAncestorsRunning(node);
      }
      else {
        logProblem(startedNodeEvent + " has been already started: " + node + "!");
      }
      return;
    }

    node = createNode(startedNodeEvent, suite);
    if (node == null) return;
    if (startedNodeEvent.isRunning()) {
      setNodeAndAncestorsRunning(node);
    }
  }

  private Node createNode(@Nonnull BaseStartedNodeEvent startedNodeEvent, boolean suite) {
    Node parentNode = findValidParentNode(startedNodeEvent);
    if (parentNode == null) {
      return null;
    }

    String nodeId = validateAndGetNodeId(startedNodeEvent);
    if (nodeId == null) {
      return null;
    }

    String nodeName = startedNodeEvent.getName();
    SMTestProxy childProxy = new SMTestProxy(nodeName, suite, startedNodeEvent.getLocationUrl(), startedNodeEvent.getMetainfo(), true);
    childProxy.putUserData(SMTestProxy.NODE_ID, startedNodeEvent.getId());
    childProxy.setTreeBuildBeforeStart();
    TestProxyPrinterProvider printerProvider = myTestProxyPrinterProvider;
    String nodeType = startedNodeEvent.getNodeType();
    if (printerProvider != null && nodeType != null && nodeName != null) {
      Printer printer = printerProvider.getPrinterByType(nodeType, nodeName, startedNodeEvent.getNodeArgs());
      if (printer != null) {
        childProxy.setPreferredPrinter(printer);
      }
    }

    Node node = new Node(nodeId, parentNode, childProxy);
    myNodeByIdMap.put(startedNodeEvent.getId(), node);
    if (myLocator != null) {
      childProxy.setLocator(myLocator);
    }
    parentNode.getProxy().addChild(childProxy);
    return node;
  }

  @Override
  protected SMTestProxy createSuite(String suiteName, String locationHint, String id, String parentNodeId) {
    Node node = createNode(new TestSuiteStartedEvent(suiteName, id, parentNodeId, locationHint, null, null, null, false), true);
    return node.getProxy();
  }

  @Override
  protected SMTestProxy createProxy(String testName, String locationHint, String id, String parentNodeId) {
    Node node = createNode(new TestStartedEvent(testName, id, parentNodeId, locationHint, null, null, null, false), false);
    return node.getProxy();
  }

  @Nullable
  private Node findValidParentNode(@Nonnull BaseStartedNodeEvent startedNodeEvent) {
    String parentId = startedNodeEvent.getParentId();
    if (parentId == null) {
      logProblem("Parent node id should be defined: " + startedNodeEvent + ".", true);
      return null;
    }
    Node parentNode = myNodeByIdMap.get(parentId);
    if (parentNode == null) {
      logProblem("Parent node is undefined for " + startedNodeEvent + ".", true);
      return null;
    }
    if (parentNode.getState() != State.NOT_RUNNING && parentNode.getState() != State.RUNNING) {
      logProblem("Parent node should be registered or running: " + parentNode + ", " + startedNodeEvent);
      return null;
    }
    return parentNode;
  }

  @Override
  public void onTestFinished(@Nonnull final TestFinishedEvent testFinishedEvent) {
    addToInvokeLater(() -> {
      Node node = findNodeToTerminate(testFinishedEvent);
      if (node != null) {
        SMTestProxy testProxy = node.getProxy();
        final Long duration = testFinishedEvent.getDuration();
        if (duration != null) {
          testProxy.setDuration(duration);
        }
        testProxy.setFrameworkOutputFile(testFinishedEvent.getOutputFile());
        testProxy.setFinished();
        if (node.getState() != State.FAILED) {
          // Don't count the same test twice if 'testFailed' message is followed by 'testFinished' message
          // which may happen if generated TeamCity messages adhere rules from
          //   https://confluence.jetbrains.com/display/TCD10/Build+Script+Interaction+with+TeamCity
          // Anyway, this id-based converter already breaks TeamCity protocol by expecting messages with
          // non-standard TeamCity attributes: 'nodeId'/'parentNodeId' instead of 'name'.
          fireOnTestFinished(testProxy);
        }
        terminateNode(node, State.FINISHED);
      }
    });
  }

  @Override
  public void onSuiteFinished(@Nonnull final TestSuiteFinishedEvent suiteFinishedEvent) {
    addToInvokeLater(() -> {
      Node node = findNodeToTerminate(suiteFinishedEvent);
      if (node != null) {
        SMTestProxy suiteProxy = node.getProxy();
        suiteProxy.setFinished();
        fireOnSuiteFinished(suiteProxy);
        terminateNode(node, State.FINISHED);
      }
    });
  }

  @javax.annotation.Nullable
  private Node findNodeToTerminate(@Nonnull TreeNodeEvent treeNodeEvent) {
    Node node = findNode(treeNodeEvent);
    if (node == null) {
      logProblem("Trying to finish nonexistent node: " + treeNodeEvent);
      return null;
    }
    return node;
  }

  @Override
  public void onUncapturedOutput(@Nonnull final String text, final Key outputType) {
    addToInvokeLater(() -> {
      Node activeNode = findActiveNode();
      SMTestProxy activeProxy = activeNode.getProxy();
      activeProxy.addOutput(text, outputType);
    });
  }

  @Override
  public void onError(@Nonnull final String localizedMessage,
                      @javax.annotation.Nullable final String stackTrace,
                      final boolean isCritical) {
    onError(null, localizedMessage, stackTrace, isCritical);
  }

  public void onError(@Nullable final String nodeId,
                      @Nonnull final String localizedMessage,
                      @Nullable final String stackTrace,
                      final boolean isCritical) {
    addToInvokeLater(() -> {
      SMTestProxy activeProxy = null;
      if (nodeId != null) {
        activeProxy = findProxyById(nodeId);
      }
      if (activeProxy == null) {
        Node activeNode = findActiveNode();
        activeProxy = activeNode.getProxy();
      }
      activeProxy.addError(localizedMessage, stackTrace, isCritical);
    });
  }

  @Override
  public void onTestFailure(@Nonnull final TestFailedEvent testFailedEvent) {
    addToInvokeLater(() -> {
      Node node = findNodeToTerminate(testFailedEvent);
      if (node == null) {
        return;
      }

      SMTestProxy testProxy = node.getProxy();

      String comparisonFailureActualText = testFailedEvent.getComparisonFailureActualText();
      String comparisonFailureExpectedText = testFailedEvent.getComparisonFailureExpectedText();
      String failureMessage = testFailedEvent.getLocalizedFailureMessage();
      String stackTrace = testFailedEvent.getStacktrace();
      if (comparisonFailureActualText != null && comparisonFailureExpectedText != null) {
        testProxy.setTestComparisonFailed(failureMessage, stackTrace, comparisonFailureActualText, comparisonFailureExpectedText, testFailedEvent);
      } else if (comparisonFailureActualText == null && comparisonFailureExpectedText == null) {
        testProxy.setTestFailed(failureMessage, stackTrace, testFailedEvent.isTestError());
      } else {
        logProblem("Comparison failure actual and expected texts should be both null or not null.\n"
                   + "Expected:\n"
                   + comparisonFailureExpectedText + "\n"
                   + "Actual:\n"
                   + comparisonFailureActualText);
      }
      long duration = testFailedEvent.getDurationMillis();
      if (duration >= 0) {
        testProxy.setDuration(duration);
      }
      fireOnTestFinished(testProxy);

      // fire event
      fireOnTestFailed(testProxy);

      terminateNode(node, State.FAILED);
    });
  }

  @Override
  public void onTestIgnored(@Nonnull final TestIgnoredEvent testIgnoredEvent) {
    addToInvokeLater(() -> {
      Node node = findNodeToTerminate(testIgnoredEvent);
      if (node != null) {
        SMTestProxy testProxy = node.getProxy();
        testProxy.setTestIgnored(testIgnoredEvent.getIgnoreComment(), testIgnoredEvent.getStacktrace());
        // fire event
        fireOnTestIgnored(testProxy);
        terminateNode(node, State.IGNORED);
      }
    });
  }

  @Override
  public void onTestOutput(@Nonnull final TestOutputEvent testOutputEvent) {
    addToInvokeLater(() -> {
      Node node = findNode(testOutputEvent);
      if (node == null) {
        logProblem("Test wasn't started! But " + testOutputEvent + "!");
        return;
      }
      SMTestProxy testProxy = node.getProxy();

      if (testOutputEvent.isStdOut()) {
        testProxy.addStdOutput(testOutputEvent.getText(), ProcessOutputTypes.STDOUT);
      } else {
        testProxy.addStdErr(testOutputEvent.getText());
      }
    });
  }

  @Override
  public void onTestsCountInSuite(final int count) {
    addToInvokeLater(() -> fireOnTestsCountInSuite(count));
  }

  @javax.annotation.Nullable
  private String validateAndGetNodeId(@Nonnull TreeNodeEvent treeNodeEvent) {
    String nodeId = treeNodeEvent.getId();
    if (nodeId == null || nodeId.equals(TreeNodeEvent.ROOT_NODE_ID)) {
      logProblem((nodeId == null ? "Missing" : "Illegal") + " nodeId: " + treeNodeEvent, true);
    }
    return nodeId;
  }

  @Nullable
  private Node findNode(@Nonnull TreeNodeEvent treeNodeEvent) {
    String nodeId = validateAndGetNodeId(treeNodeEvent);
    return nodeId != null ? myNodeByIdMap.get(nodeId) : null;
  }

  @javax.annotation.Nullable
  public SMTestProxy findProxyById(@Nonnull String id) {
    Node node = myNodeByIdMap.get(id);
    return node != null ? node.getProxy() : null;
  }

  /*
   * Remove listeners,  etc
   */
  @Override
  public void dispose() {
    super.dispose();
    addToInvokeLater(() -> {
      disconnectListeners();

      if (!myRunningTestNodes.isEmpty()) {
        Application application = ApplicationManager.getApplication();
        if (!application.isHeadlessEnvironment() && !application.isUnitTestMode()) {
          logProblem("Not all events were processed!");
        }
      }
      myRunningTestNodes.clear();
      myRunningSuiteNodes.clear();
      myNodeByIdMap.clear();
    });
  }

  private void setNodeAndAncestorsRunning(@Nonnull Node lowestNode) {
    Node node = lowestNode;
    while (node != null && node != myTestsRootNode && node.getState() == State.NOT_RUNNING) {
      node.setState(State.RUNNING, this);
      SMTestProxy proxy = node.getProxy();
      proxy.setStarted();
      if (proxy.isSuite()) {
        myRunningSuiteNodes.add(node);
        fireOnSuiteStarted(proxy);
      } else {
        myRunningTestNodes.add(lowestNode);
        fireOnTestStarted(proxy);
      }
      node = node.getParentNode();
    }
  }

  private void terminateNode(@Nonnull Node node, @Nonnull State terminateState) {
    node.setState(terminateState, this);
    myRunningTestNodes.remove(node);
    myRunningSuiteNodes.remove(node);
  }

  @Nonnull
  private Node findActiveNode() {
    if (!myRunningTestNodes.isEmpty()) {
      return myRunningTestNodes.iterator().next();
    }
    if (!myRunningSuiteNodes.isEmpty()) {
      return myRunningSuiteNodes.iterator().next();
    }
    return myTestsRootNode;
  }

  private enum State {
    NOT_RUNNING, RUNNING, FINISHED, FAILED, IGNORED
  }

  private static class Node {
    private final String myId;
    private final Node myParentNode;
    private final SMTestProxy myProxy;
    private State myState;

    Node(@Nonnull String id, @javax.annotation.Nullable Node parentNode, @Nonnull SMTestProxy proxy) {
      myId = id;
      myParentNode = parentNode;
      myProxy = proxy;
      myState = State.NOT_RUNNING;
    }

    @Nonnull
    public String getId() {
      return myId;
    }

    @javax.annotation.Nullable
    public Node getParentNode() {
      return myParentNode;
    }

    @Nonnull
    public SMTestProxy getProxy() {
      return myProxy;
    }

    @Nonnull
    public State getState() {
      return myState;
    }

    public void setState(@Nonnull State newState, @Nonnull GeneralIdBasedToSMTRunnerEventsConvertor convertor) {
      // allowed sequences: NOT_RUNNING -> RUNNING or IGNORED; RUNNING -> FINISHED, FAILED or IGNORED; FINISHED <-> FAILED; IGNORED -> FINISHED
      if (myState == State.NOT_RUNNING && newState != State.RUNNING && newState != State.IGNORED ||
          myState == State.RUNNING && newState != State.FINISHED && newState != State.FAILED && newState != State.IGNORED ||
          myState == State.FINISHED && newState != State.FAILED ||
          myState == State.FAILED && newState != State.FINISHED ||
          myState == State.IGNORED && newState != State.FINISHED) {
        convertor.logProblem("Illegal state change [" + myState + " -> " + newState + "]: " + toString(), false);
      }

      if (myState.ordinal() < newState.ordinal()) {
        // for example State.FINISHED comes later than State.FAILED, do not update state in this case
        myState = newState;
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Node node = (Node)o;

      return myId == node.myId;
    }

    @Override
    public int hashCode() {
      return myId.hashCode();
    }

    @Override
    public String toString() {
      return "{" +
             "id=" + myId +
             ", parentId=" + (myParentNode != null ? String.valueOf(myParentNode.getId()) : "<undefined>") +
             ", name='" + myProxy.getName() +
             "', isSuite=" + myProxy.isSuite() +
             ", state=" + myState +
             '}';
    }
  }

}
