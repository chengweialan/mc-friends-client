import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkTest {
    static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        byte[] good="verified content".getBytes(StandardCharsets.UTF_8);
        String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(good));
        AtomicInteger retries=new AtomicInteger();
        server.createContext("/good",e->{e.sendResponseHeaders(200,good.length);e.getResponseBody().write(good);e.close();});
        server.createContext("/bad",e->{byte[] b="corrupt".getBytes();e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();});
        server.createContext("/retry",e->{int code=retries.incrementAndGet()<2?503:200;e.sendResponseHeaders(code,good.length);e.getResponseBody().write(good);e.close();});
        server.start();String base="http://127.0.0.1:"+server.getAddress().getPort();
        try{
            require(Arrays.equals(good,FriendsUpdater.checkedFile(List.of(base+"/missing",base+"/good"),sha,good.length)),"HTTP fallback");
            require(Arrays.equals(good,FriendsUpdater.checkedFile(List.of(base+"/bad",base+"/good"),sha,good.length)),"Hash fallback");
            require(Arrays.equals(good,FriendsUpdater.checkedFile(List.of(base+"/retry"),sha,good.length)),"Retry");
            boolean failed=false;try{FriendsUpdater.checkedFile(List.of(base+"/bad"),sha,good.length);}catch(java.io.IOException e){failed=e.getMessage().contains(base+"/bad");}require(failed,"Fail closed with address");
            Path root=Files.createTempDirectory("friends-sources-test");
            FriendsUpdater.write(root.resolve("download-sources.json"),Map.of("mirrors",List.of("https://example.com/mc/")));
            FriendsUpdater.loadSources(root);require(FriendsUpdater.sources("https://origin.test/channel.json","channel.json").equals(List.of("https://example.com/mc/channel.json","https://origin.test/channel.json")),"Mirror preference");
            FriendsUpdater.write(root.resolve("download-sources.json"),Map.of("mirrors",List.of("http://example.com")));
            failed=false;try{FriendsUpdater.loadSources(root);}catch(java.io.IOException e){failed=true;}require(failed,"Reject non HTTPS config");
            Files.delete(root.resolve("download-sources.json"));Files.delete(root);
            System.out.println("PASS: HTTP fallback, corrupt mirror fallback, retry, fail closed, source ordering, HTTPS config");
        }finally{server.stop(0);}
    }
}
