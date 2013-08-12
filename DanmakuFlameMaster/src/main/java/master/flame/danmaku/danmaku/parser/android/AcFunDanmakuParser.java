package master.flame.danmaku.danmaku.parser.android;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.BiliDanmakuFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.BitmapFactory;

public class AcFunDanmakuParser extends BaseDanmakuParser {

	public AcFunDanmakuParser(IDisplayer disp) {
		super(disp);
	}

	@Override
	public Danmakus parse() {
		if(mDataSource!= null && mDataSource instanceof JSONSource){
			JSONSource jsonSource = (JSONSource) mDataSource;
			return _parse(jsonSource.data());
		}
		return null;
	}
	
	private Danmakus _parse(JSONArray jsonArray){
		Danmakus danmakus = null;
		if(jsonArray != null && jsonArray.length()>0)
			danmakus = new Danmakus();
		for(int i =0 ; i<jsonArray.length();i++){
			try {
				JSONObject obj =  jsonArray.getJSONObject(i);
				String c = obj.getString("c");
				String m = obj.getString("m");
				String[] values = c.split(",");
				if (values.length > 0) {
                    long time = (long) (Float.parseFloat(values[0]) * 1000); // 出现时间
                    int color = Integer.parseInt(values[1]) | 0xFF000000; // 颜色
                    float textSize = Float.parseFloat(values[3]); // 字体大小
                    int type = Integer.parseInt(values[2]); // 弹幕类型
                    BaseDanmaku item = BiliDanmakuFactory.createDanmaku(type, mDispWidth);
                    if (item != null) {
                        item.time = time;
                        item.textSize = textSize*(mScaledDensity - 0.5f);
                        item.textColor = color;
                        item.text = m ;
                        item.index = i;
                        item.setTimer(mTimer);
                        danmakus.addItem(item);
                    }
                }
				
			} catch (JSONException e) {
			}
		}
		
		return danmakus;
	}
}
