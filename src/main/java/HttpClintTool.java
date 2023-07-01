import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.CharsetUtils;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;


import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by chenjunlai on 2018/5/10
 */

public class HttpClintTool {
    static Logger log = Logger.getLogger(HttpClintTool.class);

    /**
     * 证书密码
     */
    private static String clientCertPassword = "123456";
    private HttpClient httpClient;




    public HttpClintTool() throws Exception{
       // HttpHost proxy = new HttpHost("192.168.13.19", 7777);
        //DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

        httpClient = HttpClients.custom()
            //    .setRoutePlanner(routePlanner)
                .setConnectionTimeToLive(15, TimeUnit.SECONDS)
                .build();
        /*
        StringEntity se = new StringEntity("1234");
        se.setContentType("text/json");
        se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        URI uri =new URI("https://222.76.227.136:18080/qrcode_CAVerify/");
        log.info("post地址:"+uri);
        HttpPost httpPost = new HttpPost(uri);
        // httpPost.setEntity(reqEntity);
        //httpPost.addHeader("Cookie", cookie);
        httpPost.setEntity(se);
        HttpResponse response = httpClient.execute(httpPost);
        String resultString = null;
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            resultString = EntityUtils.toString(response.getEntity());
        }
        log.info("POST返回值 ： "+response.getStatusLine().getStatusCode()+", 返回内容："+resultString+",response: "+response );
        */
    }






    /**
     *
     * @param remotePath 远程地址
     * @return
     * @throws URISyntaxException
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String post(String remotePath, String jsonstr)
            throws URISyntaxException, ClientProtocolException, IOException {

       /* Set<String> keys = args.keySet();

        URIBuilder https = new URIBuilder().setScheme("https").setHost(host).setPort(port).setPath(remotePath);
        for (String key : keys) {
            https.setParameter(key,args.get(key));
        }
        URI uri = https.build();
        log.info("post地址:{}",uri);
        HttpPost httpPost = new HttpPost(uri);
        FileEntity fileEntity = new FileEntity(new File(filename));
        fileEntity.setChunked(true);
        httpPost.setEntity(fileEntity);
        httpPost.addHeader("Cookie", cookie);
        */
        /*
        // 把文件转换成流对象FileBody
        File localFile = new File(localPath);
        FileBody fileBody = new FileBody(localFile);
        // 以浏览器兼容模式运行，防止文件名乱码。
        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addPart(filename, fileBody)
                .setCharset(CharsetUtils.get("UTF-8")).build();
        // uploadFile对应服务端类的同名属性<File类型>
        // .addPart("uploadFileName", uploadFileName)
        // uploadFileName对应服务端类的同名属性<String类型>
        */
        StringEntity se = new StringEntity(jsonstr);
        se.setContentType("application/json;charset=UTF-8");
        se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

        URI uri =new URI(remotePath);
        log.info("post地址:"+uri);
        HttpPost httpPost = new HttpPost(uri);
       // httpPost.setEntity(reqEntity);
        //httpPost.addHeader("Cookie", cookie);
        httpPost.setEntity(se);
        HttpResponse response = httpClient.execute(httpPost);
        String resultString = null;
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            resultString = EntityUtils.toString(response.getEntity());
        }
        log.info("POST返回值 ： "+response.getStatusLine().getStatusCode()+", 返回内容："+resultString+",response: "+response );

        return resultString;
    }
}


