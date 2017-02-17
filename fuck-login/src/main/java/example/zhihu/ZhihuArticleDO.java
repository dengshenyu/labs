package example.zhihu;

import java.util.Date;

/**
 * Created by shenyuan on 17/2/16.
 */
public class ZhihuArticleDO {
    private long articleId;
    private String title;
    private String authorName;
    private String authorUrlToken;
    private String excerpt;
    private String content;
    private int voteupCount;
    private Date createTime;
    private Date updateTime;

    public long getArticleId() {
        return articleId;
    }

    public void setArticleId(long articleId) {
        this.articleId = articleId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorUrlToken() {
        return authorUrlToken;
    }

    public void setAuthorUrlToken(String authorUrlToken) {
        this.authorUrlToken = authorUrlToken;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getVoteupCount() {
        return voteupCount;
    }

    public void setVoteupCount(int voteupCount) {
        this.voteupCount = voteupCount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
