/*
 * Copyright (C) 2015 zheng qian <xqq@0ginr.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _VERSION_UTILS_HPP
#define _VERSION_UTILS_HPP

#include <cstdint>
#include <sys/system_properties.h>

const int ICE_CREAM_SANDWICH = 14;      // Android 4.0
const int ICE_CREAM_SANDWICH_MR1 = 15;  // Android 4.0.3
const int JELLY_BEAN = 16;              // Android 4.1
const int JELLY_BEAN_MR1 = 17;          // Android 4.2
const int JELLY_BEAN_MR2 = 18;          // Android 4.3
const int KITKAT = 19;                  // Android 4.4
const int KITKAT_WATCH = 20;            // Android 4.4W
const int LOLLIPOP = 21;                // Android 5.0

struct AndroidVersion {
    int8_t major = 0;
    int8_t minor = 0;
    int8_t revision = 0;
};

int getDeviceApiLevel();
AndroidVersion getDeviceAndroidVersion();

#endif // _VERSION_UTILS_HPP
