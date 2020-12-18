/*
* Copyright 2000-2013 JetBrains s.r.o.
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

#include "stdafx.h"
#include "WinLauncher.h"

typedef JNIIMPORT jint(JNICALL *JNI_createJavaVM)(JavaVM **pvm, JNIEnv **env, void *args);

std::string EMPTY_STRING = "";

WCHAR* moduleFilePath = NULL;
WCHAR* propertiesFilePath = NULL;
WCHAR* workingDirectoryPath = NULL;
WCHAR* appHomePath = NULL;

HINSTANCE hInst; // Current instance.
char jvmPath[_MAX_PATH] = "";
JavaVMOption* vmOptions = NULL;
int vmOptionCount = 0;
bool bServerJVM = false;
HMODULE hJVM = NULL;
JNI_createJavaVM pCreateJavaVM = NULL;
JavaVM* jvm = NULL;
JNIEnv* env = NULL;
volatile bool terminating = false;

HANDLE hFileMapping;
HANDLE hEvent;
HANDLE hSingleInstanceWatcherThread;
const int FILE_MAPPING_SIZE = 16000;

// java 9 module mode
#define RUN_IN_MODULE_MODE

#ifndef RUN_IN_MODULE_MODE
#define BOOTCLASSPATH "consulo-bootstrap.jar;consulo-container-api.jar;consulo-container-impl.jar;consulo-desktop-bootstrap.jar;consulo-util-nodep.jar"
#endif

#define CONSULO_JRE "CONSULO_JRE"

#ifdef _M_X64
bool need64BitJRE = true;
#define BITS_STR "64-bit"
#else
bool need64BitJRE = false;
#define BITS_STR "32-bit"
#endif

std::string EncodeWideACP(const std::wstring &str)
{
  int cbANSI = WideCharToMultiByte(CP_ACP, 0, str.c_str(), str.size(), NULL, 0, NULL, NULL);
  char* ansiBuf = new char[cbANSI];
  WideCharToMultiByte(CP_ACP, 0, str.c_str(), str.size(), ansiBuf, cbANSI, NULL, NULL);
  std::string result(ansiBuf, cbANSI);
  delete[] ansiBuf;
  return result;
}

std::string LoadStdString(int id)
{
  wchar_t *buf = NULL;
  int len = LoadStringW(hInst, id, reinterpret_cast<LPWSTR>(&buf), 0);
  return len ? EncodeWideACP(std::wstring(buf, len)) : "";
}

bool FileExists(const std::string& path)
{
  return GetFileAttributesA(path.c_str()) != INVALID_FILE_ATTRIBUTES;
}

bool IsValidJRE(const char* path)
{
  std::string dllPath(path);
  if (dllPath[dllPath.size() - 1] != '\\')
  {
    dllPath += "\\";
  }
  return FileExists(dllPath + "bin\\server\\jvm.dll") || FileExists(dllPath + "bin\\client\\jvm.dll");
}

bool Is64BitJRE(const char* path)
{
  std::string cfgPath(path);
  std::string cfgJava9Path(path);
  cfgPath += "\\lib\\amd64\\jvm.cfg";
  cfgJava9Path += "\\lib\\jvm.cfg";
  return FileExists(cfgPath) || FileExists(cfgJava9Path);
}

bool FindValidJVM(const char* path)
{
  if (IsValidJRE(path))
  {
    strcpy_s(jvmPath, _MAX_PATH - 1, path);
    return true;
  }
  char jrePath[_MAX_PATH];
  strcpy_s(jrePath, path);
  if (jrePath[strlen(jrePath) - 1] != '\\')
  {
    strcat_s(jrePath, "\\");
  }
  strcat_s(jrePath, _MAX_PATH - 1, "jre");
  if (IsValidJRE(jrePath))
  {
    strcpy_s(jvmPath, jrePath);
    return true;
  }
  return false;
}

std::string GetAdjacentDir(char* suffix)
{
	std::wstring path(workingDirectoryPath);

	std::string target = std::string(path.begin(), path.end());

	return target + "\\" + suffix + "\\";
}

std::wstring GetAdjacentDirW(WCHAR* suffix)
{
    std::wstring path(workingDirectoryPath);

    std::wstring target = std::wstring(path.begin(), path.end());

    return target + L"\\" + suffix + L"\\";
}

bool FindJVMInEnvVar(const char* envVarName, bool& result)
{
  char envVarValue[_MAX_PATH];
  if (GetEnvironmentVariableA(envVarName, envVarValue, _MAX_PATH - 1))
  {
    if (FindValidJVM(envVarValue))
    {
      if (Is64BitJRE(jvmPath) != need64BitJRE) return false;
      result = true;
    }
    else
    {
      char buf[_MAX_PATH];
      sprintf_s(buf, "The environment variable %s (with the value of %s) does not point to a valid JVM installation.",
        envVarName, envVarValue);
      std::string error = LoadStdString(IDS_ERROR_LAUNCHING_APP);
      MessageBoxA(NULL, buf, error.c_str(), MB_OK);
      result = false;
    }
    return true;
  }
  return false;
}

bool FindJVMInRegistryKey(std::string key, bool wow64_32)
{
  HKEY hKey;
  int flags = KEY_READ;
  if (wow64_32) flags |= KEY_WOW64_32KEY;
  if (RegOpenKeyExA(HKEY_LOCAL_MACHINE, key.c_str(), 0, KEY_READ, &hKey) != ERROR_SUCCESS) return false;
  char javaHome[_MAX_PATH];
  DWORD javaHomeSize = _MAX_PATH - 1;
  bool success = false;
  if (RegQueryValueExA(hKey, "JavaHome", NULL, NULL, (LPBYTE)javaHome, &javaHomeSize) == ERROR_SUCCESS)
  {
    success = FindValidJVM(javaHome);
  }
  RegCloseKey(hKey);
  return success;
}

std::string FindJRECurrentVersionRegistryKey(bool wow64_32)
{
  HKEY hKey;
  int flags = KEY_READ;
  if (wow64_32) flags |= KEY_WOW64_32KEY;
  if (RegOpenKeyExA(HKEY_LOCAL_MACHINE, "Software\\JavaSoft\\JRE", 0, KEY_READ, &hKey) != ERROR_SUCCESS) return EMPTY_STRING;
  char version[_MAX_PATH];
  DWORD versionSize = _MAX_PATH - 1;
  bool success = false;
  if (RegQueryValueExA(hKey, "CurrentVersion", NULL, NULL, (LPBYTE)version, &versionSize) == ERROR_SUCCESS){
    success = true;
  }
  RegCloseKey(hKey);

  return success ? std::string(version) : EMPTY_STRING;
}

bool FindJVMInRegistryWithVersion(std::string version, bool wow64_32)
{
  bool foundJava = false;

  // java 9+
  foundJava = FindJVMInRegistryKey("Software\\JavaSoft\\JRE\\" + version, wow64_32);

  if (!foundJava) {
    foundJava = FindJVMInRegistryKey("Software\\JavaSoft\\JDK\\" + version, wow64_32);
  }

  // old registry path
  if (!foundJava) {
    foundJava = FindJVMInRegistryKey("Software\\JavaSoft\\Java Runtime Environment\\" + version, wow64_32);
  }
  return foundJava;
}

bool FindJVMInRegistry()
{
#ifndef _M_X64
  if (FindJVMInRegistryWithVersion("11", true))
    return true;
  if (FindJVMInRegistryWithVersion("12", true))
    return true;
  if (FindJVMInRegistryWithVersion("13", true))
      return true;
  if (FindJVMInRegistryWithVersion("14", true))
      return true;
  if (FindJVMInRegistryWithVersion("15", true))
      return true;

  std::string currentVersion2 = FindJRECurrentVersionRegistryKey(true);
  if(currentVersion2 != EMPTY_STRING && FindJVMInRegistryWithVersion(currentVersion2, true)) {
    return true;
  }
#endif

  if (FindJVMInRegistryWithVersion("11", false))
    return true;
  if (FindJVMInRegistryWithVersion("12", false))
    return true;
  if (FindJVMInRegistryWithVersion("13", true))
      return true;
  if (FindJVMInRegistryWithVersion("14", true))
      return true;
  if (FindJVMInRegistryWithVersion("15", true))
      return true;

  std::string currentVersion = FindJRECurrentVersionRegistryKey(false);
  if(currentVersion != EMPTY_STRING && FindJVMInRegistryWithVersion(currentVersion, false)) {
    return true;
  }
  return false;
}

// The following code is taken from http://msdn.microsoft.com/en-us/library/ms684139(v=vs.85).aspx
// and provides a backwards compatible way to check if this application is a 32-bit process running
// on a 64-bit OS
typedef BOOL (WINAPI *LPFN_ISWOW64PROCESS) (HANDLE, PBOOL);

LPFN_ISWOW64PROCESS fnIsWow64Process;

BOOL IsWow64()
{
  BOOL bIsWow64 = FALSE;

  //IsWow64Process is not available on all supported versions of Windows.
  //Use GetModuleHandle to get a handle to the DLL that contains the function
  //and GetProcAddress to get a pointer to the function if available.

  fnIsWow64Process = (LPFN_ISWOW64PROCESS) GetProcAddress(GetModuleHandle(TEXT("kernel32")), "IsWow64Process");

  if (NULL != fnIsWow64Process)
  {
    fnIsWow64Process(GetCurrentProcess(), &bIsWow64);
  }
  return bIsWow64;
}

bool LocateJVM()
{
  bool result;
  if (FindJVMInEnvVar(CONSULO_JRE, result))
  {
    return result;
  }

  std::vector<std::string> jrePaths;
  if(need64BitJRE) jrePaths.push_back(GetAdjacentDir("jre64"));
  jrePaths.push_back(GetAdjacentDir("jre"));
  for(std::vector<std::string>::iterator it = jrePaths.begin(); it != jrePaths.end(); ++it) {
    if (FindValidJVM((*it).c_str()) && Is64BitJRE(jvmPath) == need64BitJRE)
    {
      return true;
    }
  }

  if (FindJVMInEnvVar("JAVA_HOME", result))
  {
    return result;
  }

  if (FindJVMInRegistry())
  {
    return true;
  }

  std::string jvmError;
  jvmError = "No JVM installation found. Please install a " BITS_STR " JRE/JDK 11+.\n"
    "If you already have a JRE/JDK installed, define a JAVA_HOME variable in\n"
    "Computer > System Properties > System Settings > Environment Variables.";

  if (IsWow64())
  {
    // If WoW64, this means we are running a 32-bit program on 64-bit Windows. This may explain
    // why we couldn't locate the JVM.
    jvmError += "\n\nNOTE: We have detected that you are running a 64-bit version of the "
        "Windows operating system but are running the 32-bit executable. This "
        "can prevent you from finding a 64-bit installation of Java. Consider running "
        "the 64-bit version instead, if this is the problem you're encountering.";
  }

  std::string error = LoadStdString(IDS_ERROR_LAUNCHING_APP);
  MessageBoxA(NULL, jvmError.c_str(),  error.c_str(), MB_OK);
  return false;
}

void TrimLine(char* line)
{
  char *p = line + strlen(line) - 1;
  if (p >= line && *p == '\n')
  {
    *p-- = '\0';
  }
  while (p >= line && (*p == ' ' || *p == '\t'))
  {
    *p-- = '\0';
  }
}

bool LoadVMOptionsFile(const TCHAR* path, std::vector<std::string>& vmOptionLines)
{
  FILE *f = _tfopen(path, _T("rt"));
  if (!f) return false;

  char line[_MAX_PATH];
  while (fgets(line, _MAX_PATH, f))
  {
    TrimLine(line);
    if (line[0] == '#') continue;
    if (strcmp(line, "-server") == 0)
    {
      bServerJVM = true;
    }
    else if (strlen(line) > 0)
    {
      vmOptionLines.push_back(line);
    }
  }
  fclose(f);

  return true;
}

std::string CollectBootJars(const std::string& jarList)
{
  std::string bootDir = GetAdjacentDir("boot");
  if (bootDir.size() == 0 || !FileExists(bootDir))
  {
    return "";
  }

  std::string result;
  int pos = 0;
  while (pos < jarList.size())
  {
    int delimiterPos = jarList.find(';', pos);
    if (delimiterPos == std::string::npos)
    {
      delimiterPos = jarList.size();
    }
    if (result.size() > 0)
    {
      result += ";";
    }
    result += bootDir;
    result += jarList.substr(pos, delimiterPos - pos);


    pos = delimiterPos + 1;
  }
  return result;
}

#ifndef RUN_IN_MODULE_MODE
std::string BuildClassPath()
{
  std::string classpathLibs = std::string(BOOTCLASSPATH);
  std::string result = CollectBootJars(classpathLibs);
  return result;
}

void AddClassPathOptions(std::vector<std::string>& vmOptionLines)
{
    std::string classPath = BuildClassPath();
    if (classPath.size() == 0) return;
    vmOptionLines.push_back(std::string("-Djava.class.path=") + classPath);
}
#else

void AddModulePathOptions(std::vector<std::string>& vmOptionLines)
{
    std::wstring bootDir = GetAdjacentDirW(L"boot");
    vmOptionLines.push_back("--module-path=" + EncodeWideACP(bootDir) + ";" + EncodeWideACP(bootDir) + "/spi");
    vmOptionLines.push_back("-Djdk.module.main=consulo.desktop.bootstrap");
    vmOptionLines.push_back("-Dconsulo.module.path.boot=true");
}
#endif

void AddPredefinedVMOptions(std::vector<std::string>& vmOptionLines)
{
	// deprecated options - will removed later
	vmOptionLines.push_back("-Didea.properties.file=" + EncodeWideACP(std::wstring(propertiesFilePath)));
	vmOptionLines.push_back("-Didea.home.path=" + EncodeWideACP(std::wstring(workingDirectoryPath)));

	vmOptionLines.push_back("-Dconsulo.properties.file=" + EncodeWideACP(std::wstring(propertiesFilePath)));
	vmOptionLines.push_back("-Dconsulo.home.path=" + EncodeWideACP(std::wstring(workingDirectoryPath)));
	vmOptionLines.push_back("-Dconsulo.app.home.path=" + EncodeWideACP(std::wstring(appHomePath)));
}

bool LoadVMOptions(WCHAR* targetVmOptionFile)
{
  std::vector<std::wstring> files;

  files.push_back(std::wstring(targetVmOptionFile));

  if (files.size() == 0)
  {
    std::string error = LoadStdString(IDS_ERROR_LAUNCHING_APP);
    MessageBoxA(NULL, "Cannot find VM options file", error.c_str(), MB_OK);
    return false;
  }

  std::wstring used;
  std::vector<std::string> vmOptionLines;
  for (int i = 0; i < files.size(); i++)
  {
    if (GetFileAttributes(files[i].c_str()) != INVALID_FILE_ATTRIBUTES)
    {
      if (LoadVMOptionsFile(files[i].c_str(), vmOptionLines))
      {
        used += (used.size() ? L"," : L"") + files[i];
      }
    }
  }

#ifdef RUN_IN_MODULE_MODE
  std::wstring systemVmOptions = GetAdjacentDirW(L"bin") + L"\\app.vmoptions";

  if (GetFileAttributes(systemVmOptions.c_str()) != INVALID_FILE_ATTRIBUTES)
  {
      LoadVMOptionsFile(systemVmOptions.c_str(), vmOptionLines);
  }
#endif

  // deprecated options - will removed later
  vmOptionLines.push_back(std::string("-Djb.vmOptions=") + EncodeWideACP(used));

  vmOptionLines.push_back(std::string("-Dconsulo.vm.options.files=") + EncodeWideACP(used));

#ifdef RUN_IN_MODULE_MODE
  AddModulePathOptions(vmOptionLines);
#else
  AddClassPathOptions(vmOptionLines);
#endif

  AddPredefinedVMOptions(vmOptionLines);

  vmOptionCount = vmOptionLines.size();
  vmOptions = (JavaVMOption*)malloc(vmOptionCount * sizeof(JavaVMOption));
  for (int i = 0; i < vmOptionLines.size(); i++)
  {
    vmOptions[i].optionString = _strdup(vmOptionLines[i].c_str());
    vmOptions[i].extraInfo = 0;
  }

  return true;
}

bool LoadJVMLibrary()
{
  std::string dllName(jvmPath);
  std::string binDir = dllName + "\\bin";
  std::string serverDllName = binDir + "\\server\\jvm.dll";
  std::string clientDllName = binDir + "\\client\\jvm.dll";
  if ((bServerJVM && FileExists(serverDllName)) || !FileExists(clientDllName))
  {
    dllName = serverDllName;
  }
  else
  {
    dllName = clientDllName;
  }

  // ensure we can find msvcr100.dll which is located in jre/bin directory; jvm.dll depends on it.
  SetCurrentDirectoryA(binDir.c_str());
  hJVM = LoadLibraryA(dllName.c_str());
  if (hJVM)
  {
    pCreateJavaVM = (JNI_createJavaVM) GetProcAddress(hJVM, "JNI_CreateJavaVM");
  }
  if (!pCreateJavaVM)
  {
    std::string jvmError = "Failed to load JVM DLL ";
    jvmError += dllName.c_str();
    jvmError += "\n"
        "If you already have a " BITS_STR " JRE/JDK installed, define a JAVA_HOME variable in "
        "Computer > System Properties > System Settings > Environment Variables.";
    std::string error = LoadStdString(IDS_ERROR_LAUNCHING_APP);
    MessageBoxA(NULL, jvmError.c_str(), error.c_str(), MB_OK);
    return false;
  }
  return true;
}

bool CreateJVM()
{
  JavaVMInitArgs initArgs;
  initArgs.version = JNI_VERSION_9;
  initArgs.options = vmOptions;
  initArgs.nOptions = vmOptionCount;
  initArgs.ignoreUnrecognized = JNI_TRUE;

  int result = pCreateJavaVM(&jvm, &env, &initArgs);

  for (int i = 0; i < vmOptionCount; i++)
  {
    free(vmOptions[i].optionString);
  }
  free(vmOptions);
  vmOptions = NULL;

  if (result != JNI_OK)
  {
    std::stringstream buf;

    buf << "Failed to create JVM: error code " << result << ".\n";
    buf << "JVM Path: " << jvmPath << "\n";
    buf << "If you already have a " BITS_STR " JRE/JDK installed, define a JAVA_HOME variable in \n";
    buf << "Computer > System Properties > System Settings > Environment Variables.";
    std::string error = LoadStdString(IDS_ERROR_LAUNCHING_APP);
    MessageBoxA(NULL, buf.str().c_str(), error.c_str(), MB_OK);
  }

  return result == JNI_OK;
}

jobjectArray PrepareCommandLine()
{
  int numArgs;
  LPWSTR* argv = CommandLineToArgvW(GetCommandLineW(), &numArgs);
  jclass stringClass = env->FindClass("java/lang/String");
  jobjectArray args = env->NewObjectArray(numArgs - 1, stringClass, NULL);
  for (int i = 1; i < numArgs; i++)
  {
    const wchar_t* arg = argv[i];
    env->SetObjectArrayElement(args, i - 1, env->NewString((const jchar *)arg, wcslen(argv[i])));
  }
  return args;
}

bool RunMainClass()
{
  std::string mainClassName = "consulo/desktop/boot/main/Main";
  jclass mainClass = env->FindClass(mainClassName.c_str());
  if (!mainClass)
  {
    char buf[_MAX_PATH + 256];
    sprintf_s(buf, "Could not find main class %s", mainClassName.c_str());
    std::string error = LoadStdString(IDS_ERROR_LAUNCHING_APP);
    MessageBoxA(NULL, buf, error.c_str(), MB_OK);
    return false;
  }

  jmethodID mainMethod = env->GetStaticMethodID(mainClass, "main", "([Ljava/lang/String;)V");
  if (!mainMethod)
  {
    std::string error = LoadStdString(IDS_ERROR_LAUNCHING_APP);
    MessageBoxA(NULL, "Could not find main method", error.c_str(), MB_OK);
    return false;
  }

  jobjectArray args = PrepareCommandLine();
  env->CallStaticVoidMethod(mainClass, mainMethod, args);
  jthrowable exc = env->ExceptionOccurred();
  if (exc)
  {
    std::string error = LoadStdString(IDS_ERROR_LAUNCHING_APP);
    MessageBoxA(NULL, "Error invoking main method", error.c_str(), MB_OK);
  }

  return true;
}

void CallCommandLineProcessor(const std::wstring& curDir, const std::wstring& args)
{
  JNIEnv *env;
  JavaVMAttachArgs attachArgs;
  attachArgs.version = JNI_VERSION_1_2;
  attachArgs.name = "WinLauncher external command processing thread";
  attachArgs.group = NULL;
  jvm->AttachCurrentThread((void**)&env, &attachArgs);

  std::string processorClassName = "consulo/desktop/boot/main/windows/WindowsCommandLineProcessor";
  jclass processorClass = env->FindClass(processorClassName.c_str());
  if (processorClass)
  {
    jmethodID processMethodID = env->GetStaticMethodID(processorClass, "processWindowsLauncherCommandLine", "(Ljava/lang/String;Ljava/lang/String;)V");
    if (processMethodID)
    {
      jstring jCurDir = env->NewString((const jchar *)curDir.c_str(), curDir.size());
      jstring jArgs = env->NewString((const jchar *)args.c_str(), args.size());
      env->CallStaticVoidMethod(processorClass, processMethodID, jCurDir, jArgs);
      jthrowable exc = env->ExceptionOccurred();
      if (exc)
      {
        MessageBox(NULL, _T("Error sending command line to existing instance"), _T("Error"), MB_OK);
      }
    }
  }

  jvm->DetachCurrentThread();
}

DWORD WINAPI SingleInstanceThread(LPVOID args)
{
  while (true)
  {
    WaitForSingleObject(hEvent, INFINITE);
    if (terminating) break;

    wchar_t *view = static_cast<wchar_t *>(MapViewOfFile(hFileMapping, FILE_MAP_ALL_ACCESS, 0, 0, 0));
    if (!view) continue;
    std::wstring command(view);
    int pos = command.find('\n');
    if (pos >= 0)
    {
      std::wstring curDir = command.substr(0, pos);
      std::wstring args = command.substr(pos + 1);

      CallCommandLineProcessor(curDir, args);
    }

    UnmapViewOfFile(view);
  }
  return 0;
}

void SendCommandLineToFirstInstance()
{
  wchar_t curDir[_MAX_PATH];
  GetCurrentDirectoryW(_MAX_PATH - 1, curDir);
  std::wstring command(curDir);
  command += _T("\n");
  command += GetCommandLineW();

  void *view = MapViewOfFile(hFileMapping, FILE_MAP_ALL_ACCESS, 0, 0, 0);
  if (view)
  {
    memcpy(view, command.c_str(), (command.size() + 1) * sizeof(wchar_t));
    UnmapViewOfFile(view);
  }
  SetEvent(hEvent);
}

bool CheckSingleInstance()
{
	std::string temp2 = EncodeWideACP(std::wstring(moduleFilePath));

	char* moduleFileName = (char*)temp2.c_str();

	for (char *p = moduleFileName; *p; p++)
	{
		if (*p == ':' || *p == '\\')
		{
			*p = '_';
		}
	}
	std::string mappingName = std::string("ConsuloLauncherMapping.") + moduleFileName;
	std::string eventName = std::string("ConsuloLauncherEvent.") + moduleFileName;

	hEvent = CreateEventA(NULL, FALSE, FALSE, eventName.c_str());

	hFileMapping = OpenFileMappingA(FILE_MAP_ALL_ACCESS, FALSE, mappingName.c_str());
	if (!hFileMapping)
	{
		hFileMapping = CreateFileMappingA(INVALID_HANDLE_VALUE, NULL, PAGE_READWRITE, 0, FILE_MAPPING_SIZE,
		mappingName.c_str());
		return true;
	}
	else
	{
		SendCommandLineToFirstInstance();
		CloseHandle(hFileMapping);
		CloseHandle(hEvent);
		return false;
	}
}

DWORD parentProcId;
HANDLE parentProcHandle;

BOOL IsParentProcessRunning(HANDLE hProcess)
{
  if (hProcess == NULL) return FALSE;
  DWORD ret = WaitForSingleObject(hProcess, 0);
  return ret == WAIT_TIMEOUT;
}

BOOL CALLBACK EnumWindowsProc(HWND hWnd, LPARAM lParam)
{
  DWORD procId = 0;
  GetWindowThreadProcessId(hWnd, &procId);
  if (parentProcId == procId)
  {
    WINDOWINFO wi;
    wi.cbSize = sizeof(WINDOWINFO);
    GetWindowInfo(hWnd, &wi);
    if ((wi.dwStyle & WS_VISIBLE) != 0)
    {
      HWND *phNewWindow = (HWND *)lParam;
      *phNewWindow = hWnd;
      return FALSE;
    }
  }
  return TRUE;
}

extern "C" int __declspec(dllexport) __cdecl launchConsulo(HINSTANCE hInstance,
                       HINSTANCE hPrevInstance,
                       LPTSTR    lpCmdLine,
                       int       nCmdShow,
					   int argc,
					   WCHAR** wargv,
					   WCHAR* moduleFile,
					   WCHAR* workingDirectory,
					   WCHAR* propertiesFile,
					   WCHAR* vmOptionFile)
{
	int value = MessageBoxW(0, L"Major boot change. Please update Consulo build. Visit page on wiki https://github.com/consulo/consulo/wiki/Major-boot-changes", L"Consulo", MB_OKCANCEL);
	if(value == IDOK)
	{
		ShellExecute(0, 0, L"https://github.com/consulo/consulo/wiki/Major-boot-changes", 0, 0, SW_SHOW);
	}
	return 0;
}

extern "C" int __declspec(dllexport) __cdecl launchConsulo2(HINSTANCE hInstance,
					   HINSTANCE hPrevInstance,
					   LPTSTR lpCmdLine,
					   int nCmdShow,
					   int argc,
					   WCHAR** wargv,
					   WCHAR* moduleFile,
					   WCHAR* workingDirectory,
					   WCHAR* propertiesFile,
					   WCHAR* vmOptionFile,
					   WCHAR* appHome)
{
  UNREFERENCED_PARAMETER(hPrevInstance);

  moduleFilePath = moduleFile;
  workingDirectoryPath = workingDirectory;
  propertiesFilePath = propertiesFile;
  appHomePath = appHome;

  hInst = hInstance;

  if (!CheckSingleInstance()) 
  {
      //MessageBox(NULL, L"CheckSingleInstance Fail", L"Consulo", 0);
	  return 1;
  }

  if (!LocateJVM()) 
  {
	  //MessageBox(NULL, L"Cant locate JVM libraries", L"Consulo", 0);
	  return 1;
  }

  if (!LoadVMOptions(vmOptionFile))
  {
	  MessageBox(NULL, L"Cant load vm options", L"Consulo", 0);
	  return 1;
  }

  if (!LoadJVMLibrary())
  {
	  MessageBox(NULL, L"Cant load JVM library", L"Consulo", 0);
	  return 1;
  }

  if (!CreateJVM())
  {
	  MessageBox(NULL, L"Cant create JVM instance", L"Consulo", 0);
	  return 1;
  }

  hSingleInstanceWatcherThread = CreateThread(NULL, 0, SingleInstanceThread, NULL, 0, NULL);
  if (!RunMainClass()) return 1;

  jvm->DestroyJavaVM();

  terminating = true;
  SetEvent(hEvent);
  WaitForSingleObject(hSingleInstanceWatcherThread, INFINITE);
  CloseHandle(hEvent);
  CloseHandle(hFileMapping);

  return 0;
}
