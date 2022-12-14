# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# - Find CRCUTIL (crcutil/include.h, libcrcutil.a)
# This module defines
#  CRCUTIL_INCLUDE_DIR, directory containing headers
#  CRCUTIL_SHARED_LIB, path to libcrcutil's shared library
#  CRCUTIL_STATIC_LIB, path to libcrcutil's static library
#  CRCUTIL_FOUND, whether crcutil has been found

find_path(CRCUTIL_INCLUDE_DIR crcutil/interface.h
  PATHS ${CRCUTIL_ROOT}/include
  NO_CMAKE_SYSTEM_PATH
  NO_SYSTEM_ENVIRONMENT_PATH)
find_library(CRCUTIL_SHARED_LIB crcutil
  PATHS ${CRCUTIL_ROOT}/lib
  NO_CMAKE_SYSTEM_PATH
  NO_SYSTEM_ENVIRONMENT_PATH)
find_library(CRCUTIL_STATIC_LIB libcrcutil.a
  PATHS ${CRCUTIL_ROOT}/lib
  NO_CMAKE_SYSTEM_PATH
  NO_SYSTEM_ENVIRONMENT_PATH)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(Crcutil REQUIRED_VARS
  CRCUTIL_SHARED_LIB CRCUTIL_STATIC_LIB CRCUTIL_INCLUDE_DIR)
