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
package consulo.annotation.component;

/**
 * {@link MessageBus Message buses} may be organised into {@link MessageBus#getParent() hierarchies}. That allows to provide
 * additional messaging features like <code>'broadcasting'</code>. Here it means that messages sent to particular topic within
 * particular message bus may be dispatched to subscribers of the same topic within another message buses.
 * <p/>
 * Current enum holds available broadcasting options.
 *
 * @author max
 * @since 2006-10-22
 */
public enum TopicBroadcastDirection {
  /**
   * The message is dispatched to all subscribers of the target topic registered within the child message buses.
   * <p/>
   * Example:
   * <pre>
   *                         parent-bus &lt;--- topic1
   *                          /       \
   *                         /         \
   *    topic1 ---&gt; child-bus1     child-bus2 &lt;--- topic1
   * </pre>
   * <p/>
   * Here subscribers of the <code>'topic1'</code> registered within the <code>'child-bus1'</code> and <code>'child-bus2'</code>
   * will receive the message sent to the <code>'topic1'</code> topic at the <code>'parent-bus'</code>.
   */
  TO_CHILDREN,

  /**
   * No broadcasting is performed for the
   */
  NONE,

  /**
   * The message send to particular topic at particular bus is dispatched to all subscribers of the same topic within the parent bus.
   * <p/>
   * Example:
   * <pre>
   *           parent-bus &lt;--- topic1
   *                |
   *            child-bus &lt;--- topic1
   * </pre>
   * <p/>
   * Here subscribers of the <code>topic1</code> registered within <code>'parent-bus'</code> will receive messages posted
   * to the same topic within <code>'child-bus'</code>.
   */
  TO_PARENT
}