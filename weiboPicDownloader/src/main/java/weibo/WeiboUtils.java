package weibo;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SuppressWarnings("deprecation")
public class WeiboUtils {
    public static boolean needFilterDate = false;
    public static String startTime;
    public static String endTime;
    /** UA */
    static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36";

    /**
     * uidתcontianerId
     * 
     * @author yanximin
     */
    static String uidToContainerId(String uid) {
        if (uid == null)
            throw new IllegalArgumentException("uid is null");
        return 107603 + uid;
    }

    /**
     * 昵称转ContainerId
     * 
     * @author yanximin
     * @throws IOException
     * @throws ClientProtocolException
     */
    static String nicknameToContainerId(String nickname) throws ClientProtocolException, IOException {
        String url = "http://m.weibo.com/n/" + nickname;
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);
        post.setHeader("User-Agent", USER_AGENT);
        HttpResponse response = httpClient.execute(post);
        post.abort();
        if (response.getStatusLine().getStatusCode() == 302) {
            String cid = response.getLastHeader("Location").getValue().substring(27);
            return "107603" + cid;
        }
        return null;
    }

    /**
     * 用户名转contianerId
     * 
     * @author yanximin
     * @throws IOException
     * @throws ClientProtocolException
     */
    static String usernameToContainerId(String name) throws ClientProtocolException, IOException {
        String url = "https://weibo.cn/" + name;
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet get = new HttpGet(url);
        get.setHeader("User-Agent", USER_AGENT);
        HttpResponse response = httpClient.execute(get);
        String ret = EntityUtils.toString(response.getEntity(), "utf-8");
        Pattern pattern = Pattern.compile("href=\"/([\\d]*?)/info\"");
        Matcher matcher = pattern.matcher(ret);
        while (matcher.find()) {
            return "107603" + matcher.group(1);
        }
        return null;
    }

    public static List<String> getAllImgURL(String containerid) throws Exception {
        List<String> urls = new ArrayList<String>();
        int i = 1;
        while (getImgURL(containerid, i, urls) > 0) {
            System.out.println("分析微博中: " + i);
            i++;
            // 防封，分析一次页面休息+1S
            Thread.sleep(1000);
        }
        return urls;
    }

    private static int getImgURL(String containerid, int page, List<String> urls) throws ParseException, IOException {
        String url = "https://m.weibo.cn/api/container/getIndex?count=25&page=" + page + "&containerid=" + containerid;
        System.out.println(url);
        HttpClient httpClient = getHttpClient();
        HttpGet get = new HttpGet(url);
        get.setHeader("User-Agent", USER_AGENT);
        HttpResponse response = httpClient.execute(get);
        String ret = EntityUtils.toString(response.getEntity(), "utf-8");
        JsonObject root;
        try {
            // 防封
            root = new JsonParser().parse(ret).getAsJsonObject();
        } catch (Exception e) {
            try {
                Thread.sleep(60000);
            } catch (Exception e1) {

            }
            return 1;
        }
        JsonObject asJsonObject = root.getAsJsonObject("data");
        JsonArray array = asJsonObject.getAsJsonArray("cards");
        for (int i = 0; i < array.size(); i++) {
            JsonObject mblog = array.get(i).getAsJsonObject().getAsJsonObject("mblog");
            if (mblog != null) {
                if (WeiboUtils.needFilterDate) {
                    String createAt = mblog.get("created_at").getAsString();
                    String date = WeiboUtils.getDate(createAt);
                    if (date.compareTo(WeiboUtils.startTime) < 0 || date.compareTo(WeiboUtils.endTime) > 0){
                        continue;
                    }
                }
                JsonArray pics = mblog.getAsJsonArray("pics");
                if (pics != null) {
                    for (int j = 0; j < pics.size(); j++) {
                        JsonObject o = pics.get(j).getAsJsonObject();
                        JsonObject large = o.getAsJsonObject("large");
                        if (large != null) {
                            urls.add(large.get("url").getAsString());
                        }
                    }
                }
            }
        }
        return array.size();
    }

    /**
     * 初始HttpClient
     * 
     * @author yanximin
     */
    public static HttpClient getHttpClient() {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000);
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 30000);
        return httpClient;
    }

    public static long transWeiboDateStrToTimeStamp(String weiboDateStr) {
        if (weiboDateStr == null || "".equals(weiboDateStr)) {
            return  0;
        }
        if (weiboDateStr.contains("秒前")) {
            weiboDateStr = weiboDateStr.replace("秒前", "");
            int second = Integer.valueOf(weiboDateStr);
            return  System.currentTimeMillis() - second * 1000;
        }
        if (weiboDateStr.contains("分钟前")) {
            weiboDateStr = weiboDateStr.replace("分钟前", "");
            int second = Integer.valueOf(weiboDateStr);
            return  System.currentTimeMillis() - second * 1000 * 60;
        }
        if (weiboDateStr.contains("小时前")) {
            weiboDateStr = weiboDateStr.replace("小时前", "");
            int second = Integer.valueOf(weiboDateStr);
            return  System.currentTimeMillis() - second * 1000 * 3600;
        }
        if (weiboDateStr.contains("昨天")) {
            Date yesterdayTimestamp = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
            String yesterday = simpleDateFormat.format(yesterdayTimestamp);
            weiboDateStr = weiboDateStr.replace("昨天", yesterday);
            SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm");
            try {
                Date date = sDateFormat.parse(weiboDateStr);
                return date.getTime();
            } catch (Exception e) {
                return  0;
            }
        }
        if (weiboDateStr.contains("-")) {
            if (!weiboDateStr.startsWith("20")) {
                int year = new Date().getYear() + 1900;
                weiboDateStr = year + "-" + weiboDateStr;
            }
            SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date date = sDateFormat.parse(weiboDateStr);
                return date.getTime();
            } catch (Exception e) {
                return  0;
            }
        }
        return 0;
    }
    public static String getDate(String weiboDateStr) {
        long time;
        try {
            time = WeiboUtils.transWeiboDateStrToTimeStamp(weiboDateStr);
        } catch (Exception e) {
            time = 0;
        }

        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return sDateFormat.format(new Date(time));
    }
}
