package weibo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
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
public class WeiboDownloader {
	/*路径*/
	private static String IMG_LOCATION = "E:\\img\\";
	
	List<String> urls = new ArrayList<String>();
	int getImgURL(String containerid,int page) throws ParseException, IOException{
		String url = "https://m.weibo.cn/api/container/getIndex?count=25&page="+page+"&containerid="+containerid;
		HttpClient httpClient = getHttpClient();
		HttpGet get = new HttpGet(url);
		get.setHeader("User-Agent", USER_AGENT);
		HttpResponse response = httpClient.execute(get);
		String ret = EntityUtils.toString(response.getEntity(), "utf-8");
		JsonObject root = new JsonParser().parse(ret).getAsJsonObject();
		JsonArray array = root.getAsJsonArray("cards");
		for(int i=0;i<array.size();i++){
			JsonObject mblog = array.get(i).getAsJsonObject().getAsJsonObject("mblog");
			if(mblog!=null){
				JsonArray pics = mblog.getAsJsonArray("pics");
				if(pics!=null){
					for(int j=0;j<pics.size();j++){
						JsonObject o = pics.get(j).getAsJsonObject();
						JsonObject large = o.getAsJsonObject("large");
						if(large!=null){
							urls.add(large.get("url").getAsString());
						}
					}
				}
			}
		}
		return array.size();
	}
	
	public static void main(String[] args) throws ParseException, IOException, InterruptedException {
		Scanner scanner = new Scanner(System.in);
		System.out.println("请输入图片要保存的地址");
		IMG_LOCATION = scanner.nextLine();
		if(!IMG_LOCATION.endsWith("/")&&!IMG_LOCATION.endsWith("\\")){
			if(IMG_LOCATION.contains("/"))
				IMG_LOCATION = IMG_LOCATION + "/";
			else
				IMG_LOCATION = IMG_LOCATION + "\\";
		}
		System.out.println("请输入要下载的账号名称:");
		System.out.println("1代表用户ID");
		System.out.println("2代表用户名");
		System.out.println("3代表用户昵称(建议)");
		int type = scanner.nextInt();
		String containerId = "";
		if(type==1){
			System.out.println("输入用户ID");
			String uid = scanner.next();
			containerId = uidToContainerId(uid);
		}else if(type==2){
			System.out.println("输入用户名");
			String name = scanner.next();
			containerId = usernameToContainerId(name);
		}else if(type==3){
			System.out.println("输入用户昵称");
			String nickname = scanner.next();
			containerId = nicknameToContainerId(nickname);
		}
		WeiboDownloader weiboDownloader = new WeiboDownloader();
		int i = 1;
		while(weiboDownloader.getImgURL(containerId,i)>0){
			System.out.println("分析微博中: "+i);
			i++;
			Thread.sleep(1000);
		}
		System.out.println("分析完毕");
		System.out.println("图片数量: " + weiboDownloader.urls.size());
		List<String> list = weiboDownloader.urls;
		if(!new File(IMG_LOCATION).exists()){
			try{
				new File(IMG_LOCATION).mkdirs();
			}catch (Exception e) {
				System.out.println("无法创建目录,请手动创建");
			}
		}
		for(i=0;i<list.size();i++){
			System.out.println("下载图片: "+i);
			byte[] imgbytes = download(list.get(i),10000);
			FileUtils.byte2File(imgbytes,IMG_LOCATION + containerId.substring(6), i+1+".jpg");
		}
		System.out.println("图片下载完成, 路径是 " + IMG_LOCATION + containerId.substring(6));
		scanner.close();
	}
	
	/**最大图片大小40M*/
	private static final long MAX_DOWNLOAD_SIZE = 40L * 1024 * 1024;
	/**UA*/
	static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36";
	/**
	 * uidתcontianerId
	 * @author yanximin
	 * */
	static String uidToContainerId(String uid){
		if(uid==null)
			throw new IllegalArgumentException("uid is null");
		return 107603+uid;
	}
	/**
	 * 昵称转ContainerId
	 * @author yanximin
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * */
	static String nicknameToContainerId(String nickname) throws ClientProtocolException, IOException{
		String url = "http://m.weibo.com/n/"+nickname;
		HttpClient httpClient = HttpClients.createDefault();
		HttpPost post = new HttpPost(url);
		post.setHeader("User-Agent", USER_AGENT);
		HttpResponse response = httpClient.execute(post);
		post.abort();
		if(response.getStatusLine().getStatusCode()==302){
			String cid = response.getLastHeader("Location").getValue().substring(27);
			return "107603" + cid;
		}
		return null;
	}
	
	/**
	 * 用户名转contianerId
	 * @author yanximin
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * */
	static String usernameToContainerId(String name) throws ClientProtocolException, IOException{
		String url = "https://weibo.cn/"+name;
		HttpClient httpClient = HttpClients.createDefault();
		HttpGet get = new HttpGet(url);
		get.setHeader("User-Agent", USER_AGENT);
		HttpResponse response = httpClient.execute(get);
		String ret = EntityUtils.toString(response.getEntity(), "utf-8");
		Pattern pattern = Pattern.compile("href=\"/([\\d]*?)/info\"");
		Matcher matcher = pattern.matcher(ret);
		while(matcher.find()){
			return "107603" + matcher.group(1);
		}
		return null;
	}
	/**
	 * 下载指定文件到内存
	 * @param webUrl
	 * @return byte
	 * 
	 *
	 */
	private static byte[] download(String webUrl, int timeOut) {
		HttpURLConnection connection = null;
		long start = System.currentTimeMillis();
		try {
			URL url = new URL(webUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(timeOut);
			connection.setReadTimeout(timeOut);
			connection.setRequestProperty("User-Agent", USER_AGENT);
			int len = connection.getContentLength();
			if (len >= MAX_DOWNLOAD_SIZE) {
				return null;
			}
			if (len == -1) {
				try (InputStream in = connection.getInputStream()) {
					return IOUtils.toByteArray(connection.getInputStream());
				}
			} else {
				byte[] data = new byte[len];
				byte[] buffer = new byte[4096 * 2];
				int count = 0, sum = 0;
				try (InputStream in = connection.getInputStream()) {
					while ((count = in.read(buffer)) > 0) {
						long elapse = System.currentTimeMillis() - start;
						if (elapse >= timeOut) {
							data = null;
							break;
						}
						System.arraycopy(buffer, 0, data, sum, count);
						sum += count;
					}
				}
				return data;
			}
		} catch (Exception e) {
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	/**
	 * 初始HttpClient
	 * @author yanximin
	 * */
	public static HttpClient getHttpClient() {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000);
		httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 30000);
		return httpClient;
	}
}
