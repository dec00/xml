package de.cyberhoe.iptv;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

final class XtreamApi {
    static final class Item {
        final String id, name, icon, extension, epg;
        Item(String id, String name, String icon, String extension, String epg) {
            this.id=id; this.name=name; this.icon=icon; this.extension=extension; this.epg=epg;
        }
        @Override public String toString() { return name + (epg.isEmpty() ? "" : "\n" + epg); }
    }

    private final String server, user, pass;
    XtreamApi(String server, String user, String pass) {
        this.server = server.replaceAll("/+$", ""); this.user=user; this.pass=pass;
    }
    private String enc(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); }
        catch (Exception impossible) { return s; }
    }
    private String api(String action) {
        return server+"/player_api.php?username="+enc(user)+"&password="+enc(pass)+(action.isEmpty()?"":"&action="+action);
    }
    private String get(String target) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(target).openConnection();
        c.setConnectTimeout(12000); c.setReadTimeout(20000); c.setRequestProperty("User-Agent","CyberIPTV/0.1");
        int status=c.getResponseCode();
        BufferedReader r=new BufferedReader(new InputStreamReader(status<400?c.getInputStream():c.getErrorStream()));
        StringBuilder b=new StringBuilder(); String line; while((line=r.readLine())!=null)b.append(line);
        if(status>=400) throw new IllegalStateException("Serverfehler "+status); return b.toString();
    }
    void authenticate() throws Exception {
        JSONObject root=new JSONObject(get(api("")));
        JSONObject info=root.optJSONObject("user_info");
        if(info==null || !"Active".equalsIgnoreCase(info.optString("status"))) throw new IllegalArgumentException("Zugang nicht aktiv");
    }
    List<Item> live() throws Exception { return parse(get(api("get_live_streams")), "stream_id", "name", "stream_icon", "", "epg_channel_id"); }
    List<Item> movies() throws Exception { return parse(get(api("get_vod_streams")), "stream_id", "name", "stream_icon", "container_extension", ""); }
    List<Item> series() throws Exception { return parse(get(api("get_series")), "series_id", "name", "cover", "", "plot"); }
    private List<Item> parse(String json,String id,String name,String icon,String ext,String epg) throws Exception {
        JSONArray a=new JSONArray(json); List<Item> out=new ArrayList<>();
        for(int i=0;i<a.length();i++){ JSONObject o=a.getJSONObject(i); out.add(new Item(o.optString(id),o.optString(name,"Ohne Namen"),o.optString(icon),ext.isEmpty()?"":o.optString(ext,"mp4"),epg.isEmpty()?"":o.optString(epg))); }
        return out;
    }
    String liveUrl(Item i){ return server+"/live/"+enc(user)+"/"+enc(pass)+"/"+i.id+".m3u8"; }
    String movieUrl(Item i){ return server+"/movie/"+enc(user)+"/"+enc(pass)+"/"+i.id+"."+i.extension; }
    String seriesInfoUrl(Item i){ return api("get_series_info")+"&series_id="+enc(i.id); }
}
