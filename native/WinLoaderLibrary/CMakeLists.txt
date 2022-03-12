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
  set(JAVA_HOME $ENV{JDK_18_x64})
elseif(TARGET_ARM64)
  set(FILE_NAME "consulo-aarch64")
  set(JAVA_HOME $ENV{JDK_AARCH64})
else()
  set(FILE_NAME "consulo")
  set(JAVA_HOME $ENV{JDK_18})
endif()
if(NOT JAVA_HOME)
  message(FATAL_ERROR "JDK_18/JDK_18_x64/JDK_AARCH64 variables not defined (x64=${TARGET_X64}, aarch64=${TARGET_ARM64}).")
endif()

add_definitions (-DUNICODE -D_UNICODE)

set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} /MT")

include_directories(${JAVA_HOME}/include ${JAVA_HOME}/include/win32)

add_library(${FILE_NAME} SHARED resource.h stdafx.cpp stdafx.h targetver.h WinLauncher.cpp WinLauncher.h WinLauncher.rc)
