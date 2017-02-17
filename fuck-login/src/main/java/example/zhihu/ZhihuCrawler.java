package example.zhihu;

import com.google.common.util.concurrent.RateLimiter;
import config.Config;
import core.ZhihuLogin;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import util.HttpUtils;
import util.JsonUtils;

import java.util.*;

/**
 * 知乎爬虫, 爬取关注人的文章\回答, 并存放在Mysql数据库中.
 * Created by shenyuan on 17/2/16.
 *
 * 使用前需要做以下两个预备工作:
 * 1.修改Config类中的知乎账号配置以及数据库配置
 * 2.建立数据库以及表, 建表可参考resources下的zhihu.sql
 */
public class ZhihuCrawler {

    private ZhihuLogin zhihuLogin;
    private HttpContext httpContext;
    private Map<String, String> headers;
    public String myUrlToken;
    private double permitsPerSec = 3;

    public static void main(String[] args) throws Exception {

        //1.修改Config.zhihuAccount和Config.zhihuPassword
        ZhihuCrawler crawler = new ZhihuCrawler(Config.zhihuAccount, Config.zhihuPassword);

        //2.使用resources下的zhihu.sql创建answer表和article表, 以存放抓取数据
        //3.修改Config.mysqlUrl, Config.mysqlUser和Config.mysqlPassword, 以连接数据库
        ZhihuAnswerDao answerDao = new ZhihuAnswerDao();
        ZhihuArticleDao articleDao = new ZhihuArticleDao();

        if (crawler.login()) {
            List<String> followees = crawler.crawlFollowee(crawler.getMyUrlToken());
            for (String followee : followees) {
                List<ZhihuAnswerDO> answerDOs = crawler.crawlAnswer(followee, 0);
                for (ZhihuAnswerDO answerDO : answerDOs)
                    answerDao.save(answerDO);
                List<ZhihuArticleDO> articleDOs = crawler.crawlArticle(followee, 0);
                for (ZhihuArticleDO articleDO : articleDOs)
                    articleDao.save(articleDO);
            }
        } else {
            System.out.println("Fail to login!");
        }

    }

    public String getMyUrlToken() {
        return myUrlToken;
    }


    private double acquire() {
        RateLimiter rateLimiter = RateLimiter.create(permitsPerSec);
        return rateLimiter.acquire();
    }


    public ZhihuCrawler(String account, String password) {
        CookieStore cookieStore = new BasicCookieStore();
        httpContext = new BasicHttpContext();
        httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

        headers = new HashMap<String, String>();
        headers.put("Host", "www.zhihu.com");
        headers.put("Referer", "https://www.zhihu.com/");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:33.0) Gecko/20100101 Firefox/33.0");

