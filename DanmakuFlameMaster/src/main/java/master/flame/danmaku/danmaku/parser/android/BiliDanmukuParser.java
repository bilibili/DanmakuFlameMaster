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

import android.graphics.Color;
import master.flame.danmaku.danmaku.model.AlphaValue;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.Duration;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.util.Locale;

public class BiliDanmukuParser extends BaseDanmakuParser {

    static {
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
    }

    @Override
    public Danmakus parse() {

        if (mDataSource != null) {
            AndroidFileSource source = (AndroidFileSource) mDataSource;
            try {
                XMLReader xmlReader = XMLReaderFactory.createXMLReader();
                XmlContentHandler contentHandler = new XmlContentHandler();
                xmlReader.setContentHandler(contentHandler);
                xmlReader.parse(new InputSource(source.data()));
                return contentHandler.getResult();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return null;
    }

    public class XmlContentHandler extends DefaultHandler {

        public Danmakus result = null;

        public BaseDanmaku item = null;

        public boolean completed = false;

        public int index = 0;

        public Danmakus getResult() {
            return result;
        }

        @Override
        public void startDocument() throws SAXException {
            result = new Danmakus();
        }

        @Override
        public void endDocument() throws SAXException {
            completed = true;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            String tagName = localName.length() != 0 ? localName : qName;
            tagName = tagName.toLowerCase(Locale.getDefault()).trim();
            if (tagName.equals("d")) {
                String pValue = attributes.getValue("p");
                // parse p value to danmaku
                String[] values = pValue.split(",");
                if (values.length > 0) {
                    long time = (long) (Float.parseFloat(values[0]) * 1000); // 出现时间
                    int type = Integer.parseInt(values[1]); // 弹幕类型
                    float textSize = Float.parseFloat(values[2]); // 字体大小
                    int color = Integer.parseInt(values[3]) | 0xFF000000; // 颜色
                    // int poolType = Integer.parseInt(values[5]); // 弹幕池类型（忽略
                    item = DanmakuFactory.createDanmaku(type, mDispWidth / (mDispDensity - 0.6f));
                    if (item != null) {
                        item.time = time;
                        item.textSize = textSize * (mDispDensity - 0.6f);
                        item.textColor = color;
                        item.textShadowColor = color <= Color.BLACK ? Color.WHITE : Color.BLACK;
                    }
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (item != null) {
                String tagName = localName.length() != 0 ? localName : qName;
                if (tagName.equalsIgnoreCase("d")) {
                    item.setTimer(mTimer);
                    result.addItem(item);
                }
                item = null;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (item != null) {
                DanmakuFactory.fillText(item, decodeXmlString(new String(ch, start, length)));
                item.index = index++;

                // initial specail danmaku data
                String text = ((String)item.text).trim();
                if (item.getType() == BaseDanmaku.TYPE_SPECIAL && text.startsWith("[")
                        && text.endsWith("]")) {
                    text = text.substring(2, text.length() - 2);
                    String[] textArr = text.split("\",\"");
                    if (textArr == null || textArr.length < 5)
                        return;
                    item.text = textArr[4];
                    float beginX = Float.parseFloat(textArr[0]);
                    float beginY = Float.parseFloat(textArr[1]);
                    float endX = beginX;
                    float endY = beginY;
                    String[] alphaArr = textArr[2].split("-");
                    int beginAlpha = (int) (AlphaValue.MAX * Float.parseFloat(alphaArr[0]));
                    int endAlpha = beginAlpha;
                    if (alphaArr.length > 1) {
                        endAlpha = (int) (AlphaValue.MAX * Float.parseFloat(alphaArr[1]));
                    }
                    long alphaDuraion = (long) (Float.parseFloat(textArr[3]) * 1000);
                    long translationDuration = alphaDuraion;
                    long translationStartDelay = 0;
                    float rotateY = 0, rotateZ = 0;
                    if (textArr.length > 5) {
                        rotateZ = Float.parseFloat(textArr[5]);
                        rotateY = Float.parseFloat(textArr[6]);
                    }
                    if (textArr.length > 7) {
                        endX = Float.parseFloat(textArr[7]);
                        endY = Float.parseFloat(textArr[8]);
                        translationDuration = Integer.parseInt(textArr[9]);
                        translationStartDelay = (long) (Float.parseFloat(textArr[10]));
                    }
                    item.duration = new Duration(alphaDuraion);
                    item.rotationZ = rotateZ;
                    item.rotationY = rotateY;
                    DanmakuFactory.fillTranslationData(item, mDispWidth, mDispHeight, beginX,
                            beginY, endX, endY, translationDuration, translationStartDelay);
                    DanmakuFactory.fillAlphaData(item, beginAlpha, endAlpha, alphaDuraion);
                }

            }
        }

        private String decodeXmlString(String title) {
            if (title.indexOf("&amp;") > -1) {
                title = title.replace("&amp;", "&");
            }
            if (title.indexOf("&quot;") > -1) {
                title = title.replace("&quot;", "\"");
            }
            if (title.contains("&gt;")) {
                title = title.replace("&gt;", ">");
            }
            if (title.contains("&lt;")) {
                title = title.replace("&lt;", "<");
            }
            return title;
        }

    }
}
