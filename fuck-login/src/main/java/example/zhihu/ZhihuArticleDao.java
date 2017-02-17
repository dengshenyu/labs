package example.zhihu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

/**
 * 实现数据库article表的操作
 * Created by shenyuan on 17/2/16.
 */
public class ZhihuArticleDao extends BaseDao {
    public void save(ZhihuArticleDO articleDO) {
        Connection conn = getConnection();
        PreparedStatement stmt = null;

        try {
            //stmt = conn.createStatement();
            String sql = "insert into zhihu_article(article_id, title, author_name, author_url_token, excerpt, " +
                    "content, create_time, update_time, voteup_count) values(?, ?, ?, ?, ?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, articleDO.getArticleId());
            stmt.setString(2, articleDO.getTitle());
            stmt.setString(3, articleDO.getAuthorName());
            stmt.setString(4, articleDO.getAuthorUrlToken());
            stmt.setString(5, articleDO.getExcerpt());
            stmt.setString(6, articleDO.getContent());

            stmt.setTimestamp(7, new Timestamp(articleDO.getCreateTime().getTime()));
            stmt.setTimestamp(8, new Timestamp(articleDO.getUpdateTime().getTime()));
            stmt.setInt(9, articleDO.getVoteupCount());

            stmt.execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
