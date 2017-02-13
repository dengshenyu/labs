package util;

import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by shenyuan on 17/2/6.
 */
public class HttpUtils {
    private static int timeout = 60000;
    static {
        //Ignore the "Unrecognized Name" warning sent from weixin server
        System.setProperty("jsse.enableSNIExtension", "false");
    }


    public static String post(String url, Map<String, String> headers, Map<String, String> params, HttpContext httpContext) {
        String body = null;

        CloseableHttpClient client = getHttpClient();
        if (client == null) {
            System.out.println("Http client is null!");
            return body;
        }

        CloseableHttpResponse response = null;
        try {
            HttpPost request = new HttpPost(url);
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet())
                    request.setHeader(header.getKey(), header.getValue());
            }

            if (params != null) {
                List<NameValuePair> nameValues = new ArrayList<NameValuePair>();
                for (Map.Entry<String, String> param : params.entrySet())
                    nameValues.add(new BasicNameValuePair(param.getKey(), param.getValue()));
                request.setEntity(new UrlEncodedFormEntity(nameValues));
            }

            response = client.execute(request, httpContext);
            body = getResponseContent(response);
            return body;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    EntityUtils.consume(response.getEntity());
                    response.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static byte[] getBinary(String url, Map<String, String> headers, HttpContext httpContext) {
        CloseableHttpClient client = getHttpClient();
        CloseableHttpResponse response = null;

        try {
            HttpGet request = new HttpGet(url);
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet())
                    request.setHeader(header.getKey(), header.getValue());
            }
            response = client.execute(request, httpContext);
            InputStream is = response.getEntity().getContent();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int i;
            while ((i = is.read()) != -1) {
                os.write(i);
            }
            byte[] bytes = os.toByteArray();
            os.close();

            return bytes;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    EntityUtils.consume(response.getEntity());
                    response.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static String getBinaryAsHex(String url, Map<String, String> headers, HttpContext httpContext) {
        CloseableHttpClient client = getHttpClient();
        CloseableHttpResponse response = null;

        try {
            HttpGet request = new HttpGet(url);
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet())
                    request.setHeader(header.getKey(), header.getValue());
            }
            response = client.execute(request, httpContext);
            InputStream is = response.getEntity().getContent();
            StringBuilder sb = new StringBuilder();
            int i;
            while ((i = is.read()) != -1) {
                sb.append(String.format("%02X", i));
            }
            return sb.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    EntityUtils.consume(response.getEntity());
                    response.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static String get(String url, Map<String, String> headers, HttpContext httpContext) {
        String body = null;

        for (int i = 0; i < 3; i++) {
            CloseableHttpClient client = getHttpClient();
            if (client == null) {
                System.out.println("Http client is null!");
                return body;
            }

            CloseableHttpResponse response = null;
            try {
                HttpGet request = new HttpGet(url);
                if (headers != null) {
                    for (Map.Entry<String, String> header : headers.entrySet())
                        request.setHeader(header.getKey(), header.getValue());
                }
                response = client.execute(request, httpContext);
                body = getResponseContent(response);
                return body;
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (response != null) {
                        EntityUtils.consume(response.getEntity());
                        response.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    private static String getResponseContent(CloseableHttpResponse response) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String data = sb.toString();
        return data;
    }


    private static CloseableHttpClient getHttpClient() {
        try {
            //https相关
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new HostnameVerifier() {
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
            //访问控制相关
            RequestConfig config = RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .setConnectTimeout(timeout)
                    .setConnectionRequestTimeout(timeout)
                    .setSocketTimeout(timeout).build();
            //构建
            CloseableHttpClient client = HttpClients.
                    custom()
                    .setSSLSocketFactory(sslsf)
                    .setDefaultRequestConfig(config)

                    .build();
            return client;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
