package master.flame.danmaku.danmaku.parser.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.danmaku.util.IOUtils;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.text.TextUtils;

/**
 * a json file source
 * @author yrom
 */
public class JSONObjectSource implements IDataSource<JSONObject>{
	private JSONObject mJSONObject;
	private InputStream mInput;
	public JSONObjectSource(String json) throws JSONException{
		init(json);
	}
	
	public JSONObjectSource(InputStream in) throws JSONException{
		init(in);
	}
	
	private void init(InputStream in) throws JSONException {
		if(in == null)
			throw new NullPointerException("input stream cannot be null!");
		mInput = in;
		String json = IOUtils.getString(mInput);
		init(json);
	}
	
	public JSONObjectSource(URL url) throws JSONException, IOException{
		this(url.openStream());
	}
	
	public JSONObjectSource(File file) throws FileNotFoundException, JSONException{
		init(new FileInputStream(file));
	}
	
	public JSONObjectSource(Uri uri) throws IOException, JSONException {
		String scheme = uri.getScheme();
        if (SCHEME_HTTP_TAG.equalsIgnoreCase(scheme) || SCHEME_HTTPS_TAG.equalsIgnoreCase(scheme)) {
            init(new URL(uri.getPath()).openStream());
        } else if (SCHEME_FILE_TAG.equalsIgnoreCase(scheme)) {
            init(new FileInputStream(uri.getPath()));
        }
	}
	
	private void init(String json) throws JSONException {
		if(!TextUtils.isEmpty(json)){
			mJSONObject = new JSONObject(json);
		}
	}
    public JSONObject data(){
    	return mJSONObject;
    }

	@Override
	public void release() {
		IOUtils.closeQuietly(mInput);
		mInput = null;
	}

}
