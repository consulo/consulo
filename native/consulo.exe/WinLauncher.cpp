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
#include <algorithm>

#ifdef _M_X64
const WCHAR* TARGET_LIBRARY = L"consulo64.dll";
#else
const WCHAR* TARGET_LIBRARY = L"consulo.dll";
#endif

bool startsWith(const WCHAR *str, const WCHAR *pre)
{
    size_t lenpre = wcslen(pre),
           lenstr = wcslen(str);
    return lenstr < lenpre ? false : wcsncmp(pre, str, lenpre) == 0;
}

std::wstring getParentPath()
{
    WCHAR buffer[MAX_PATH];

    GetModuleFileNameW(NULL, buffer, MAX_PATH);

	std::wstring buff(buffer);

	std::wstring::size_type pos = buff.find_last_of(L"\\/");

	return buff.substr(0, pos);
}

WCHAR* toArray(std::wstring wstr)
{
	WCHAR* result = new WCHAR[wstr.length() + sizeof(WCHAR)];

	const WCHAR* value = wstr.c_str();

	wcsncpy(result, value, wstr.length());

	result[wstr.length()] = '\0';

	return result;
}

typedef int (__cdecl *launchConsulo)(
					   HINSTANCE hInstance,
                       HINSTANCE hPrevInstance,
                       LPTSTR    lpCmdLine,
                       int       nCmdShow,
					   int argc,
					   WCHAR** wargv,
					   WCHAR* moduleFile,
					   WCHAR* workingDirectory,
					   WCHAR* propertiesFile,
					   WCHAR* vmOptionFile,
					   WCHAR* appHomePath);

void pushAppData(std::vector<std::wstring> *names)
{
	WIN32_FIND_DATA ffd;
	HANDLE hFind = INVALID_HANDLE_VALUE;

	WCHAR buffer[MAX_PATH];
	SHGetSpecialFolderPath(NULL,buffer, CSIDL_APPDATA, FALSE);

	std::wstring str = std::wstring(buffer);

	std::wstring directoryPath = (str + L"\\Consulo Platform\\*");

	hFind = FindFirstFile(directoryPath.c_str(), &ffd);

	if (INVALID_HANDLE_VALUE == hFind)
	{
		return;
	}

	while (FindNextFile(hFind, &ffd))
	{
		WCHAR* name;

		name = ffd.cFileName;

		if (ffd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY && startsWith(name, L"build"))
		{
			std::wstring value(ffd.cFileName);

			names->push_back(value);
		}
	}

	FindClose(hFind);
}

int APIENTRY _tWinMain(HINSTANCE hInstance,
                       HINSTANCE hPrevInstance,
                       LPTSTR    lpCmdLine,
                       int       nCmdShow)
{
	WIN32_FIND_DATA ffd;
	HANDLE hFind = INVALID_HANDLE_VALUE;

	std::wstring platformPath = getParentPath() + L"\\platform\\*";
	hFind = FindFirstFile(platformPath.c_str(), &ffd);

	if (INVALID_HANDLE_VALUE == hFind)
	{
		MessageBox(NULL, L"'platform' directory not found. Please visit https://consulo.io/trouble.html", L"Consulo", MB_OK);
		ShellExecute(0, 0, L"https://consulo.io/trouble.htm", 0, 0, SW_SHOW);
		return 1;
	}

	std::vector<std::wstring> bootBuilds;
	std::vector<std::wstring> names;

	while (FindNextFile(hFind, &ffd))
	{
		WCHAR* name;

		name = ffd.cFileName;

		if (ffd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY && startsWith(name, L"build"))
		{
			std::wstring value(ffd.cFileName);

			names.push_back(value);
			bootBuilds.push_back(value);
		}
	}

	FindClose(hFind);

	pushAppData(&names);

	if(names.size() == 0)
	{
		MessageBox(NULL, L"No platform builds for run", L"Consulo", MB_OK);
		return 2;
	}

	std::sort(names.begin(), names.end());

	std::wstring last = names.back();

	WCHAR buffer[MAX_PATH];

	GetModuleFileName(NULL, buffer, MAX_PATH);
	std::wstring module(buffer);

	std::wstring vmOptionsFile = module + L".vmoptions";
	std::wstring propertiesFile = getParentPath() + L"\\consulo.properties";

	if (GetFileAttributes(vmOptionsFile.c_str()) == INVALID_FILE_ATTRIBUTES)
    {
		MessageBox(NULL, L"vmoptions file not found", L"Consulo", MB_OK);
		return 3;
    }

	if (GetFileAttributes(propertiesFile.c_str()) == INVALID_FILE_ATTRIBUTES)
    {
		MessageBox(NULL, L"'consulo.properties' file not found", L"Consulo", MB_OK);
		return 4;
    }

	std::wstring workDirectory;

	if(std::find(bootBuilds.begin(), bootBuilds.end(), last) != bootBuilds.end())
	{
		workDirectory = getParentPath() +  L"\\platform\\" + last;
	}
	else
	{
		WCHAR buffer[MAX_PATH];
		SHGetSpecialFolderPath(NULL,buffer, CSIDL_APPDATA,FALSE);

		std::wstring str = std::wstring(buffer);
		workDirectory = str + L"\\Consulo Platform\\" + last;
	}

	std::wstring targetLibrary = workDirectory + L"\\bin\\" + TARGET_LIBRARY;

	HMODULE libraryHandle = LoadLibrary(targetLibrary.c_str());
	if(!libraryHandle)
	{
		MessageBox(NULL, L"Target library is not found", L"Consulo", MB_OK);
		return 5;
	}

	launchConsulo func = (launchConsulo)GetProcAddress(libraryHandle, "launchConsulo2");
	if(!func)
	{
		MessageBox(NULL, L"'launchConsulo2' function is not found. Please visit https://consulo.io/trouble.html", L"Consulo", MB_OK);
		ShellExecute(0, 0, L"https://consulo.io/trouble.htm", 0, 0, SW_SHOW);
		return 6;
	}

	WCHAR* vmOptionFileW = toArray(vmOptionsFile);
	WCHAR* workDirectoryW = toArray(workDirectory);
	WCHAR* propertiesFileW = toArray(propertiesFile);
	WCHAR* appHomePath = toArray(getParentPath());

	return func(hInstance, hPrevInstance, lpCmdLine, nCmdShow, __argc, __wargv, buffer, workDirectoryW, propertiesFileW, vmOptionFileW, appHomePath);
}