        zhihuLogin = new ZhihuLogin(httpContext, this.headers);
    }

    public boolean login() {
        boolean result = zhihuLogin.login(Config.zhihuAccount, Config.zhihuPassword);
        if (result) {
            try {
                String profile = zhihuLogin.getProfile();
                Document doc = Jsoup.parse(profile);
                Element element = doc.select("#url_token").first();
                myUrlToken = element.attr("value");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return result;
    }

    private List<String> crawlFollowee(String user) {
        List<String> followees = new ArrayList<String>();
        String url = "https://www.zhihu.com/api/v4/members/"+ user +"/followees?include=data%5B*%5D.answer_count%2Carticles_count%2Cgender%2Cfollower_count%2Cis_followed%2Cis_following%2Cbadge%5B%3F(type%3Dbest_answerer)%5D.topics&offset=0&limit=20";
        while (true) {
            acquire();
            System.out.println("Current url: " + url);
            String resp = HttpUtils.get(url, headers, httpContext);
            System.out.println("Response: " + resp);
            Map respMap = JsonUtils.fromJson(resp, Map.class);
            List<Map> data = (List<Map>)respMap.get("data");
            Map paging = (Map)respMap.get("paging");
            boolean isEnd = (Boolean)paging.get("is_end");

            if (CollectionUtils.isNotEmpty(data)) {
                for (Map item : data) {
                    String urlToken = (String)item.get("url_token");
                    followees.add(urlToken);
                }
            }
            if (isEnd)
                break;
            url = (String)paging.get("next");
        }

        return followees;
    }

    private List<ZhihuArticleDO> crawlArticle(String user, long startTime) {
        List<ZhihuArticleDO> articles = new ArrayList<ZhihuArticleDO>();
        String url = "https://www.zhihu.com/api/v4/members/"+ user +"/articles?sort_by=created&include=data%5B%2A%5D.comment_count%2Ccollapsed_counts%2Creviewing_comments_count%2Ccan_comment%2Ccomment_permission%2Ccontent%2Cvoteup_count%2Ccreated%2Cupdated%2Cupvoted_followees%2Cvoting%3Bdata%5B%2A%5D.author.badge%5B%3F%28type%3Dbest_answerer%29%5D.topics&limit=20&offset=0";

        while (true) {
            acquire();
            System.out.println("Current url: " + url);
            String resp = HttpUtils.get(url, headers, httpContext);
            System.out.println("Response: " + resp);
            Map respMap = JsonUtils.fromJson(resp, Map.class);
            List<Map> data = (List<Map>)respMap.get("data");
            Map paging = (Map)respMap.get("paging");
            boolean isEnd = (Boolean)paging.get("is_end");

            if (CollectionUtils.isNotEmpty(data)) {
                for (Map item : data) {
                    Map author = (Map)item.get("author");

                    ZhihuArticleDO article = new ZhihuArticleDO();
                    article.setArticleId(((Number)item.get("id")).longValue());
                    article.setTitle((String)item.get("title"));
                    article.setAuthorName((String)author.get("name"));
                    article.setAuthorUrlToken((String)author.get("url_token"));
                    article.setExcerpt((String)item.get("excerpt"));
                    article.setContent((String)item.get("content"));
                    long createTime = ((Number)item.get("created")).longValue();
                    long updateTime = ((Number)item.get("updated")).longValue();
                    article.setCreateTime(new Date(createTime * 1000));
                    article.setUpdateTime(new Date(updateTime * 1000));
                    article.setVoteupCount(((Number)item.get("voteup_count")).intValue());

                    articles.add(article);
                    if (createTime * 1000 < startTime)
                        isEnd = true;
                }
            }

            if (isEnd)
                break;
            url = (String)paging.get("next");
        }

        return articles;
    }

    private List<ZhihuAnswerDO> crawlAnswer(String user, long startTime) {
        List<ZhihuAnswerDO> answers = new ArrayList<ZhihuAnswerDO>();
        String url = "https://www.zhihu.com/api/v4/members/"+ user +"/answers?include=data%5B*%5D.is_normal%2Csuggest_edit%2Ccomment_count%2Ccollapsed_counts%2Creviewing_comments_count%2Ccan_comment%2Ccontent%2Cvoteup_count%2Creshipment_settings%2Ccomment_permission%2Cmark_infos%2Ccreated_time%2Cupdated_time%2Crelationship.voting%2Cis_author%2Cis_thanked%2Cis_nothelp%2Cupvoted_followees%3Bdata%5B*%5D.author.badge%5B%3F(type%3Dbest_answerer)%5D.topics&sort_by=created&offset=0&limit=20";

        while (true) {
            acquire();
            System.out.println("Current url: " + url);
            String resp = HttpUtils.get(url, headers, httpContext);
            System.out.println("Response: " + resp);
            Map respMap = JsonUtils.fromJson(resp, Map.class);
            List<Map> data = (List<Map>)respMap.get("data");
            Map paging = (Map)respMap.get("paging");
            boolean isEnd = (Boolean)paging.get("is_end");

            if (CollectionUtils.isNotEmpty(data)) {
                for (Map item : data) {
                    Map question = (Map)item.get("question");
                    Map author = (Map)item.get("author");
                    ZhihuAnswerDO answerDO = new ZhihuAnswerDO();

                    answerDO.setAnswerId(((Number)item.get("id")).longValue());
                    answerDO.setQuestionId(((Number)question.get("id")).longValue());
                    answerDO.setAuthorName((String)author.get("name"));
                    answerDO.setAuthorUrlToken((String)author.get("url_token"));
                    answerDO.setQuestionTitle((String)question.get("title"));
                    answerDO.setExcerpt((String)item.get("excerpt"));
                    answerDO.setContent((String)item.get("content"));
                    long createTime = ((Number)item.get("created_time")).longValue();
                    long updateTime = ((Number)item.get("updated_time")).longValue();
                    answerDO.setCreateTime(new Date(createTime * 1000));
                    answerDO.setUpdateTime(new Date(updateTime * 1000));
                    answerDO.setVoteupCount(((Number)item.get("voteup_count")).intValue());
                    answers.add(answerDO);

                    if (createTime * 1000 < startTime)
                        isEnd = true;
                }
            }

            if (isEnd)
                break;
            url = (String)paging.get("next");
        }

        return answers;
    }
}
