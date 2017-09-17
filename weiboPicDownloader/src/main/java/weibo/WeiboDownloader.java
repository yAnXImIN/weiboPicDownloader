package weibo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.ParseException;

public class WeiboDownloader {
	
	/**
	 * UA
	 * */
	static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36";
	
	/**
	 * 路径
	 * */
	public static String IMG_LOCATION = "E:\\img\\";
	
	public static void main(String[] args) throws ParseException, IOException, InterruptedException {
		Scanner scanner = new Scanner(System.in);
		System.out.println("请输入图片要保存的地址");
		IMG_LOCATION = scanner.nextLine();
		
		System.out.println("请输入要下载的账号名称:");
		System.out.println("1代表用户ID");
		System.out.println("2代表用户名");
		System.out.println("3代表用户昵称(建议)");
		int type = scanner.nextInt();
		String containerId = "";
		if(type==1){
			System.out.println("输入用户ID");
			String uid = scanner.next().trim();
			containerId = WeiboUtils.uidToContainerId(uid);
		}else if(type==2){
			System.out.println("输入用户名");
			String name = scanner.next().trim();
			containerId = WeiboUtils.usernameToContainerId(name);
		}else if(type==3){
			System.out.println("输入用户昵称");
			String nickname = scanner.next().trim();
			containerId = WeiboUtils.nicknameToContainerId(nickname);
		}
		
		//关闭输入
		scanner.close();
		
		List<String> imgUrls = null;
		try {
			imgUrls = WeiboUtils.getAllImgURL(containerId);
		} catch (Exception e1) {
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
		System.out.println("图片下载完成, 路径是 " + IMG_LOCATION + containerId.substring(6));
		executor.shutdown();
	}
	
	
}
