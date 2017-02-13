
import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import util.HttpUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by shenyuan on 17/2/13.
 */
public class ZhihuLogin {
    private static HttpContext httpContext;
    private static Map<String, String> headers;

    static {
        CookieStore cookieStore = new BasicCookieStore();
        httpContext = new BasicHttpContext();
        httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

        headers = new HashMap<String, String>();
        headers.put("Host", "www.zhihu.com");
        headers.put("Referer", "https://www.zhihu.com/");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:33.0) Gecko/20100101 Firefox/33.0");
    }

    public static void main(String[] args) {
        try {
            System.out.println("请输入你的用户名: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String account = br.readLine().trim();
            System.out.println("请输入你的密码: ");
            String secret = br.readLine().trim();
            ZhihuLogin login = new ZhihuLogin();
            login.login(account, secret);
            System.out.println(login.getProfile());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private String getProfile() {
        //通过查看用户个人信息来判断是否已经登录
        String url = "https://www.zhihu.com/settings/profile";
        String resp = HttpUtils.get(url, headers, httpContext);
        return resp;
    }

    private String getXsrf() {
        String indexUrl = "https://www.zhihu.com";

        String indexPage = HttpUtils.get(indexUrl, headers, httpContext);
        Pattern pattern = Pattern.compile("name=\"_xsrf\" value=\"(.*?)\"");
        Matcher matcher = pattern.matcher(indexPage);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String getCaptcha() {
        try {
            long t = System.currentTimeMillis();
            String captchaUrl = "https://www.zhihu.com/captcha.gif?r=" + t + "&type=login";
            byte[] bytes = HttpUtils.getBinary(captchaUrl, headers, httpContext);

            JFrame frame = new JFrame();
            JPanel panel = new JPanel(new BorderLayout());
            JLabel imageLabel = new JLabel(new ImageIcon(ImageIO.read(new ByteArrayInputStream(bytes))));
            panel.add(imageLabel);
            frame.add(panel);
            frame.setSize(300, 300);
            frame.setVisible(true);

            System.out.println("请输入验证码:");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            return br.readLine().trim();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private boolean login(String account, String secret) {
        Pattern pattern = Pattern.compile("^1\\d{10}$");
        Matcher matcher = pattern.matcher(account);

        String postUrl;
        Map<String, String> params = new HashMap<String, String>();
        if (matcher.find()) {
            System.out.println("手机号登录");
            postUrl = "https://www.zhihu.com/login/phone_num";
            params.put("_xsrf", getXsrf());
            params.put("password", secret);
            params.put("remember_me", "true");
            params.put("phone_num", account);
        } else {
            //邮箱登录
            postUrl = "https://www.zhihu.com/login/email";
            params.put("_xsrf", getXsrf());
            params.put("password", secret);
            params.put("remember_me", "true");
            params.put("email", account);
        }

        // 不使用验证码直接登录成功
        String resp = HttpUtils.post(postUrl, headers, params, httpContext);
        if (resp != null) {
            System.out.println(resp);
            return true;
        }

        params.put("captcha", getCaptcha());
        resp = HttpUtils.post(postUrl, headers, params, httpContext);
        System.out.println(resp);
        return true;
    }

}
