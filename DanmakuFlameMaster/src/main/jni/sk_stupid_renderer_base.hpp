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

#ifndef _SK_STUPID_RENDERER_BASE_HPP
#define _SK_STUPID_RENDERER_BASE_HPP

#include "sk_stupid_common_def.hpp"

class SkStupidRendererBase {
public:
    SkStupidRendererBase() = default;

    SkStupidRendererBase(SkStupidRendererBase&) = delete;

    virtual ~SkStupidRendererBase() { };

    virtual void setExtraData(void* data) = 0;

    virtual void* getExtraData() = 0;

    virtual bool isDeviceSupported() = 0;

    virtual bool setupBackend(int width, int height, int msaaSampleCount) = 0;

    virtual bool teardownBackend() = 0;

    virtual void updateSize(int width, int height) = 0;

    virtual SkCanvas_t* lockCanvas() = 0;

    virtual void unlockCanvasAndPost(SkCanvas_t* canvas) = 0;
};


#endif // _SK_STUPID_RENDERER_BASE_HPP
