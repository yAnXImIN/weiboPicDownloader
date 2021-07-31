package weibo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.ParseException;

public class WeiboDownloader {
	public static final int TYPE_USER_ID = 1;
	public static final int TYPE_USER_NAME = 2;
	public static final int TYPE_USER_NICKNAME = 3;

	/**
	 * UA
	 * */
	static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36";
	
	/**
	 * 路径
	 * */
	public static String IMG_LOCATION = "E:\\img\\";

	public static void downloadCli(String selectType, String inputName, String filePath, String startTime, String endTime) throws ParseException, IOException, InterruptedException {
		IMG_LOCATION = filePath;
        Map<String, Integer> map = new HashMap<>();
		// 目前失效 先注释
		// map.put("用户昵称", TYPE_USER_NICKNAME);
        // map.put("用户名", TYPE_USER_NAME);
        map.put("用户ID", TYPE_USER_ID);
		int type = map.get(selectType);
		String containerId = "";
		if(type == TYPE_USER_ID){
			String uid = inputName;
			containerId = WeiboUtils.uidToContainerId(uid);
		}else if(type == TYPE_USER_NAME){
			String name = inputName;
			containerId = WeiboUtils.usernameToContainerId(name);
		}else if(type == TYPE_USER_NICKNAME){
			String nickname = inputName;
			containerId = WeiboUtils.nicknameToContainerId(nickname);
		}
        if (containerId == null) {
            System.out.println("未找到用户, 请检查账户名");
            return;
        }
		List<String> imgUrls;
		try {
			imgUrls = WeiboUtils.getAllImgURL(containerId);
		} catch (Exception e1) {
			e1.printStackTrace();
			System.out.println("解析出现异常， 请稍候再试！");
			return;
		}
		System.out.println("分析完毕");
		System.out.println("图片数量: " + imgUrls.size());
		
		if(!IMG_LOCATION.endsWith("/")&&!IMG_LOCATION.endsWith("\\")){
			if(IMG_LOCATION.contains("/"))
				IMG_LOCATION = IMG_LOCATION + "/" + containerId.substring(6) + "/";
			else
				IMG_LOCATION = IMG_LOCATION + "\\" +  containerId.substring(6) + "\\";
		}
		
		if(!new File(IMG_LOCATION).exists()){
			try{
				new File(IMG_LOCATION).mkdirs();
				System.out.println("创建 " + IMG_LOCATION + "成功");
			}catch (Exception e) {
				System.out.println("无法创建目录,请手动创建");
			}
		}
		CountDownLatch downLatch = new CountDownLatch(imgUrls.size());
		ExecutorService executor = Executors.newFixedThreadPool(4);
		for(int i=0;i<imgUrls.size();i++){
			executor.submit(new ImageDownloadTask(downLatch, i, imgUrls.get(i)));
		}
		
		downLatch.await();
		System.out.println("图片下载完成, 路径是 " + IMG_LOCATION);
		executor.shutdown();
	}
	
	
}
