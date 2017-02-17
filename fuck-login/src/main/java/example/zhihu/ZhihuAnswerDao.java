package example.zhihu;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

/**
 * 实现数据库answer表的操作
 * Created by shenyuan on 17/2/16.
 */
public class ZhihuAnswerDao extends BaseDao {

    public ZhihuAnswerDao(String url, String user, String password) {
        super(url, user, password);
    }

    public void save(ZhihuAnswerDO answerDO) {
        Connection conn = getConnection();
        PreparedStatement stmt = null;

        try {
            //stmt = conn.createStatement();
            String sql = "insert into zhihu_answer(answer_id, question_id, author_name, author_url_token, question_title, " +
                    "excerpt, content, create_time, update_time, voteup_count) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, answerDO.getAnswerId());
            stmt.setLong(2, answerDO.getQuestionId());
            stmt.setString(3, answerDO.getAuthorName());
            stmt.setString(4, answerDO.getAuthorUrlToken());
            stmt.setString(5, answerDO.getQuestionTitle());
            stmt.setString(6, answerDO.getExcerpt());
            stmt.setString(7, answerDO.getContent());

            stmt.setTimestamp(8, new Timestamp(answerDO.getCreateTime().getTime()));
            stmt.setTimestamp(9, new Timestamp(answerDO.getUpdateTime().getTime()));
            stmt.setInt(10, answerDO.getVoteupCount());

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
