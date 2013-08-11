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

package tv.light.danmaku.parser.android;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import tv.light.danmaku.model.BaseDanmaku;
import tv.light.danmaku.model.IDisplayer;
import tv.light.danmaku.model.android.Danmakus;
import tv.light.danmaku.parser.BaseDanmakuParser;
import tv.light.danmaku.parser.BiliDanmakuFactory;
import tv.light.danmaku.parser.IDataSource;

import java.io.IOException;

public class BiliDanmukuParse extends BaseDanmakuParser {

    static {
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
    }

    private final int mDispWidth;
    private final float mDispDensity;


    public BiliDanmukuParse(IDisplayer disp) {
        mDispWidth = disp.getWidth();
        mDispDensity = disp.getDensity();
    }

    @Override
    public Danmakus parse(IDataSource ds) {

        if (ds != null) {
            AndroidFileSource source = (AndroidFileSource) ds;
            try {
                XMLReader xmlReader = XMLReaderFactory.createXMLReader();
                XmlContentHandler contentHandler = new XmlContentHandler();
                xmlReader.setContentHandler(contentHandler);
                xmlReader.parse(new InputSource(source.inStream));
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
            tagName = tagName.toLowerCase().trim();
            if (tagName.equals("d")) {
                String pValue = attributes.getValue("p");
                // TODO: parse p value to danmaku
                String[] values = pValue.split(",");
                if (values.length > 0) {
                    long time = (long) (Float.parseFloat(values[0]) * 1000); // 出现时间
                    int type = Integer.parseInt(values[1]); // 弹幕类型
                    float textSize = Float.parseFloat(values[2]); // 字体大小
                    int color = Integer.parseInt(values[3]) | 0xFF000000; // 颜色
                    // int poolType = Integer.parseInt(values[5]); // 弹幕池类型（忽略
                    item = BiliDanmakuFactory.createDanmaku(type, mDispWidth);
                    if (item != null) {
                        item.time = time;
                        item.textSize = textSize * mDispDensity;
                        item.textColor = color;
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
                item.text = decodeXmlString(new String(ch, start, length));
                item.index = index++;
            }
        }

    }
}
