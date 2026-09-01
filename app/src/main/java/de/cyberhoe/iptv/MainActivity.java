package de.cyberhoe.iptv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private static final String[] SERVERS = {
        "Automatisch – schnellster Server",
        "http://cf.business-cdn.me",
        "http://cf.hi-ott.me",
        "http://cf.hi-cdn.me",
        "http://cf.hi-max.me",
        "http://cf.cdn-959.me",
        "http://cf.its-cdn.me",
        "http://cf.cdn-akoy2.me",
        "http://cf.cdn-amz2.me",
        "Eigene Server-URL"
    };
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private SharedPreferences prefs; private XtreamApi api; private List<XtreamApi.Item> items=new ArrayList<>();
    private ArrayAdapter<XtreamApi.Item> adapter; private ListView list; private EditText search; private String section="live";
    private int purple=Color.rgb(181,124,255), panel=Color.rgb(22,24,34);

    private String[] privateConfig(){
        try(InputStream in=getAssets().open("private_config.json")){
            byte[] raw=new byte[in.available()]; int read=in.read(raw);
            JSONObject o=new JSONObject(new String(raw,0,Math.max(0,read),StandardCharsets.UTF_8));
            return new String[]{o.optString("server",SERVERS[1]),o.optString("username",""),o.optString("password","")};
        }catch(Exception ignored){return new String[]{SERVERS[1],"",""};}
    }

    @Override protected void onCreate(Bundle b){ super.onCreate(b); prefs=getSharedPreferences("private",MODE_PRIVATE); showLogin(); }
    private TextView title(String s,int size){ TextView v=new TextView(this); v.setText(s); v.setTextColor(Color.WHITE); v.setTextSize(size); v.setPadding(20,18,20,18); return v; }
    private Button button(String s){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setBackgroundColor(panel); b.setFocusable(true); return b; }
    private EditText field(String hint,boolean secret){ EditText e=new EditText(this); e.setHint(hint); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.GRAY); e.setSingleLine(); e.setPadding(22,16,22,16); if(secret)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); return e; }
    private void showLogin(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(50,30,50,30); root.setBackgroundColor(Color.rgb(9,10,16));
        TextView logo=title("CYBER IPTV",32); logo.setTextColor(purple); root.addView(logo);
        Spinner serverChoice=new Spinner(this); ArrayAdapter<String> choices=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,SERVERS); serverChoice.setAdapter(choices); serverChoice.setFocusable(true);
        EditText server=field("Server-URL, z. B. http://anbieter:8080",false), user=field("Benutzername",false), pass=field("Passwort",true);
        String[] config=privateConfig();
        server.setText(prefs.getString("server",config[0]));
        user.setText(prefs.getString("user",config[1]));
        pass.setText(prefs.getString("pass",config[2]));
        serverChoice.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){if(pos>0&&pos<SERVERS.length-1)server.setText(SERVERS[pos]);}});
        root.addView(serverChoice,new LinearLayout.LayoutParams(-1,-2)); root.addView(server,new LinearLayout.LayoutParams(-1,-2)); root.addView(user,new LinearLayout.LayoutParams(-1,-2)); root.addView(pass,new LinearLayout.LayoutParams(-1,-2));
        Button login=button("ANMELDEN"); root.addView(login,new LinearLayout.LayoutParams(-1,-2)); TextView status=title("",16); root.addView(status);
        login.setOnClickListener(v->{ String s=server.getText().toString().trim(),u=user.getText().toString().trim(),p=pass.getText().toString(); if(s.isEmpty()||u.isEmpty()||p.isEmpty()){status.setText("Bitte alle Felder ausfüllen.");return;} status.setText("Schnellster verfügbarer Server wird gesucht …"); login.setEnabled(false); io.execute(()->{try{api=XtreamApi.connectFast(serverCandidates(s),prefs.getString("working_server",s),u,p);prefs.edit().putString("server",s).putString("working_server",api.currentServer()).putString("user",u).putString("pass",p).apply();runOnUiThread(this::showHome);}catch(Exception e){runOnUiThread(()->{login.setEnabled(true);status.setText("Anmeldung fehlgeschlagen: "+e.getMessage());});}}); });
        setContentView(root);
        if(!user.getText().toString().isEmpty()&&!pass.getText().toString().isEmpty()) login.post(login::performClick);
    }
    private List<String> serverCandidates(String selected){
        LinkedHashSet<String> out=new LinkedHashSet<>();
        addServerVariants(out,selected);
        addServerVariants(out,prefs.getString("working_server",""));
        String[] config=privateConfig(); addServerVariants(out,config[0]);
        for(int i=1;i<SERVERS.length-1;i++)addServerVariants(out,SERVERS[i]);
        return new ArrayList<>(out);
    }
    private void addServerVariants(Set<String> out,String value){
        if(value==null||value.trim().isEmpty())return;
        String clean=value.trim().replaceAll("/+$",""); out.add(clean);
        if(clean.startsWith("http://"))out.add("https://"+clean.substring(7));
        else if(clean.startsWith("https://"))out.add("http://"+clean.substring(8));
    }
    private void showHome(){
        String s=prefs.getString("server",""); api=new XtreamApi(serverCandidates(s),prefs.getString("working_server",s),prefs.getString("user",""),prefs.getString("pass",""));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(9,10,16));
        LinearLayout nav=new LinearLayout(this); String[] labels={"LIVE-TV","FILME","SERIEN","FAVORITEN","ABMELDEN"};
        for(String label:labels){Button b=button(label);nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->{if(label.equals("ABMELDEN")){prefs.edit().clear().apply();showLogin();}else load(label);});}
        root.addView(nav); search=field("Sender, Film oder Serie suchen …",false); root.addView(search,new LinearLayout.LayoutParams(-1,-2));
        list=new ListView(this); list.setBackgroundColor(Color.rgb(9,10,16)); list.setDividerHeight(2); adapter=new ArrayAdapter<XtreamApi.Item>(this,android.R.layout.simple_list_item_1,new ArrayList<>()){
            @Override public View getView(int pos,View convert,android.view.ViewGroup parent){TextView v=(TextView)super.getView(pos,convert,parent);v.setTextColor(Color.WHITE);v.setTextSize(19);v.setPadding(25,18,25,18);v.setBackgroundColor(panel);return v;}};
        list.setAdapter(adapter); root.addView(list,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence x,int a,int c,int d){}public void onTextChanged(CharSequence x,int a,int b,int c){filter(x.toString());}public void afterTextChanged(android.text.Editable e){}});
        list.setOnItemClickListener((p,v,pos,id)->open(adapter.getItem(pos)));
        list.setOnItemLongClickListener((p,v,pos,id)->{toggleFavorite(adapter.getItem(pos));return true;}); load("LIVE-TV");
    }
    private void load(String label){ section=label.equals("FILME")?"movie":label.equals("SERIEN")?"series":label.equals("FAVORITEN")?"favorites":"live"; adapter.clear(); adapter.add(new XtreamApi.Item("","Wird geladen …","","",""));
        io.execute(()->{try{List<XtreamApi.Item> got=section.equals("movie")?api.movies():section.equals("series")?api.series():api.live();items=got;prefs.edit().putString("working_server",api.currentServer()).apply();if(section.equals("favorites")){Set<String> fav=prefs.getStringSet("favorites",new HashSet<>());List<XtreamApi.Item> f=new ArrayList<>();for(XtreamApi.Item i:got)if(fav.contains(i.id))f.add(i);items=f;}runOnUiThread(()->filter(search.getText().toString()));}catch(Exception e){runOnUiThread(()->{adapter.clear();adapter.add(new XtreamApi.Item("","Fehler: "+e.getMessage(),"","",""));});}});
    }
    private void filter(String q){adapter.clear();String n=q.toLowerCase();for(XtreamApi.Item i:items)if(i.name.toLowerCase().contains(n))adapter.add(i);}
    private void open(XtreamApi.Item i){ if(i==null||i.id.isEmpty())return; if(section.equals("series")){Toast.makeText(this,"Serienepisoden folgen in Version 0.2",Toast.LENGTH_LONG).show();return;} boolean live=section.equals("live")||section.equals("favorites"); Intent x=new Intent(this,PlayerActivity.class);x.putStringArrayListExtra("urls",new ArrayList<>(live?api.liveUrls(i):api.movieUrls(i)));x.putExtra("is_live",live);x.putExtra("title",i.name);startActivity(x);}
    private void toggleFavorite(XtreamApi.Item i){Set<String> f=new HashSet<>(prefs.getStringSet("favorites",new HashSet<>()));boolean added=f.add(i.id);if(!added)f.remove(i.id);prefs.edit().putStringSet("favorites",f).apply();Toast.makeText(this,added?"Zu Favoriten hinzugefügt":"Aus Favoriten entfernt",Toast.LENGTH_SHORT).show();}
    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
}
