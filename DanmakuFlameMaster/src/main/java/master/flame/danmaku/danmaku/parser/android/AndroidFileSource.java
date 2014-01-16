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

package master.flame.danmaku.danmaku.parser.android;

import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.danmaku.util.IOUtils;

import android.net.Uri;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.client.HttpClient;

public class AndroidFileSource implements IDataSource<InputStream> {

    private InputStream inStream;

    public AndroidFileSource(String filepath) {
        fillStreamFromFile(new File(filepath));
    }

    public AndroidFileSource(Uri uri) {
        fillStreamFromUri(uri);
    }

    public AndroidFileSource(File file) {
        fillStreamFromFile(file);
    }

    public AndroidFileSource(InputStream stream) {
        this.inStream = stream;
    }

    public void fillStreamFromFile(File file) {
        try {
            inStream = new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void fillStreamFromUri(Uri uri) {
        String scheme = uri.getScheme();
        if (SCHEME_HTTP_TAG.equalsIgnoreCase(scheme) || SCHEME_HTTPS_TAG.equalsIgnoreCase(scheme)) {
            fillStreamFromHttpFile(uri);
        } else if (SCHEME_FILE_TAG.equalsIgnoreCase(scheme)) {
            fillStreamFromFile(new File(uri.getPath()));
        }
    }

    public void fillStreamFromHttpFile(Uri uri) {
        try {
            URL url = new URL(uri.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");
            int code = connection.getResponseCode();
            if(code == 200 || code == 206)
                inStream = new BufferedInputStream(connection.getInputStream());
            
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void release() {
        IOUtils.closeQuietly(inStream);
        inStream = null;
    }

	@Override
	public InputStream data() {
		return inStream;
	}

}
