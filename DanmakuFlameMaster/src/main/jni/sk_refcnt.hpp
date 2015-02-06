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

#ifndef _SK_REFCNT_HPP
#define _SK_REFCNT_HPP

#include <cstdint>
#include <cassert>
#include <malloc.h>

#ifndef NDEBUG
    #define DbgAssert(x) assert(x)
#else
    #define DbgAssert(x)
#endif

class SkNoncopyable_t {
public:
    SkNoncopyable_t() {}
private:
    SkNoncopyable_t(const SkNoncopyable_t&) = delete;
    SkNoncopyable_t& operator=(const SkNoncopyable_t&) = delete;
};

class SkRefCnt_t : SkNoncopyable_t {
public:
    SkRefCnt_t() : mRefCnt(1) { }

    virtual ~SkRefCnt_t() {
#ifndef NDEBUG
        DbgAssert(mRefCnt == 1);
        mRefCnt = 0;
#endif
    }

    int32_t getRefCnt() const {
        return mRefCnt;
    }

    bool unique() const {
        bool const unique = (mRefCnt == 1);
        return unique;
    }

    void ref() const {
        DbgAssert(mRefCnt > 0);
        sk_atomic_inc(&mRefCnt);
    }

    void unref() const {
        DbgAssert(mRefCnt > 0);
        if (sk_atomic_dec(&mRefCnt) == 1) {
            internalDispose();
        }
    }

    void unref(void* dtor) const {
        DbgAssert(mRefCnt > 0);
        if (sk_atomic_dec(&mRefCnt) == 1) {
            if (dtor) {
                typedef void (*Dtor)(void* obj);
                Dtor destructor = reinterpret_cast<Dtor>(dtor);
                destructor((void*)this);
                free((void*)this);
            }
        }
    }

    void validate() const {
        DbgAssert(mRefCnt > 0);
    }

    void deref() const {
        this->unref();
    }
private:
    void internalDispose() const {
        free((void*)this);
    }
private:
    static __attribute__((always_inline)) int32_t sk_atomic_inc(int32_t* addr) {
        return __sync_fetch_and_add(addr, 1);
    }

    static __attribute__((always_inline)) int32_t sk_atomic_dec(int32_t* addr) {
        return __sync_fetch_and_add(addr, -1);
    }
private:
    mutable int32_t mRefCnt;
};

template <typename T> static inline void Sk_SafeUnref(T* obj) {
    if (obj) {
        obj->unref();
    }
}

template <typename T> static inline void Sk_SafeUnref(T* obj, void* dtor) {
    if (obj) {
        obj->unref(dtor);
    }
}

#endif // _SK_REFCNT_HPP
