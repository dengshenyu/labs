package core;

import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import util.HttpUtils;
import util.JsonUtils;

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
    private HttpContext httpContext;
    private Map<String, String> headers;

    public ZhihuLogin(HttpContext httpContext, Map<String, String> headers) {
        this.httpContext = httpContext;
        this.headers = headers;
    }

    public String getProfile() {
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

    public boolean login(String account, String secret) {
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

        params.put("captcha", getCaptcha());
        String resp = HttpUtils.post(postUrl, headers, params, httpContext);
        System.out.println(resp);
        if (resp != null) {
            Map respMap = JsonUtils.fromJson(resp, Map.class);
            return ((Integer)respMap.get("r")) == 0;
        }

        return false;
    }

}
