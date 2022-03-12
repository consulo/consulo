cmake_minimum_required(VERSION 3.4)
project(WinLoaderLibrary)
include(CheckSymbolExists)

if(NOT WIN32)
  message(FATAL_ERROR "Windows only.")
endif()

check_symbol_exists("_M_AMD64" "" TARGET_X64)
check_symbol_exists("_M_ARM64" "" TARGET_ARM64)

if(TARGET_X64)
  set(FILE_NAME "consulo64")
elseif(TARGET_ARM64)
  set(FILE_NAME "consulo-aarch64")
else()
  set(FILE_NAME "consulo")
endif()

add_definitions (-DUNICODE -D_UNICODE)

set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} /MT")

add_executable(${FILE_NAME} WIN32 resource.h stdafx.cpp stdafx.h targetver.h WinLauncher.cpp WinLauncher.h WinLauncher.rc)

target_sources(${FILE_NAME} PRIVATE DpiManifest.xml)