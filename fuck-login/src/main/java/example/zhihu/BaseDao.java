package example.zhihu;

import config.Config;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Created by shenyuan on 17/2/16.
 */
public class BaseDao {
    private String url, user, password;

    public BaseDao(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    protected Connection getConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            return DriverManager.getConnection(url, user, password);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
