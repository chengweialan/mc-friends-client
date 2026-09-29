import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.*;
import java.util.zip.*;

/** Shared Windows/macOS selector and transactional mod synchronizer. Java 17+. */
public class FriendsUpdater {
    static final String CHANNEL="https://raw.githubusercontent.com/chengweialan/mc-friends-client/main/channel.json";
    static final String PACK_PATTERN="https://raw\\.githubusercontent\\.com/chengweialan/mc-friends-client/[a-f0-9]{40}/pack/pack\\.toml";
    static Consumer<String> status=System.out::println;
    static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(25)).followRedirects(HttpClient.Redirect.NORMAL).build();
    static Map<String,Object> obj(Object x){return (Map<String,Object>)x;}
    static List<Object> list(Object x){return (List<Object>)x;}
    static String str(Object x){return Objects.toString(x,"");}
    static Map<String,Object> read(Path p)throws Exception{return obj(Json.parse(Files.readString(p,StandardCharsets.UTF_8)));}
    static void write(Path p,Object o)throws Exception{atomic(p,Json.stringify(o).getBytes(StandardCharsets.UTF_8));}
    static void atomic(Path p,byte[] b)throws Exception{
        Files.createDirectories(p.getParent());Path tmp=Files.createTempFile(p.getParent(),".friends-",".tmp");
        try{Files.write(tmp,b);try{Files.move(tmp,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(tmp);}
    }
    static String hash(Path p)throws Exception{
        MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=Files.newInputStream(p)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}return HexFormat.of().formatHex(md.digest());
    }
    static byte[] fetch(String url)throws Exception{
        HttpRequest req=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(3)).header("User-Agent","FriendsMC/0.4.0").GET().build();
        HttpResponse<byte[]> r=HTTP.send(req,HttpResponse.BodyHandlers.ofByteArray());if(r.statusCode()!=200)throw new IOException("HTTP "+r.statusCode()+": "+url);return r.body();
    }
    static Map<String,Object> channel()throws Exception{
        Map<String,Object> c=obj(Json.parse(new String(fetch(CHANNEL+"?t="+System.currentTimeMillis()),StandardCharsets.UTF_8)));
        if(((Number)c.get("schema")).intValue()!=2||((Number)c.get("java")).intValue()!=25)throw new IOException("请下载新的客户端压缩包 / Client update required");
        if(!str(c.get("packUrl")).matches(PACK_PATTERN))throw new IOException("清单地址不是固定发布版本");
        for(String k:List.of("minecraft","neoforge"))if(!str(c.get(k)).matches("[A-Za-z0-9][A-Za-z0-9._-]{0,79}"))throw new IOException("Invalid version");
        return c;
    }
    static Map<String,Object> catalog(String packUrl,String expected)throws Exception{
        if(!packUrl.matches(PACK_PATTERN))throw new IOException("Invalid immutable pack URL");
        byte[] b=fetch(packUrl.replace("pack.toml","mods.lock.json"));
        String actual=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
        if(!actual.equals(expected))throw new IOException("模组清单校验失败");
        Map<String,Object> c=obj(Json.parse(new String(b,StandardCharsets.UTF_8)));validate(c);return c;
    }
    static void validate(Map<String,Object> c)throws Exception{
        Set<String> ids=new HashSet<>(),names=new HashSet<>();
        for(Object o:list(c.get("mods"))){Map<String,Object> m=obj(o);String n=str(m.get("filename"));
            if(!n.matches("[A-Za-z0-9_.+ -]+\\.jar")||!names.add(n)||!ids.add(str(m.get("id"))))throw new IOException("Invalid/duplicate mod entry");
            if(!str(m.get("sha256")).matches("[a-f0-9]{64}")||!str(m.get("url")).startsWith("https://cdn.modrinth.com/"))throw new IOException("Invalid download metadata");
        }
        for(Object o:list(c.get("mods")))for(Object dep:list(obj(o).get("dependencies")))if(!ids.contains(str(dep)))throw new IOException("Missing dependency "+dep);
    }
    static Map<String,Object> choices(Path root,Map<String,Object> catalog,boolean ui)throws Exception{
        Path file=root.resolve("state/mod-options.json");Map<String,Object> saved=Files.exists(file)?read(file):new LinkedHashMap<>();
        if(ui){final boolean[] accepted={false};SwingUtilities.invokeAndWait(()->{
            JPanel rows=new JPanel();rows.setLayout(new BoxLayout(rows,BoxLayout.Y_AXIS));rows.setBorder(new EmptyBorder(14,18,14,18));
            Map<String,JCheckBox> boxes=new LinkedHashMap<>();
            for(Object o:list(catalog.get("mods"))){Map<String,Object> m=obj(o);if(Boolean.TRUE.equals(m.get("hidden")))continue;
                String id=str(m.get("id"));boolean required=Boolean.TRUE.equals(m.get("required"));
                JCheckBox b=new JCheckBox((required?"[必装] ":"[可选] ")+m.get("name"),required||Boolean.TRUE.equals(saved.getOrDefault(id,m.get("default"))));
                b.setEnabled(!required);b.setFont(new Font("Dialog",Font.BOLD,15));rows.add(b);
                JLabel desc=new JLabel("    "+m.get("description"));desc.setBorder(new EmptyBorder(0,0,10,0));rows.add(desc);boxes.put(id,b);
            }
            JScrollPane scroll=new JScrollPane(rows);scroll.setPreferredSize(new Dimension(670,490));
            JPanel panel=new JPanel(new BorderLayout(0,12));panel.add(new JLabel("必装模组保持全员一致；可选项会记住。未勾选的旧版本会备份移出。"),BorderLayout.NORTH);panel.add(scroll);
            int answer=JOptionPane.showConfirmDialog(null,panel,"Friends MC · 选择模组 / Mod options",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if(answer==JOptionPane.OK_OPTION){boxes.forEach((id,b)->saved.put(id,b.isSelected()));accepted[0]=true;}
        });if(!accepted[0])throw new IOException("已取消模组选择，游戏未启动。");}
        return saved;
    }
    static List<Map<String,Object>> selected(Map<String,Object> catalog,Map<String,Object> choice,boolean server){
        Map<String,Map<String,Object>> all=new LinkedHashMap<>();Set<String> want=new HashSet<>();
        for(Object o:list(catalog.get("mods"))){Map<String,Object> m=obj(o);String id=str(m.get("id"));all.put(id,m);
            boolean enabled=server ? Boolean.TRUE.equals(m.get("server")) : Boolean.TRUE.equals(m.get("required")) || (!Boolean.TRUE.equals(m.get("hidden")) && Boolean.TRUE.equals(choice.getOrDefault(id,m.get("default"))));
            if(enabled)want.add(id);
        }
        boolean changed;do{changed=false;for(String id:new ArrayList<>(want))for(Object d:list(all.get(id).get("dependencies")))changed|=want.add(str(d));}while(changed);
        List<Map<String,Object>> result=new ArrayList<>();for(var e:all.entrySet())if(want.contains(e.getKey()))result.add(e.getValue());return result;
    }
    static Path safe(Path base,String name)throws Exception{
        if(!name.matches("[A-Za-z0-9_.+ -]+\\.jar"))throw new IOException("Unsafe managed name");
        Path p=base.resolve(name);if(Files.isSymbolicLink(base)||Files.isSymbolicLink(p))throw new IOException("不支持符号链接形式的 mods 文件: "+p);return p;
    }
    static Set<String> providedIds(Path jar)throws Exception{
        Set<String> ids=new HashSet<>();try(ZipFile z=new ZipFile(jar.toFile())){var e=z.getEntry("META-INF/neoforge.mods.toml");if(e==null)return ids;
            String t=new String(z.getInputStream(e).readAllBytes(),StandardCharsets.UTF_8);
            Matcher blocks=Pattern.compile("(?s)\\[\\[mods\\]\\](.*?)(?=\\n\\s*\\[|\\z)").matcher(t);
            while(blocks.find()){Matcher m=Pattern.compile("(?m)^\\s*modId\\s*=\\s*\"([^\"]+)\"").matcher(blocks.group(1));if(m.find())ids.add(m.group(1));}
        }return ids;
    }
    static void recover(Path game)throws Exception{
        Path state=game.resolve(".friends-sync"),journal=state.resolve("transaction.json");if(!Files.exists(journal))return;
        Map<String,Object> j=read(journal);Path backup=state.resolve(str(j.get("backup")));
        if(!backup.normalize().startsWith(state)||!str(j.get("backup")).matches("backup-[0-9a-f-]+"))throw new IOException("Invalid journal");
        for(Object o:list(j.get("files"))){Map<String,Object> f=obj(o);String name=str(f.get("name"));Path dst=safe(game.resolve("mods"),name);
            if(Boolean.TRUE.equals(f.get("existed")))atomic(dst,Files.readAllBytes(safe(backup,name)));else Files.deleteIfExists(dst);
        }
        if(Files.exists(backup.resolve("receipt.json")))atomic(state.resolve("managed.json"),Files.readAllBytes(backup.resolve("receipt.json")));else Files.deleteIfExists(state.resolve("managed.json"));
        Files.delete(journal);status.accept("上次更新未完成，已恢复至更新前状态。");
    }
    static void sync(Path root,Path game,Map<String,Object> catalog,Map<String,Object> choice)throws Exception{
        Files.createDirectories(game);Files.createDirectories(root.resolve("state"));
        try(FileChannel ch=FileChannel.open(game.resolve(".friends-sync.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=ch.tryLock()){
            if(lock==null)throw new IOException("另一个更新程序正在运行");
            Path mods=game.resolve("mods"),state=game.resolve(".friends-sync");Files.createDirectories(mods);Files.createDirectories(state);recover(game);
            Path receipt=state.resolve("managed.json");Map<String,Object> old=Files.exists(receipt)?read(receipt):new LinkedHashMap<>();
            Map<String,Object> wanted=new LinkedHashMap<>();List<Map<String,Object>> selected=selected(catalog,choice,false);
            Set<String> ids=new HashSet<>();for(Map<String,Object> m:selected)for(Object id:list(m.get("modIds")))ids.add(str(id));
            for(Map<String,Object> m:selected)wanted.put(str(m.get("filename")),m.get("sha256"));
            try(var paths=Files.list(mods)){for(Path p:paths.filter(p->p.toString().endsWith(".jar")).toList()){
                if(old.containsKey(p.getFileName().toString())||wanted.containsKey(p.getFileName().toString()))continue;
                Set<String> overlap=providedIds(p);overlap.retainAll(ids);if(!overlap.isEmpty())throw new IOException("个人模组与整合包重复，请先移出该文件后重试："+p.getFileName());
            }}
            Path cache=root.resolve("state/mod-cache");Files.createDirectories(cache);int count=0;
            for(Map<String,Object> m:selected){String sha=str(m.get("sha256")),name=str(m.get("filename"));Path dst=safe(mods,name),cached=cache.resolve(sha+".jar");
                status.accept("("+(++count)+"/"+selected.size()+") 检查 / 下载 "+m.get("name"));
                if(Files.exists(dst)&&hash(dst).equals(sha))continue;
                if(!Files.exists(cached)||!hash(cached).equals(sha)){
                    byte[] b=fetch(str(m.get("url")));if(b.length!=((Number)m.get("size")).longValue()||!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b)).equals(sha))throw new IOException("下载校验失败："+name);atomic(cached,b);
                }
            }
            Set<String> affected=new LinkedHashSet<>();
            for(String name:old.keySet())if(!wanted.containsKey(name)&&Files.exists(safe(mods,name)))affected.add(name);
            for(var e:wanted.entrySet()){Path p=safe(mods,e.getKey());if(!Files.exists(p)||!hash(p).equals(str(e.getValue())))affected.add(e.getKey());}
            if(affected.isEmpty()){
                write(receipt,wanted);write(root.resolve("state/mod-options.json"),choice);
                write(game.resolve("friends-mod-release.json"),Map.of("release",catalog.get("release"),"minecraft",catalog.get("minecraft"),"neoforge",catalog.get("neoforge")));
                status.accept("所有已选择模组均为发布版本，无需下载。");return;
            }
            Path backup=state.resolve("backup-"+UUID.randomUUID());Files.createDirectories(backup);List<Object> files=new ArrayList<>();
            for(String name:affected){Path p=safe(mods,name);boolean exists=Files.exists(p);if(exists)Files.copy(p,safe(backup,name));files.add(Map.of("name",name,"existed",exists));}
            if(Files.exists(receipt))Files.copy(receipt,backup.resolve("receipt.json"));
            write(state.resolve("transaction.json"),Map.of("backup",backup.getFileName().toString(),"files",files));
            try{
                for(String name:old.keySet())if(!wanted.containsKey(name))Files.deleteIfExists(safe(mods,name));
                for(var e:wanted.entrySet()){Path dst=safe(mods,e.getKey());if(!Files.exists(dst)||!hash(dst).equals(str(e.getValue())))atomic(dst,Files.readAllBytes(cache.resolve(e.getValue()+".jar")));}
                write(receipt,wanted);write(root.resolve("state/mod-options.json"),choice);write(game.resolve("friends-mod-release.json"),Map.of("release",catalog.get("release"),"minecraft",catalog.get("minecraft"),"neoforge",catalog.get("neoforge")));
                Files.delete(state.resolve("transaction.json"));
            }catch(Exception e){recover(game);throw e;}
            status.accept("模组已对齐；个人模组、按键、地图和账号设置保留。");
        }
    }
    static void checkRunning(Path root,Path game)throws Exception{
        long me=ProcessHandle.current().pid();String r=root.toString().toLowerCase(),g=game.toString().toLowerCase();
        for(ProcessHandle p:ProcessHandle.allProcesses().toList())if(p.pid()!=me){var info=p.info();String cmd=info.command().orElse("").toLowerCase(),line=info.commandLine().orElse("").toLowerCase();
            if((cmd.contains("prismlauncher")&&line.contains(r))||(cmd.contains("java")&&line.contains(g)&&!line.contains("friends-updater.jar")))throw new IOException("请先关闭此客户端的游戏和启动器，再运行 Start。");
        }
    }
    static void setupPrism(Path root,Path instance,Map<String,Object> c)throws Exception{
        Files.createDirectories(instance);Path pack=instance.resolve("mmc-pack.json");
        if(!Files.exists(pack))write(pack,Map.of("formatVersion",1,"components",List.of(Map.of("uid","net.minecraft","version",c.get("minecraft"),"important",true),Map.of("uid","net.neoforged","version",c.get("neoforge"),"important",true))));
        else{
            Map<String,String> versions=new HashMap<>();for(Object o:list(read(pack).get("components"))){Map<String,Object> m=obj(o);versions.put(str(m.get("uid")),str(m.get("version")));}
            if(!Objects.equals(versions.get("net.minecraft"),c.get("minecraft"))||!Objects.equals(versions.get("net.neoforged"),c.get("neoforge")))throw new IOException("Prism 实例版本被修改，请恢复为发布版本。");
        }
        Path cfg=instance.resolve("instance.cfg");Properties props=new Properties();if(Files.exists(cfg))try(var reader=Files.newBufferedReader(cfg)){props.load(reader);}
        props.setProperty("InstanceType","OneSix");props.setProperty("name","FriendsMC-"+c.get("minecraft"));props.setProperty("OverrideJavaLocation","true");props.setProperty("JavaPath",Path.of(System.getProperty("java.home"),"bin/java").toString());props.setProperty("OverrideMemory","true");props.setProperty("MinMemAlloc","1024");props.setProperty("MaxMemAlloc","4096");
        StringBuilder text=new StringBuilder("[General]\n");for(String k:props.stringPropertyNames())if(!k.equals("[General]"))text.append(k).append('=').append(props.getProperty(k)).append('\n');atomic(cfg,text.toString().getBytes(StandardCharsets.UTF_8));
    }
    static void mac(Path root,boolean prepare)throws Exception{
        Path data=root.resolve("launcher-data");Files.createDirectories(data);
        try(FileChannel ch=FileChannel.open(root.resolve(".start.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=ch.tryLock()){
            if(lock==null)throw new IOException("更新程序已经运行");status.accept("正在核对发布版本…");Map<String,Object> c=channel();
            String id="FriendsMC-"+c.get("minecraft")+"-"+c.get("neoforge");Path instance=data.resolve("instances").resolve(id),game=instance.resolve(".minecraft");checkRunning(root,game);
            Map<String,Object> catalog=catalog(str(c.get("packUrl")),str(c.get("catalogSha256")));checkVersions(c,catalog);
            Map<String,Object> selected=choices(root,catalog,true);sync(root,game,catalog,selected);setupPrism(root,instance,c);
            Path servers=game.resolve("servers.dat");if(!Files.exists(servers))Files.copy(root.resolve("templates/servers.dat"),servers);
            if(!prepare){Path app=root.resolve("launcher/Prism Launcher.app/Contents/MacOS/prismlauncher");
                if(!Files.isExecutable(app))throw new IOException("Prism 未就绪，请重新解压完整 macOS 客户端。");
                new ProcessBuilder("/usr/bin/open","-n",root.resolve("launcher/Prism Launcher.app").toString(),"--args","--dir",data.toString(),"--show",id).start();
            }
            status.accept("更新完成。请在 Prism 中登录微软账号，双击 FriendsMC 启动。首次下载进度在 Prism 显示。");
        }
    }
    static void checkVersions(Map<String,Object> c,Map<String,Object> cat)throws Exception{for(String k:List.of("release","minecraft","neoforge"))if(!Objects.equals(c.get(k),cat.get(k)))throw new IOException("发布版本与模组清单不一致");}
    static void macUi(Path root,boolean prepare)throws Exception{
        final JFrame[] frame={null};final JTextArea[] area={null};final JProgressBar[] progress={null};final boolean[] done={false};SwingUtilities.invokeAndWait(()->{
            JFrame f=new JFrame("Friends MC · 更新中心");f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);JPanel p=new JPanel(new BorderLayout(16,16));p.setBorder(new EmptyBorder(24,24,24,24));p.setBackground(new Color(12,20,27));
            JLabel title=new JLabel("FRIENDS MC  /  macOS");title.setFont(new Font("Dialog",Font.BOLD,24));title.setForeground(new Color(169,228,184));p.add(title,BorderLayout.NORTH);
            JTextArea a=new JTextArea(16,58);a.setEditable(false);a.setLineWrap(true);a.setWrapStyleWord(true);a.setFont(new Font("Dialog",Font.PLAIN,14));a.setBackground(new Color(19,31,41));a.setForeground(new Color(220,235,241));p.add(new JScrollPane(a));
            JButton copy=new JButton("复制日志");copy.addActionListener(e->Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(a.getText()),null));
            JPanel bottom=new JPanel(new BorderLayout(12,12));JProgressBar bar=new JProgressBar();bar.setIndeterminate(true);bar.setForeground(new Color(169,228,184));bottom.add(bar);bottom.add(copy,BorderLayout.EAST);p.add(bottom,BorderLayout.SOUTH);
            f.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosing(java.awt.event.WindowEvent e){if(done[0])f.dispose();else if(JOptionPane.showConfirmDialog(f,"取消本次更新？下次启动会检查并恢复未完成的更新。","取消",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)System.exit(2);}});
            f.setContentPane(p);f.pack();f.setLocationRelativeTo(null);f.setVisible(true);frame[0]=f;area[0]=a;progress[0]=bar;
        });
        StringBuilder log=new StringBuilder();status=s->{System.out.println(s);log.append(s).append('\n');SwingUtilities.invokeLater(()->{area[0].append(s+"\n");area[0].setCaretPosition(area[0].getDocument().getLength());Matcher m=Pattern.compile("^\\((\\d+)/(\\d+)\\)").matcher(s);if(m.find()){progress[0].setIndeterminate(false);progress[0].setMaximum(Integer.parseInt(m.group(2)));progress[0].setValue(Integer.parseInt(m.group(1)));}});};
        try{mac(root,prepare);}catch(Exception e){status.accept("更新失败："+e.getMessage()+"\n游戏未启动，请复制日志联系服主。");throw e;}finally{
            Files.createDirectories(root.resolve("logs"));Files.writeString(root.resolve("logs/latest-update.log"),log.toString());SwingUtilities.invokeLater(()->{done[0]=true;progress[0].setIndeterminate(false);});
        }
    }
    public static void main(String[] args){
        try{
            if(args.length<2)throw new IOException("Usage: mac ROOT | sync ROOT GAMEDIR PACKURL SHA256 | test ROOT GAMEDIR CATALOG [minimal]");
            Path root=Path.of(args[1]).toAbsolutePath().normalize();
            if(args[0].equals("mac")){macUi(root,args.length>2&&args[2].equals("--prepare-only"));return;}
            Path game=Path.of(args[2]).toAbsolutePath().normalize();checkRunning(root,game);
            boolean test=args[0].equals("test");Map<String,Object> cat=test?read(Path.of(args[3])):catalog(args[3],args[4]);validate(cat);
            Map<String,Object> selection=choices(root,cat,!test);
            if(test&&args.length>4&&args[4].equals("minimal"))for(Object o:list(cat.get("mods")))selection.put(str(obj(o).get("id")),false);
            sync(root,game,cat,selection);System.exit(0);
        }catch(Exception e){System.err.println(e.getMessage());e.printStackTrace();if(args.length>0&&!args[0].equals("mac"))System.exit(1);}
    }
    static class Json {
        final String s;int i=0;Json(String s){this.s=s.startsWith("\ufeff")?s.substring(1):s;}
        static Object parse(String s){Json j=new Json(s);Object o=j.value();j.ws();if(j.i!=j.s.length())throw new IllegalArgumentException("Trailing JSON");return o;}
        void ws(){while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;}
        char take(){if(i>=s.length())throw new IllegalArgumentException("Incomplete JSON");return s.charAt(i++);}
        Object value(){ws();char c=take();if(c=='{'){Map<String,Object> m=new LinkedHashMap<>();ws();if(s.charAt(i)=='}'){i++;return m;}while(true){ws();if(take()!='\"')throw new IllegalArgumentException();String key=string();ws();if(take()!=':')throw new IllegalArgumentException();m.put(key,value());ws();c=take();if(c=='}')return m;if(c!=',')throw new IllegalArgumentException();}}
            if(c=='['){List<Object> a=new ArrayList<>();ws();if(s.charAt(i)==']'){i++;return a;}while(true){a.add(value());ws();c=take();if(c==']')return a;if(c!=',')throw new IllegalArgumentException();}}
            if(c=='\"')return string();int start=i-1;while(i<s.length()&&",]} \r\n\t".indexOf(s.charAt(i))<0)i++;String v=s.substring(start,i);return switch(v){case "true"->true;case "false"->false;case "null"->null;default->Double.valueOf(v);};}
        String string(){StringBuilder b=new StringBuilder();while(true){char c=take();if(c=='\"')return b.toString();if(c=='\\'){c=take();switch(c){case 'u'-> {b.append((char)Integer.parseInt(s.substring(i,i+4),16));i+=4;}case 'n'->b.append('\n');case 'r'->b.append('\r');case 't'->b.append('\t');case 'b'->b.append('\b');case 'f'->b.append('\f');case '\\','/','\"'->b.append(c);default->throw new IllegalArgumentException();}}else b.append(c);}}
        static String stringify(Object o){if(o==null)return "null";if(o instanceof Boolean||o instanceof Number)return o.toString();if(o instanceof Map<?,?> m){List<String> p=new ArrayList<>();m.forEach((k,v)->p.add(stringify(k.toString())+":"+stringify(v)));return "{"+String.join(",",p)+"}";}if(o instanceof Collection<?> c){List<String> p=new ArrayList<>();c.forEach(v->p.add(stringify(v)));return "["+String.join(",",p)+"]";}StringBuilder b=new StringBuilder("\"");for(char c:o.toString().toCharArray())switch(c){case '\\'->b.append("\\\\");case '\"'->b.append("\\\"");case '\n'->b.append("\\n");case '\r'->b.append("\\r");case '\t'->b.append("\\t");default->{if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString();}
    }
}
