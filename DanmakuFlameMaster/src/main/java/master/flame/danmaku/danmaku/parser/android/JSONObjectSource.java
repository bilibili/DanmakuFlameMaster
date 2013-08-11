package master.flame.danmaku.danmaku.parser.android;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;

import master.flame.danmaku.danmaku.util.IOUtils;

/**
 * Created by Yrom on 13-8-11.
 */
public class JSONObjectSource extends AndroidFileSource{

    public JSONObjectSource(String filepath) {
        super(filepath);
    }

    public JSONObjectSource(Uri uri) {
        super(uri);
    }

    public JSONObjectSource(File file) {
        super(file);
    }

    public JSONObjectSource(InputStream stream) {
        super(stream);
    }

    public JSONObject getJSONObject(){
        String json = IOUtils.getString(inStream);
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            return null;
        }
    }

}
