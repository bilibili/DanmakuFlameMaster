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

#ifndef _SK_STUPID_RENDERER_16_HPP
#define _SK_STUPID_RENDERER_16_HPP

#include <pthread.h>
#include "version_utils.hpp"
#include "sk_stupid_renderer_base.hpp"

class SkStupidRenderer_16 : public SkStupidRendererBase {
public:
    static bool supportApi(int api);
public:
    static const int minSdkVersion = 16;
    static const int maxSdkVersion = 17;
public:
    explicit SkStupidRenderer_16(void* nativeHandle /* Reserved*/);

    virtual ~SkStupidRenderer_16() override;

    virtual void setExtraData(void* data) override;

    virtual void* getExtraData() override;

    virtual bool isDeviceSupported() override;

    virtual bool setupBackend(int width, int height, int msaaSampleCount) override;

    virtual bool teardownBackend() override;

    virtual void updateSize(int width, int height) override;

    virtual SkCanvas_t* lockCanvas() override;

    virtual void unlockCanvasAndPost(SkCanvas_t* canvas) override;
private:
    void loadSymbols();

    bool checkSymbols();

    void createSkCanvas();

    void windowSizeChanged();
private:
    void* mExtraData = nullptr;
    int mApiLevel = 0;
    AndroidVersion mAndroidVersion;
    int mWidth = 0, mHeight = 0;
    int mMSAASampleCount = 0;
    void* mLibraryHandle = nullptr;
    bool mSymbolsLoaded = false;
    bool mSymbolsComplete = false;
    SkCanvas_t* mCanvas = nullptr;
    pthread_mutex_t mCanvasMutex;
};

#endif // _SK_STUPID_RENDERER_16_HPP
