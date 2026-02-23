package com.github.tvbox.osc.util.live;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TxtSubscribe {

    private static class ChannelInfo {
        private ArrayList<String> urls;
        private String logoUrl;

        public ChannelInfo() {
            this.urls = new ArrayList<>();
            this.logoUrl = null;
        }

        public ArrayList<String> getUrls() {
            return this.urls;
        }

        public void setUrls(ArrayList<String> urls) {
            this.urls = urls;
        }

        public String getLogoUrl() {
            return this.logoUrl;
        }

        public void setLogoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
        }
    }

    private static final Pattern NAME_PATTERN = Pattern.compile(".*,(.+?)$");
    private static final Pattern GROUP_PATTERN = Pattern.compile("group-title=\"(.*?)\"");
    private static final Pattern LOGO_PATTERN = Pattern.compile("tvg-logo=\"(.*?)\"");

    public static JsonArray parse(String str) {
        LinkedHashMap<String, LinkedHashMap<String, ChannelInfo>> linkedHashMap = new LinkedHashMap<>();;
        if (str.startsWith("#EXTM3U")) {
            parseM3u(linkedHashMap, str);
        } else {
            parseTxt(linkedHashMap, str);
        }
        return live2JsonArray(linkedHashMap);
    }

    private static void parseM3u(LinkedHashMap<String, LinkedHashMap<String, ChannelInfo>> linkedHashMap, String str) {
        ChannelInfo channelInfo;
        try {
            BufferedReader bufferedReader = new BufferedReader(new StringReader(str));
            LinkedHashMap<String, ChannelInfo> channel = new LinkedHashMap<>();
            LinkedHashMap<String, ChannelInfo> channelTemp = channel;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.equals("")) continue;
                if (line.startsWith("#EXTM3U")) continue;
                if (line.startsWith("#EXTINF")) {
                    String name = getStrByRegex(NAME_PATTERN, line);
                    String group = getStrByRegex(GROUP_PATTERN, line);
                    String logoUrl = getStrByRegex(LOGO_PATTERN, line, null);
                    // 此时再读取一行，就是对应的 url 链接了
                    String url = bufferedReader.readLine().trim();
                    if (linkedHashMap.containsKey(group)) {
                        channelTemp = linkedHashMap.get(group);
                    } else {
                        channelTemp = new LinkedHashMap<>();
                        linkedHashMap.put(group, channelTemp);
                    }
                    if (null != channelTemp && channelTemp.containsKey(name)) {
                        channelInfo = channelTemp.get(name);
                    } else {
                        channelInfo = new ChannelInfo();
                        channelTemp.put(name, channelInfo);
                    }
                    if (null != channelInfo) {
                        ArrayList<String> urls = channelInfo.getUrls();
                        if (!urls.contains(url)) {
                            urls.add(url);
                        }
                        if (null != logoUrl && !logoUrl.isEmpty()) {
                            channelInfo.setLogoUrl(logoUrl);
                        }
                    }
                }
            }
            bufferedReader.close();
            if (channel.isEmpty()) return;
            linkedHashMap.put("未分组", channel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getStrByRegex(Pattern pattern, String line) {
        String defaultStr = pattern.pattern().equals(GROUP_PATTERN.pattern()) ? "未分组" : "未命名";
        return getStrByRegex(pattern, line, defaultStr);
    }

    private static String getStrByRegex(Pattern pattern, String line, String defaultStr) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) return matcher.group(1);
        return defaultStr;
    }

    private static void parseTxt(LinkedHashMap<String, LinkedHashMap<String, ChannelInfo>> linkedHashMap, String str) {
        ArrayList<String> arrayList;
        ChannelInfo channelInfo;
        try {
            BufferedReader bufferedReader = new BufferedReader(new StringReader(str));
            String readLine = bufferedReader.readLine();
            LinkedHashMap<String, ChannelInfo> linkedHashMap2 = new LinkedHashMap<>();
            LinkedHashMap<String, ChannelInfo> linkedHashMap3 = linkedHashMap2;
            while (readLine != null) {
                if (readLine.trim().isEmpty()) {
                    readLine = bufferedReader.readLine();
                } else {
                    String[] split = readLine.split(",", 2);
                    if (split.length < 2) {
                        readLine = bufferedReader.readLine();
                    } else {
                        if (readLine.contains("#genre#")) {
                            String group = split[0].trim();
                            if (!linkedHashMap.containsKey(group)) {
                                linkedHashMap3 = new LinkedHashMap<>();
                                linkedHashMap.put(group, linkedHashMap3);
                            } else {
                                linkedHashMap3 = linkedHashMap.get(group);
                            }
                        } else {
                            String channelName = split[0].trim();
                            for (String str2 : split[1].trim().split("#")) {
                                String trim3 = str2.trim();
                                if (!trim3.isEmpty() && (trim3.startsWith("http") || trim3.startsWith("rtsp") || trim3.startsWith("rtmp"))) {
                                    if (!linkedHashMap3.containsKey(channelName)) {
                                        channelInfo = new ChannelInfo();
                                        linkedHashMap3.put(channelName, channelInfo);
                                    } else {
                                        channelInfo = linkedHashMap3.get(channelName);
                                    }
                                    if (null != channelInfo) {
                                        ArrayList<String> urls = channelInfo.getUrls();
                                        if (null != urls && !urls.contains(trim3)) {
                                            urls.add(trim3);
                                        }
                                    }
                                }
                            }
                        }
                        readLine = bufferedReader.readLine();
                    }
                }
            }
            bufferedReader.close();
            if (linkedHashMap2.isEmpty()) {
                return;
            }
            linkedHashMap.put("未分组", linkedHashMap2);
        } catch (Throwable unused) {
        }
    }

    private static JsonArray live2JsonArray(LinkedHashMap<String, LinkedHashMap<String, ChannelInfo>> linkedHashMap) {
        JsonArray jsonarr = new JsonArray();
        for (String str : linkedHashMap.keySet()) {
            JsonArray jsonarr2 = new JsonArray();
            LinkedHashMap<String, ChannelInfo> linkedHashMap2 = linkedHashMap.get(str);
            if (!linkedHashMap2.isEmpty()) {
                for (String str2 : linkedHashMap2.keySet()) {
                    ChannelInfo channelInfo = linkedHashMap2.get(str2);
                    ArrayList<String> urls = channelInfo.getUrls();
                    if (!urls.isEmpty()) {
                        JsonArray jsonarr3 = new JsonArray();
                        for (int i = 0; i < urls.size(); i++) {
                            jsonarr3.add(urls.get(i));
                        }
                        JsonObject jsonobj = new JsonObject();
                        try {
                            jsonobj.addProperty("name", str2);
                            jsonobj.add("urls", jsonarr3);
                            String logoUrl = channelInfo.getLogoUrl();
                            if (null != logoUrl && !logoUrl.isBlank()) {
                                jsonobj.addProperty("logo", logoUrl);
                            }
                        } catch (Throwable e) {
                        }
                        jsonarr2.add(jsonobj);
                    }
                }
                JsonObject jsonobj2 = new JsonObject();
                try {
                    jsonobj2.addProperty("group", str);
                    jsonobj2.add("channels", jsonarr2);
                } catch (Throwable e) {
                }
                jsonarr.add(jsonobj2);
            }
        }
        return jsonarr;
    }
}
