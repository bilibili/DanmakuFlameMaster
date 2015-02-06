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

#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <android/log.h>
#include <sys/system_properties.h>
#include "version_utils.hpp"

static int gApiLevel = 0;
static AndroidVersion gVersion;

int getDeviceApiLevel() {
    if (gApiLevel != 0) {
        return gApiLevel;
    }

    char buffer[PROP_VALUE_MAX] = {0};
    int api = -1;

    int length = __system_property_get("ro.build.version.sdk", buffer);
    if (length > 0) {
        api = atoi(buffer);
        gApiLevel = api;
    }

    return api;
}

AndroidVersion getDeviceAndroidVersion() {
    if (gVersion.major != 0) {
        return gVersion;
    }

    char buffer[PROP_VALUE_MAX] = {0};
    AndroidVersion version;

    int length = __system_property_get("ro.build.version.release", buffer);
    if (length > 0) {
        const char* delimiter = ".";
        char* saveptr = nullptr;
        char* fragment = strtok_r(buffer, delimiter, &saveptr);
        int i = 0;

        while (fragment) {
            int value = atoi(fragment);
            if (i == 0) {
                version.major = value;
            } else if (i == 1) {
                version.minor = value;
            } else if (i == 2) {
                version.revision = value;
            }

            fragment = strtok_r(nullptr, delimiter, &saveptr);
            i++;
        }
        gVersion = version;
    }

    return version;
}
