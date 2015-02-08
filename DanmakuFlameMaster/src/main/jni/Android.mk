#
# Copyright (C) 2015 zheng qian <xqq@0ginr.com>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LIB_FOLDER:= ../obj/local/$(TARGET_ARCH_ABI)
LOCAL_MODULE    := libndkbitmap
LOCAL_SRC_FILES := $(LOCAL_LIB_FOLDER)/libndkbitmap.so

include $(PREBUILT_SHARED_LIBRARY)


include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARYS := libndkbitmap

LOCAL_MODULE    := DFMACC
LOCAL_SRC_FILES := jni_entry.cpp \
                   ndk_bitmap.cpp \
                   skia_redirector_jni.cpp \
                   version_utils.cpp \
                   sk_stupid_renderer_16.cpp \
                   sk_stupid_renderer_18.cpp \
                   sk_stupid_injector.cpp

LOCAL_CPPFLAGS  := -std=c++11 -O2 -Wall -march=armv7-a
LOCAL_LDLIBS    := -llog -lGLESv2

include $(BUILD_SHARED_LIBRARY)