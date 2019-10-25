package weibo;

import java.util.concurrent.CountDownLatch;

public class ImageDownloadTask implements Runnable{
    private CountDownLatch downLatch;
    private int imageIndex;
    private String imageUrl;
    
    public int getImageIndex() {
        return imageIndex;
    }
    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public ImageDownloadTask(CountDownLatch downLatch, int imageIndex, String imageUrl) {
        super();
        this.downLatch = downLatch;
        this.imageIndex = imageIndex;
        this.imageUrl = imageUrl;
    }

    @Override
    public void run() {
        try{
            System.out.println("下载图片: " + ( imageIndex + 1));
            byte[] imgBytes = FileUtils.download(imageUrl, 100_000);
            FileUtils.byte2File(imgBytes, WeiboDownloader.IMG_LOCATION, imageIndex+1+getSuffix(imageUrl));
        }catch (Exception e) {
        }finally {
            downLatch.countDown();
        }
    }
    private String getSuffix(String url){
        if(!url.substring(url.lastIndexOf("/")).contains(".")){
            return ".jpg";
        }
        try{
            return url.substring(url.lastIndexOf("."));
        }catch(Exception e){
            e.printStackTrace();
        }
        return ".jpg";
    }
}
