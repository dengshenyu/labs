package example.zhihu;

import config.Config;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Created by shenyuan on 17/2/16.
 */
public class BaseDao {

    protected Connection getConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            return DriverManager.getConnection(Config.mysqlUrl, Config.mysqlUser, Config.mysqlPassword);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
