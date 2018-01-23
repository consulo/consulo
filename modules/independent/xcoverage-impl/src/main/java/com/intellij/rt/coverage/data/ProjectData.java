/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.rt.coverage.data;


import com.intellij.rt.coverage.util.CoverageIOUtil;
import com.intellij.rt.coverage.util.ErrorReporter;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProjectData implements CoverageData, Serializable {
  public static final String PROJECT_DATA_OWNER = "com/intellij/rt/coverage/data/ProjectData";

  private static final MethodCaller TOUCH_LINE_METHOD = new MethodCaller("touchLine", new Class[]{int.class});
  private static final MethodCaller TOUCH_LINES_METHOD = new MethodCaller("touchLines", new Class[]{int[].class});
  private static final MethodCaller TOUCH_SWITCH_METHOD = new MethodCaller("touch", new Class[]{int.class, int.class, int.class});
  private static final MethodCaller TOUCH_JUMP_METHOD = new MethodCaller("touch", new Class[]{int.class, int.class, boolean.class});
  private static final MethodCaller TOUCH_METHOD = new MethodCaller("touch", new Class[]{int.class});
  private static final MethodCaller GET_CLASS_DATA_METHOD = new MethodCaller("getClassData", new Class[]{String.class});
  private static final MethodCaller TRACE_LINE_METHOD = new MethodCaller("traceLine", new Class[]{Object.class, int.class});

  private static boolean ourStopped = false;

  public static ProjectData ourProjectData;
  private File myDataFile;

  /**
   * @noinspection UnusedDeclaration
   */
  private String myCurrentTestName;
  private boolean myTraceLines;
  private boolean mySampling;
  private boolean myDiscovery;
  private Map myTrace;
  private File myTracesDir;

  private ClassesMap myClasses = new ClassesMap();
  private Map myLinesMap;

  private static Object ourProjectDataObject;

  public ClassData getClassData(final String name) {
    return myClasses.get(name);
  }

  public ClassData getOrCreateClassData(String name) {
    ClassData classData = myClasses.get(name);
    if (classData == null) {
      classData = new ClassData(name);
      myClasses.put(name, classData);
    }
    return classData;
  }

  public static ProjectData getProjectData() {
    return ourProjectData;
  }

  public void stop() {
    ourStopped = true;
  }

  public boolean isStopped() {
    return ourStopped;
  }

  public boolean isSampling() {
    return mySampling;
  }

  public boolean isTestDiscovery() {
    return myDiscovery;
  }

  public static ProjectData createProjectData() throws IOException {
    final ProjectData projectData = createProjectData(null, null, false, true);
    projectData.myDiscovery = true;
    return projectData;
  }


  public static ProjectData createProjectData(final File dataFile, final ProjectData initialData, boolean traceLines, boolean isSampling) throws IOException {
    ourProjectData = initialData == null ? new ProjectData() : initialData;
    if (dataFile != null && !dataFile.exists()) {
      final File parentDir = dataFile.getParentFile();
      if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();
      dataFile.createNewFile();
    }
    ourProjectData.mySampling = isSampling;
    ourProjectData.myTraceLines = traceLines;
    ourProjectData.myDataFile = dataFile;
    return ourProjectData;
  }

  public void merge(final CoverageData data) {
    final ProjectData projectData = (ProjectData)data;
    for (Iterator iter = projectData.myClasses.names().iterator(); iter.hasNext(); ) {
      final String key = (String)iter.next();
      final ClassData mergedData = projectData.myClasses.get(key);
      ClassData classData = myClasses.get(key);
      if (classData == null) {
        classData = new ClassData(mergedData.getName());
        myClasses.put(key, classData);
      }
      classData.merge(mergedData);
    }
  }

  public void checkLineMappings() {
    if (myLinesMap != null) {
      for (Iterator iterator = myLinesMap.keySet().iterator(); iterator.hasNext(); ) {
        final String className = (String)iterator.next();
        final ClassData classData = getClassData(className);
        final FileMapData[] fileData = (FileMapData[])myLinesMap.get(className);
        //postpone process main file because its lines would be reset and next files won't be processed correctly
        FileMapData mainData = null;
        for (int i = 0; i < fileData.length; i++) {
          final String fileName = fileData[i].getClassName();
          if (fileName.equals(className)) {
            mainData = fileData[i];
            continue;
          }
          final ClassData classInfo = getOrCreateClassData(fileName);
          classInfo.checkLineMappings(fileData[i].getLines(), classData);
        }

        if (mainData != null) {
          classData.checkLineMappings(mainData.getLines(), classData);
        }
        else {
          ErrorReporter.reportError("Class data was not extracted: " + className, new Throwable());
        }
      }
    }
  }

  public void addLineMaps(String className, FileMapData[] fileDatas) {
    if (myLinesMap == null) {
      myLinesMap = new HashMap();
    }
    myLinesMap.put(className, fileDatas);
  }

  // --------------- used from listeners --------------------- //
  public void testEnded(final String name) {
    if (myTrace == null) return;
    final File traceFile = new File(getTracesDir(), name + ".tr");
    try {
      if (!traceFile.exists()) {
        traceFile.createNewFile();
      }
      DataOutputStream os = null;
      try {
        os = new DataOutputStream(new FileOutputStream(traceFile));
        os.writeInt(myTrace.size());
        for (Iterator it = myTrace.keySet().iterator(); it.hasNext(); ) {
          final Object classData = it.next();
          os.writeUTF(classData.toString());
          final boolean[] lines = (boolean[])myTrace.get(classData);
          int numberOfTraces = 0;
          for (int idx = 0; idx < lines.length; idx++) {
            if (lines[idx]) numberOfTraces++;
          }
          os.writeInt(numberOfTraces);
          for (int idx = 0; idx < lines.length; idx++) {
            final boolean incl = lines[idx];
            if (incl) {
              os.writeInt(idx);
            }
          }
        }
      }
      finally {
        if (os != null) {
          os.close();
        }
      }
    }
    catch (IOException e) {
      ErrorReporter.reportError("Error writing traces to file " + traceFile.getPath(), e);
    }
    finally {
      myTrace = null;
    }
  }

  public void testStarted(final String name) {
    myCurrentTestName = name;
    if (myTraceLines) myTrace = new ConcurrentHashMap();
  }
  //---------------------------------------------------------- //


  private File getTracesDir() {
    if (myTracesDir == null) {
      final String fileName = myDataFile.getName();
      final int i = fileName.lastIndexOf('.');
      final String dirName = i != -1 ? fileName.substring(0, i) : fileName;
      myTracesDir = new File(myDataFile.getParent(), dirName);
      if (!myTracesDir.exists()) {
        myTracesDir.mkdirs();
      }
    }
    return myTracesDir;
  }

  public static String getCurrentTestName() {
    try {
      final Object projectDataObject = getProjectDataObject();
      return (String)projectDataObject.getClass().getDeclaredField("myCurrentTestName").get(projectDataObject);
    }
    catch (Exception e) {
      ErrorReporter.reportError("Current test name was not retrieved:", e);
      return null;
    }
  }

  /**
   * @noinspection UnusedDeclaration
   */
  public Map getClasses() {
    return myClasses.asMap();
  }


  // -----------------------  used from instrumentation  ------------------------------------------------//

  //load ProjectData always through system class loader (null) then user's ClassLoaders won't affect    //
  //IMPORTANT: do not remove reflection, it was introduced to avoid ClassCastExceptions in CoverageData //
  //loaded via user's class loader                                                                      //

  // -------------------------------------------------------------------------------------------------- //

  public static void touchLine(Object classData, int line) {
    if (ourProjectData != null) {
      ((ClassData)classData).touchLine(line);
      return;
    }
    touch(TOUCH_LINE_METHOD, classData, new Object[]{new Integer(line)});
  }

  public static void touchSwitch(Object classData, int line, int switchNumber, int key) {
    if (ourProjectData != null) {
      ((ClassData)classData).touch(line, switchNumber, key);
      return;
    }
    touch(TOUCH_SWITCH_METHOD, classData, new Object[]{new Integer(line), new Integer(switchNumber), new Integer(key)});
  }

  public static void touchJump(Object classData, int line, int jump, boolean hit) {
    if (ourProjectData != null) {
      ((ClassData)classData).touch(line, jump, hit);
      return;
    }
    touch(TOUCH_JUMP_METHOD, classData, new Object[]{new Integer(line), new Integer(jump), new Boolean(hit)});
  }

  public static void trace(Object classData, int line) {
    if (ourProjectData != null) {
      ((ClassData)classData).touch(line);
      ourProjectData.traceLine(classData, line);
      return;
    }

    touch(TOUCH_METHOD, classData, new Object[]{new Integer(line)});
    try {
      final Object projectData = getProjectDataObject();
      TRACE_LINE_METHOD.invoke(projectData, new Object[]{classData, new Integer(line)});
    }
    catch (Exception e) {
      ErrorReporter.reportError("Error tracing class " + classData.toString(), e);
    }
  }

  private static Object touch(final MethodCaller methodCaller, Object classData, final Object[] paramValues) {
    try {
      return methodCaller.invoke(classData, paramValues);
    }
    catch (Exception e) {
      ErrorReporter.reportError("Error in project data collection: " + methodCaller.myMethodName, e);
      return null;
    }
  }

  public static int[] touchClassLines(String className, int[] lines) {
    if (ourProjectData != null) {
      return ourProjectData.getClassData(className).touchLines(lines);
    }
    try {
      final Object projectDataObject = getProjectDataObject();
      Object classData = GET_CLASS_DATA_METHOD.invoke(projectDataObject, new Object[]{className});
      return (int[])touch(TOUCH_LINES_METHOD, classData, new Object[]{lines});
    }
    catch (Exception e) {
      ErrorReporter.reportError("Error in class data loading: " + className, e);
      return lines;
    }
  }

  public static Object loadClassData(String className) {
    if (ourProjectData != null) {
      return ourProjectData.getClassData(className);
    }
    try {
      final Object projectDataObject = getProjectDataObject();
      return GET_CLASS_DATA_METHOD.invoke(projectDataObject, new Object[]{className});
    }
    catch (Exception e) {
      ErrorReporter.reportError("Error in class data loading: " + className, e);
      return null;
    }
  }

  private static Object getProjectDataObject() throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
    if (ourProjectDataObject == null) {
      final Class projectDataClass = Class.forName(ProjectData.class.getName(), false, null);
      ourProjectDataObject = projectDataClass.getDeclaredField("ourProjectData").get(null);
    }
    return ourProjectDataObject;
  }

  public void traceLine(Object classData, int line) {
    if (myTrace != null) {
      synchronized (myTrace) {
        boolean[] lines = (boolean[])myTrace.get(classData);
        if (lines == null) {
          lines = new boolean[line + 20];
          myTrace.put(classData, lines);
        }
        if (lines.length <= line) {
          boolean[] longLines = new boolean[line + 20];
          System.arraycopy(lines, 0, longLines, 0, lines.length);
          lines = longLines;
          myTrace.put(classData, lines);
        }
        lines[line] = true;
      }
    }
  }
  // ----------------------------------------------------------------------------------------------- //

  private static class MethodCaller {
    private Method myMethod;
    private String myMethodName;
    private Class[] myParamTypes;

    private MethodCaller(final String methodName, final Class[] paramTypes) {
      myMethodName = methodName;
      myParamTypes = paramTypes;
    }

    public Object invoke(Object thisObj, final Object[] paramValues) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
      if (myMethod == null) {
        myMethod = findMethod(thisObj.getClass(), myMethodName, myParamTypes);
      }
      return myMethod.invoke(thisObj, paramValues);
    }

    private static Method findMethod(final Class clazz, String name, Class[] paramTypes) throws NoSuchMethodException {
      Method m = clazz.getDeclaredMethod(name, paramTypes);
      // speedup method invocation by calling setAccessible(true)
      m.setAccessible(true);
      return m;
    }
  }

  /**
   * This map provides faster read operations for the case when key is mostly the same
   * object. In our case key is the class name which is the same string with high probability.
   * According to CPU snapshots with usual map we spend a lot of time on equals() operation.
   * This class was introduced to reduce number of equals().
   */
  private static class ClassesMap {
    private static final int POOL_SIZE = 1000;
    private IdentityClassData[] myIdentityArray = new IdentityClassData[POOL_SIZE];
    private final Map myClasses = new HashMap(1000);

    public ClassData get(String name) {
      int idx = Math.abs(name.hashCode() % POOL_SIZE);
      final IdentityClassData lastClassData = myIdentityArray[idx];
      if (lastClassData != null) {
        final ClassData data = lastClassData.getClassData(name);
        if (data != null) return data;
      }

      final ClassData data = (ClassData)myClasses.get(name);
      myIdentityArray[idx] = new IdentityClassData(name, data);
      return data;
    }

    public void put(String name, ClassData data) {
      myClasses.put(name, data);
    }

    public HashMap asMap() {
      return new HashMap(myClasses);
    }

    public Collection names() {
      return myClasses.keySet();
    }
  }

  private static class IdentityClassData {
    private String myClassName;
    private ClassData myClassData;

    private IdentityClassData(String className, ClassData classData) {
      myClassName = className;
      myClassData = classData;
    }

    public ClassData getClassData(String name) {
      if (name == myClassName) {
        return myClassData;
      }
      return null;
    }
  }

  //----------test discovery
  public static final String TRACE_DIR = "org.jetbrains.instrumentation.trace.dir";


  private String myTraceDir = System.getProperty(TRACE_DIR, "");

  public void setTraceDir(String traceDir) {
    myTraceDir = traceDir;
  }

  private final ConcurrentMap myTrace2 = new ConcurrentHashMap();
  private final ConcurrentMap myTrace3 = new ConcurrentHashMap();

  // called from instrumented code during class's static init
  public static boolean[] trace(String className, boolean[] methodFlags, String[] methodNames) {
    return ourProjectData.traceLines(className, methodFlags, methodNames);
  }

  private synchronized boolean[] traceLines(String className, boolean[] methodFlags, String[] methodNames) {
    //System.out.println("Registering " + className);
    //assert methodFlags.length == methodNames.length;
    final boolean[] previousMethodFlags = (boolean[])myTrace2.putIfAbsent(className, methodFlags);

    if (previousMethodFlags != null) {
      //  assert previousMethodFlags.length == methodFlags.length;
      final String[] previousMethodNames = (String[])myTrace3.get(className);
      //assert previousMethodNames != null && previousMethodNames.length == methodNames.length;
    }
    else {
      myTrace3.put(className, methodNames);
    }
    return previousMethodFlags != null ? previousMethodFlags : methodFlags;
  }

  public synchronized void testDiscoveryEnded(final String name) {
    new File(myTraceDir).mkdirs();
    final File traceFile = new File(myTraceDir, name + ".tr");
    try {
      if (!traceFile.exists()) {
        traceFile.createNewFile();
      }
      DataOutputStream os = null;
      try {
        os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(traceFile), 64 * 1024));

        //saveOldTrace(os);

        Map classToUsedMethods = new HashMap();
        for (Iterator iterator = myTrace2.entrySet().iterator(); iterator.hasNext(); ) {
          Map.Entry e = (Map.Entry)iterator.next();
          boolean[] used = (boolean[])e.getValue();
          int usedMethodsCount = 0;

          for (int i = 0; i < used.length; i++) {
            boolean anUsed = used[i];
            if (anUsed) ++usedMethodsCount;
          }

          if (usedMethodsCount > 0) {
            classToUsedMethods.put(e.getKey(), new Integer(usedMethodsCount));
          }
        }

        CoverageIOUtil.writeINT(os, classToUsedMethods.size());
        for (Iterator iterator = myTrace2.entrySet().iterator(); iterator.hasNext(); ) {
          Map.Entry e = (Map.Entry)iterator.next();
          final boolean[] used = (boolean[])e.getValue();
          final String className = (String)e.getKey();

          Integer integer = (Integer)classToUsedMethods.get(className);
          if (integer == null) continue;

          int usedMethodsCount = integer.intValue();

          CoverageIOUtil.writeUTF(os, className);
          CoverageIOUtil.writeINT(os, usedMethodsCount);

          String[] methodNames = (String[])myTrace3.get(className);
          for (int i = 0, len = used.length; i < len; ++i) {
            // we check usedMethodCount here since used can still be updated by other threads
            if (used[i] && usedMethodsCount-- > 0) {
              CoverageIOUtil.writeUTF(os, methodNames[i]);
            }
          }
        }
      }
      finally {
        if (os != null) {
          os.close();
        }
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized void testDiscoveryStarted(final String name) {
    //clearOldTrace();
    for (Iterator iterator = myTrace2.entrySet().iterator(); iterator.hasNext(); ) {
      Object e = iterator.next();
      boolean[] used = (boolean[])((Map.Entry)e).getValue();
      for (int i = 0, len = used.length; i < len; ++i) {
        if (used[i]) used[i] = false;
      }
    }
  }

}
