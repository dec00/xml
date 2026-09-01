package de.cyberhoe.iptv;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

final class XtreamApi {
    static final class Item {
        final String id, name, icon, extension, epg;
        Item(String id, String name, String icon, String extension, String epg) {
            this.id=id; this.name=name; this.icon=icon; this.extension=extension; this.epg=epg;
        }
        @Override public String toString() { return name + (epg.isEmpty() ? "" : "\n" + epg); }
    }

    private final List<String> servers;
    private volatile String server;
    private final String user, pass;
    XtreamApi(String server, String user, String pass) {
        this(Collections.singletonList(server),server,user,pass);
    }
    XtreamApi(List<String> candidates,String preferred,String user,String pass) {
        Set<String> clean=new LinkedHashSet<>();
        if(preferred!=null&&!preferred.trim().isEmpty())clean.add(normalize(preferred));
        for(String s:candidates)if(s!=null&&!s.trim().isEmpty())clean.add(normalize(s));
        this.servers=new ArrayList<>(clean); this.server=this.servers.get(0); this.user=user; this.pass=pass;
    }
    private static String normalize(String value){return value.trim().replaceAll("/+$","");}
    static XtreamApi connectFast(List<String> candidates,String preferred,String user,String pass) throws Exception {
        Set<String> unique=new LinkedHashSet<>();
        if(preferred!=null&&!preferred.trim().isEmpty())unique.add(normalize(preferred));
        for(String s:candidates)if(s!=null&&!s.trim().isEmpty())unique.add(normalize(s));
        if(unique.isEmpty())throw new IllegalArgumentException("Keine Server-Adresse vorhanden");
        List<String> all=new ArrayList<>(unique);
        ExecutorService pool=Executors.newFixedThreadPool(Math.min(4,all.size()));
        CompletionService<String> done=new ExecutorCompletionService<>(pool);
        for(String candidate:all)done.submit(()->{authenticateAt(candidate,user,pass);return candidate;});
        Exception last=null; long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        try{
            for(int remaining=all.size();remaining>0;remaining--){
                long wait=deadline-System.nanoTime(); if(wait<=0)break;
                Future<String> result=done.poll(wait,TimeUnit.NANOSECONDS); if(result==null)break;
                try{return new XtreamApi(all,result.get(),user,pass);}catch(Exception e){last=e;}
            }
        }finally{pool.shutdownNow();}
        throw new IllegalStateException("Keiner der hinterlegten Server akzeptiert den Zugang. Bitte die aktuelle Xtream-Server-URL vom Anbieter eintragen.");
    }
    private static void authenticateAt(String server,String user,String pass) throws Exception {
        String target=normalize(server)+"/player_api.php?username="+encode(user)+"&password="+encode(pass);
        String body=getUrl(target,3500,6500).trim();
        if(!body.startsWith("{")){
            if(body.startsWith("<")||body.toLowerCase().contains("<html"))throw new IllegalArgumentException("Server liefert nur eine Webseite statt der IPTV-API");
            throw new IllegalArgumentException("Server liefert keine gültige IPTV-API");
        }
        JSONObject root=new JSONObject(body);
        JSONObject info=root.optJSONObject("user_info");
        if(info==null||!"Active".equalsIgnoreCase(info.optString("status")))throw new IllegalArgumentException("Zugang nicht aktiv");
    }
    private static String useful(Exception e){
        Throwable t=e; while(t.getCause()!=null)t=t.getCause();
        String m=t.getMessage(); return m==null||m.trim().isEmpty()?t.getClass().getSimpleName():m;
    }
    private static String encode(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); }
        catch (Exception impossible) { return s; }
    }
    private String enc(String s){return encode(s);}
    private String api(String host,String action) {
        return host+"/player_api.php?username="+enc(user)+"&password="+enc(pass)+(action.isEmpty()?"":"&action="+action);
    }
    private static String getUrl(String target,int connectTimeout,int readTimeout) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(target).openConnection();
        c.setConnectTimeout(connectTimeout); c.setReadTimeout(readTimeout); c.setUseCaches(false); c.setRequestProperty("Connection","keep-alive"); c.setRequestProperty("User-Agent","CyberIPTV/0.2");
        try{
            int status=c.getResponseCode();
            if(status>=400)throw new IllegalStateException("Serverfehler "+status);
            try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()))){StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null)b.append(line);return b.toString();}
        }finally{c.disconnect();}
    }
    private List<String> orderedServers(){
        List<String> out=new ArrayList<>(); out.add(server); for(String s:servers)if(!s.equals(server))out.add(s); return out;
    }
    private String request(String action,String expectedStart) throws Exception {
        Exception last=null;
        for(String candidate:orderedServers()){
            try{
                String body=getUrl(api(candidate,action),4000,15000).trim();
                if(!body.startsWith(expectedStart))throw new IllegalStateException("Ungültige Serverantwort");
                server=candidate; return body;
            }catch(Exception e){last=e;}
        }
        throw new IllegalStateException(last==null?"Kein Server vorhanden":useful(last));
    }
    void authenticate() throws Exception {
        JSONObject root=new JSONObject(request("","{"));
        JSONObject info=root.optJSONObject("user_info");
        if(info==null || !"Active".equalsIgnoreCase(info.optString("status"))) throw new IllegalArgumentException("Zugang nicht aktiv");
    }
    List<Item> live() throws Exception { return parse(request("get_live_streams","["), "stream_id", "name", "stream_icon", "", "epg_channel_id"); }
    List<Item> movies() throws Exception { return parse(request("get_vod_streams","["), "stream_id", "name", "stream_icon", "container_extension", ""); }
    List<Item> series() throws Exception { return parse(request("get_series","["), "series_id", "name", "cover", "", "plot"); }
    private List<Item> parse(String json,String id,String name,String icon,String ext,String epg) throws Exception {
        JSONArray a=new JSONArray(json); List<Item> out=new ArrayList<>();
        for(int i=0;i<a.length();i++){ JSONObject o=a.getJSONObject(i); out.add(new Item(o.optString(id),o.optString(name,"Ohne Namen"),o.optString(icon),ext.isEmpty()?"":o.optString(ext,"mp4"),epg.isEmpty()?"":o.optString(epg))); }
        return out;
    }
    String currentServer(){return server;}
    List<String> liveUrls(Item i){List<String> out=new ArrayList<>();for(String s:orderedServers())out.add(s+"/live/"+enc(user)+"/"+enc(pass)+"/"+i.id+".m3u8");return out;}
    List<String> movieUrls(Item i){List<String> out=new ArrayList<>();for(String s:orderedServers())out.add(s+"/movie/"+enc(user)+"/"+enc(pass)+"/"+i.id+"."+i.extension);return out;}
    String liveUrl(Item i){return liveUrls(i).get(0);}
    String movieUrl(Item i){return movieUrls(i).get(0);}
    String seriesInfoUrl(Item i){ return api(server,"get_series_info")+"&series_id="+enc(i.id); }
}
