cmake_minimum_required(VERSION 3.1)
project(windows-file-system-helper)
include(CheckSymbolExists)

if(NOT WIN32)
  message(FATAL_ERROR "Windows only.")
endif()

if (DEFINED ENV{BUILD_NUMBER})
  set(BUILD_NUMBER $ENV{BUILD_NUMBER})
else()
  set(BUILD_NUMBER 9999)
endif()

string(TIMESTAMP YEAR "%Y")

check_symbol_exists("_M_AMD64" "" TARGET_X64)
check_symbol_exists("_M_ARM64" "" TARGET_ARM64)

if(TARGET_X64)
  set(FILE_NAME "windows-file-system-helper64")
  set(JAVA_HOME $ENV{JDK_18_x64})
elseif(TARGET_ARM64)
  set(FILE_NAME "windows-file-system-helper-aarch64")
  set(JAVA_HOME $ENV{JDK_AARCH64})
else()
  set(FILE_NAME "windows-file-system-helper")
  set(JAVA_HOME $ENV{JDK_18})
endif()
if(NOT JAVA_HOME)
  message(FATAL_ERROR "JDK_18/JDK_18_x64/JDK_AARCH64 variables not defined (x64=${TARGET_X64}, aarch64=${TARGET_ARM64}).")
endif()

set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} /MT")
include_directories(${JAVA_HOME}/include ${JAVA_HOME}/include/win32)

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/IdeaWin32.rc ${CMAKE_CURRENT_BINARY_DIR}/IdeaWin32.rc @ONLY)

add_library(windows-file-system-helper SHARED IdeaWin32.c IdeaWin32.h ${CMAKE_CURRENT_BINARY_DIR}/IdeaWin32.rc)

set_target_properties(windows-file-system-helper PROPERTIES OUTPUT_NAME ${FILE_NAME})