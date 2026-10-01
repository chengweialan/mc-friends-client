import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.zip.*;

/** Native packwiz integration; deliberately separate from the legacy Friends pack. */
public class FoolUpdater extends FriendsUpdater {
    static final String BASE="https://friends-mc-downloads-1318356926.cos.ap-beijing.myqcloud.com/friends-mc/packs/fool";
    static boolean windows(){return System.getProperty("os.name").toLowerCase().contains("win");}
    static String platform(){return windows()?"windows":System.getProperty("os.arch").contains("aarch")?"mac-arm":"mac-x64";}
    static void validateFool(Map<String,Object> c)throws Exception{
        if(!"fool".equals(c.get("packId"))||((Number)c.get("schema")).intValue()!=1||((Number)c.get("java")).intValue()!=17)throw new IOException("愚者更新器需要升级，请获取新版客户端补丁。");
        for(String k:List.of("release","minecraft","forge"))if(!str(c.get(k)).matches("[A-Za-z0-9][A-Za-z0-9._-]{0,79}"))throw new IOException("Invalid version");
        if(!str(c.get("packUrl")).equals(BASE+"/releases/"+c.get("release")+"/pack.toml"))throw new IOException("Invalid release URL");
        if(!str(c.get("packSha256")).matches("[a-f0-9]{64}"))throw new IOException("Invalid release fingerprint");
        for(Object value:obj(c.get("runtime")).values()){
            Map<String,Object> r=obj(value);
            if(!str(r.get("filename")).matches("zulu17[0-9A-Za-z._-]+\\.zip")||!str(r.get("sha256")).matches("[a-f0-9]{64}"))throw new IOException("Invalid Java runtime");
            String home=str(r.get("home"));if(home.startsWith("/")||home.contains("..")||home.contains("\\")||home.contains(":"))throw new IOException("Invalid Java home");
        }
    }
    static Map<String,Object> foolChannel()throws Exception{
        status.accept("读取愚者国内更新清单…");
        byte[] b=fetchChecked(List.of(BASE+"/channel.json?t="+System.currentTimeMillis()),x->validateFool(obj(Json.parse(new String(x,StandardCharsets.UTF_8)))));
        return obj(Json.parse(new String(b,StandardCharsets.UTF_8)));
    }
    static void download(String url,Path target,String expected)throws Exception{
        Files.createDirectories(target.getParent());Path tmp=target.resolveSibling(target.getFileName()+".partial");
        HttpRequest req=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(10)).GET().build();
        HttpResponse<InputStream> response=HTTP.send(req,HttpResponse.BodyHandlers.ofInputStream());
        try(InputStream in=response.body()){
            if(response.statusCode()!=200)throw new IOException("下载失败 HTTP "+response.statusCode());
            try(OutputStream out=Files.newOutputStream(tmp)){byte[] buffer=new byte[65536];long count=0,last=0;int n;
                while((n=in.read(buffer))!=-1){out.write(buffer,0,n);count+=n;if(count-last>4*1024*1024){status.accept("下载 Java 17："+(count/1024/1024)+" MB");last=count;}}
            }
        }
        if(!hash(tmp).equals(expected))throw new IOException("Java 下载不完整，请重试。");
        Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);
    }
    static Path gameJava(Path root,Path game,Map<String,Object> c)throws Exception{
        Map<String,Object> r=obj(obj(c.get("runtime")).get(platform()));String filename=str(r.get("filename"));
        Path destination=windows()?game.resolve("java"):root.resolve("runtime-fool").resolve(filename.replace(".zip",""));
        Path java=destination.resolve(str(r.get("home"))).resolve(windows()?"bin/java.exe":"bin/java");
        Path complete=destination.resolve(".friends-runtime.json");
        if(Files.exists(complete)&&Files.exists(java)&&str(read(complete).get("sha256")).equals(str(r.get("sha256"))))return java;
        Path archive=root.resolve("state/runtime-cache").resolve(filename);Files.createDirectories(archive.getParent());
        if(!Files.exists(archive)||!hash(archive).equals(str(r.get("sha256"))))download(BASE+"/runtime/"+filename,archive,str(r.get("sha256")));
        Path staging=destination.resolveSibling(destination.getFileName()+"-prepare-"+UUID.randomUUID());Files.createDirectories(staging);
        try(ZipInputStream zip=new ZipInputStream(Files.newInputStream(archive))){ZipEntry entry;
            while((entry=zip.getNextEntry())!=null){String name=entry.getName();Path p=staging.resolve(name).normalize();
                if(!p.startsWith(staging)||name.contains(":")||name.contains("\\"))throw new IOException("Invalid runtime ZIP entry");
                if(entry.isDirectory())Files.createDirectories(p);else{Files.createDirectories(p.getParent());Files.copy(zip,p,StandardCopyOption.REPLACE_EXISTING);}
            }
        }
        Path executable=staging.resolve(str(r.get("home"))).resolve(windows()?"bin/java.exe":"bin/java");
        if(!Files.isRegularFile(executable))throw new IOException("Java runtime incomplete");
        if(!windows()){
            Path home=staging.resolve(str(r.get("home")));
            try(var paths=Files.list(home.resolve("bin"))){paths.forEach(p->p.toFile().setExecutable(true,false));}
            Path helper=home.resolve("lib/jspawnhelper");if(Files.exists(helper))helper.toFile().setExecutable(true,false);
        }
        write(staging.resolve(".friends-runtime.json"),Map.of("sha256",r.get("sha256")));
        Files.createDirectories(destination.getParent());
        if(Files.exists(destination)){
            Path backups=root.resolve("state/runtime-backups");Files.createDirectories(backups);
            Files.move(destination,backups.resolve("fool-java-"+UUID.randomUUID()));
        }
        Files.move(staging,destination);return java;
    }
    static void nativeSync(Path root,Path game,String packUrl,boolean gui)throws Exception{
        List<String> command=new ArrayList<>(List.of(Path.of(System.getProperty("java.home"),"bin",windows()?"java.exe":"java").toString(),"-Dstdout.encoding=UTF-8","-Dstderr.encoding=UTF-8","-jar",root.resolve("tools/packwiz-installer-bootstrap.jar").toString(),"--bootstrap-no-update","--bootstrap-main-jar",root.resolve("tools/packwiz-installer.jar").toString(),"-s","client"));
        if(!gui)command.add("-g");command.add(packUrl);
        Process process=new ProcessBuilder(command).directory(game.toFile()).redirectErrorStream(true).start();
        Thread cleanup=new Thread(()->process.destroy());Runtime.getRuntime().addShutdownHook(cleanup);
        try{
            try(BufferedReader input=process.inputReader(StandardCharsets.UTF_8)){String line;while((line=input.readLine())!=null)status.accept(line);}
            if(process.waitFor()!=0)throw new IOException("packwiz 更新失败；此次不会打开游戏。请重试。");
        }finally{process.destroy();Runtime.getRuntime().removeShutdownHook(cleanup);}
    }
    static void syncFool(Path root,Path game,Map<String,Object> c,boolean gui)throws Exception{
        validateFool(c);checkRunning(root,game);Files.createDirectories(game);
        try(FileChannel ch=FileChannel.open(game.resolve(".friends-fool.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=ch.tryLock()){
            if(lock==null)throw new IOException("此愚者实例正在更新。");
            // Check the selected immutable pack descriptor before allowing packwiz to change files.
            checkedFile(List.of(str(c.get("packUrl"))),str(c.get("packSha256")),-1);
            gameJava(root,game,c);status.accept("同步愚者模组、任务、配方和资源；首次约 2GB…");
            nativeSync(root,game,str(c.get("packUrl")),gui);
            write(game.resolve("friends-fool-release.json"),c);
            status.accept("愚者文件已对齐。未托管的个人文件保留。");
        }
    }
    static void prismFool(Path root,Path instance,Path java,Map<String,Object> c)throws Exception{
        Files.createDirectories(instance);Path mmc=instance.resolve("mmc-pack.json");
        if(Files.exists(mmc)){
            Map<String,String> actual=new HashMap<>();for(Object v:list(read(mmc).get("components"))){Map<String,Object> m=obj(v);actual.put(str(m.get("uid")),str(m.get("version")));}
            if(!c.get("minecraft").equals(actual.get("net.minecraft"))||!c.get("forge").equals(actual.get("net.minecraftforge")))throw new IOException("Prism 愚者实例版本被修改。");
        }else write(mmc,Map.of("formatVersion",1,"components",List.of(Map.of("uid","net.minecraft","version",c.get("minecraft"),"important",true),Map.of("uid","net.minecraftforge","version",c.get("forge"),"important",true))));
        Path cfg=instance.resolve("instance.cfg");Properties p=new Properties();if(Files.exists(cfg))try(Reader reader=Files.newBufferedReader(cfg)){p.load(reader);}
        p.setProperty("InstanceType","OneSix");p.setProperty("name","FriendsMC · 愚者");p.setProperty("OverrideJavaLocation","true");p.setProperty("JavaPath",java.toString());
        p.putIfAbsent("OverrideMemory","true");p.putIfAbsent("MinMemAlloc","1024");p.putIfAbsent("MaxMemAlloc","6144");
        StringBuilder s=new StringBuilder("[General]\n");for(String key:p.stringPropertyNames())if(!key.equals("[General]"))s.append(key).append('=').append(p.getProperty(key)).append('\n');atomic(cfg,s.toString().getBytes(StandardCharsets.UTF_8));
    }
    static void macFool(Path root)throws Exception{
        final JTextArea[] area={null};final JFrame[] frame={null};final boolean[] finished={false};
        SwingUtilities.invokeAndWait(()->{
            JFrame f=new JFrame("Friends MC · 愚者更新");JPanel panel=new JPanel(new BorderLayout(12,12));panel.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));panel.setBackground(new Color(12,20,27));
            JLabel title=new JLabel("THE FOOL / 愚者 · 独立世界");title.setFont(new Font("Dialog",Font.BOLD,24));title.setForeground(new Color(169,228,184));panel.add(title,BorderLayout.NORTH);
            JTextArea a=new JTextArea(18,65);a.setEditable(false);a.setLineWrap(true);a.setBackground(new Color(19,31,41));a.setForeground(Color.WHITE);panel.add(new JScrollPane(a));JLabel note=new JLabel("同步完成后打开 Prism；服务器由服主切换。");note.setForeground(Color.LIGHT_GRAY);panel.add(note,BorderLayout.SOUTH);f.setContentPane(panel);f.pack();f.setLocationRelativeTo(null);f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            f.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosing(java.awt.event.WindowEvent e){if(finished[0])f.dispose();else if(JOptionPane.showConfirmDialog(f,"取消本次更新？","愚者",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)System.exit(2);}});f.setVisible(true);frame[0]=f;area[0]=a;
        });
        status=s->{System.out.println(s);SwingUtilities.invokeLater(()->{area[0].append(s+"\n");area[0].setCaretPosition(area[0].getDocument().getLength());});};
        try(FileChannel ch=FileChannel.open(root.resolve(".start.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=ch.tryLock()){
            if(lock==null)throw new IOException("另一更新器正在运行。");Map<String,Object> c=foolChannel();String id="FriendsMC-fool-"+c.get("minecraft")+"-"+c.get("forge");Path data=root.resolve("launcher-data"),instance=data.resolve("instances").resolve(id),game=instance.resolve(".minecraft");
            syncFool(root,game,c,true);prismFool(root,instance,gameJava(root,game,c),c);
            if(!Files.exists(game.resolve("servers.dat")))Files.copy(root.resolve("templates/servers.dat"),game.resolve("servers.dat"));
            new ProcessBuilder("/usr/bin/open","-n",root.resolve("launcher/Prism Launcher.app").toString(),"--args","--dir",data.toString(),"--show",id).start();
            status.accept("愚者同步完成。请在 Prism 中登录并启动；进服前确认当前开放的是愚者。");
        }catch(Exception e){status.accept("未完成："+e.getMessage());throw e;}finally{finished[0]=true;}
    }
    public static void main(String[] args){
        try{
            Path root=Path.of(args[1]).toAbsolutePath().normalize();
            switch(args[0]){
                case "channel" -> write(Path.of(args[2]),foolChannel());
                case "sync" -> syncFool(root,Path.of(args[2]).toAbsolutePath().normalize(),read(Path.of(args[3])),true);
                case "mac" -> macFool(root);
                default -> throw new IOException("Unknown command");
            }
        }catch(Exception e){System.err.println(e.getMessage());System.exit(1);}
    }
}
