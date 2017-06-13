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

    GetModuleFileName(NULL, buffer, MAX_PATH);

	std::wstring buff(buffer);

	std::wstring::size_type pos = buff.find_last_of(L"\\/");

	return buff.substr(0, pos);
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
					   WCHAR* vmOptionFile);

int APIENTRY _tWinMain(HINSTANCE hInstance,
                       HINSTANCE hPrevInstance,
                       LPTSTR    lpCmdLine,
                       int       nCmdShow)
{
	WIN32_FIND_DATA ffd;
	HANDLE hFind = INVALID_HANDLE_VALUE;

	hFind = FindFirstFile(L"platform\\*", &ffd);
	
	if (INVALID_HANDLE_VALUE == hFind) 
	{
		MessageBox(NULL, L"'platform' directory not found", L"Consulo", MB_OK);
		return 1;
	} 
	
	std::vector<std::wstring> names;

	int i = 0;
	while (FindNextFile(hFind, &ffd))
	{
		WCHAR* name;

		name = ffd.cFileName;
		
		if (ffd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY && startsWith(name, L"build"))
		{
			std::wstring value(ffd.cFileName);

			names.push_back(value);
		}
	}

	FindClose(hFind);

	if(names.size() == 0)
	{
		MessageBox(NULL, L"No platform builds for run", L"Consulo", MB_OK);
		return 2;
	}

	std::sort(names.begin(), names.end());

	std::wstring last = names.back();

	TCHAR buffer[_MAX_PATH];

	GetModuleFileName(NULL, buffer, _MAX_PATH);
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

	std::wstring workDirectory = getParentPath() +  L"\\platform\\" + last;

	std::wstring targetLibrary = workDirectory + L"\\bin\\" + TARGET_LIBRARY;

	HMODULE libraryHandle = LoadLibrary(targetLibrary.c_str());
	if(!libraryHandle)
	{
		MessageBox(NULL, L"Target library is not found", L"Consulo", MB_OK);
		return 5;
	}

	launchConsulo func = (launchConsulo)GetProcAddress(libraryHandle, "launchConsulo");
	if(!func)
	{
		MessageBox(NULL, L"'launchConsulo' function is not found", L"Consulo", MB_OK);
		return 6;
	}

	WCHAR* vmOptionFileW = (WCHAR*)vmOptionsFile.c_str();
	WCHAR* workDirectoryW = (WCHAR*)workDirectory.c_str();
	WCHAR* propertiesFileW = (WCHAR*)propertiesFile.c_str();

	return func(hInstance, hPrevInstance, lpCmdLine, nCmdShow, __argc, __wargv, buffer, workDirectoryW, propertiesFileW, vmOptionFileW);
}