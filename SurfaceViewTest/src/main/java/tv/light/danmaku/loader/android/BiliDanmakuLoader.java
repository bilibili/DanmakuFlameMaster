/*
 * Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
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

package tv.light.danmaku.loader.android;

import tv.light.danmaku.loader.ILoader;
import tv.light.danmaku.parser.IDataSource;
import tv.light.danmaku.parser.android.AndroidFileSource;

import android.net.Uri;

public class BiliDanmakuLoader implements ILoader {

    private static BiliDanmakuLoader _instance;

    public static BiliDanmakuLoader instance() {
        if (_instance == null) {
            _instance = new BiliDanmakuLoader();
        }
        return _instance;
    }

    private Uri uri;

    public BiliDanmakuLoader() {

    }

    /**
     * @param uri 弹幕文件地址(http:// file://)
     * @return
     */
    @Override
    public IDataSource load(String uri) {
        try {
            this.uri = Uri.parse(uri);
            return new AndroidFileSource(uri);
        } catch (Exception e) {

        }
        return null;
    }

}
