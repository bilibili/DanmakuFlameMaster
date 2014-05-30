
package master.flame.danmaku.danmaku.parser.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;

public class AcFunDanmakuParser extends BaseDanmakuParser {

    @Override
    public Danmakus parse() {
        if (mDataSource != null && mDataSource instanceof JSONSource) {
            JSONSource jsonSource = (JSONSource) mDataSource;
            return _parse(jsonSource.data());
        }
        return new Danmakus();
    }

    private Danmakus _parse(JSONArray jsonArray) {
        Danmakus danmakus = new Danmakus();
        if (jsonArray == null || jsonArray.length() == 0) {
            return danmakus;
        }
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject obj = jsonArray.getJSONObject(i);
                String c = obj.getString("c");
                String[] values = c.split(",");
                if (values.length > 0) {
                    int type = Integer.parseInt(values[2]); // 弹幕类型
                    if (type == 7)
                        // FIXME : hard code
                        // TODO : parse advance danmaku json
                        continue;
                    long time = (long) (Float.parseFloat(values[0]) * 1000); // 出现时间
                    int color = Integer.parseInt(values[1]) | 0xFF000000; // 颜色
                    float textSize = Float.parseFloat(values[3]); // 字体大小
                    BaseDanmaku item = DanmakuFactory.createDanmaku(type, mDispWidth / (mDispDensity - 0.6f));
                    if (item != null) {
                        item.time = time;
                        item.textSize = textSize * (mDispDensity - 0.6f);
                        item.textColor = color;
                        item.textShadowColor = color <= Color.BLACK ? Color.WHITE : Color.BLACK;
                        DanmakuFactory.fillText(item, obj.optString("m", "...."));
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
