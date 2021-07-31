package weibo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class MainGui extends JFrame {
    private static final long serialVersionUID = -8161981948004677531L;
    int DEFAULT_WIDTH = 300;
    int DEFAULT_HEIGHT = 200;
    private JLabel label;
    private JComboBox<String> faceCombo;
    private static TextField tf;
    private static JButton filePathButton;
    private static DateChooser startTimeChooser;
    private static DateChooser endTimeChooser;
    private static JLabel endShowDate;
    private static JLabel startShowDate;
    private static Button button;
    private static String type;
    private static String name;
    private static String startTime ;
    private static String endTime;
    private static String filePath;

    public MainGui() {
        this.setTitle("微博图片下载");
        this.setSize(this.DEFAULT_WIDTH, this.DEFAULT_HEIGHT);
        this.setLayout(new GridLayout(6, 2,3,3));
        // 第一行内容
        label = new JLabel("账号类型:");
        label.setSize(200,10000);

        faceCombo = new JComboBox<>();
        faceCombo.setEditable(false);
        faceCombo.setEnabled(true);
        // faceCombo.addItem("用户昵称");
        // faceCombo.addItem("用户名");
        faceCombo.addItem("用户ID");
        this.add(label);
        this.add(faceCombo);
        label.setHorizontalAlignment(SwingConstants.RIGHT);

        // 第二行内容
        JLabel label2 = new JLabel("昵称");
        label2.setHorizontalAlignment(SwingConstants.RIGHT);
        tf = new TextField(20);
        this.add(label2);
        this.add(tf);

        // 第三行内容
        JLabel label3 = new JLabel("开始时间");
        label3.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(label3);

        startTimeChooser = DateChooser.getInstance("yyyy-MM-dd");
        startShowDate = new JLabel("单击选择日期");
        startTimeChooser.register(startShowDate);
        this.add(startShowDate);

        // 第四行内容
        JLabel label4 = new JLabel("结束时间");
        label4.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(label4);

        endTimeChooser = DateChooser.getInstance("yyyy-MM-dd");
        endShowDate = new JLabel("单击选择日期");
        endTimeChooser.register(endShowDate);
        this.add(endShowDate);

        button = new Button("Start");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                type = (String)faceCombo.getSelectedItem();
                name = tf.getText();
                System.out.println(startShowDate.getText());
                System.out.println(endShowDate.getText());
                if (startShowDate.getText() != "单击选择日期") {
                    startTime = startTimeChooser.getStrDate();
                }
                if (endShowDate.getText() != "单击选择日期") {
                    endTime = endTimeChooser.getStrDate();
                }
                if ((startTime == null && endTime != null) || (startTime != null && endTime == null)) {
                    JOptionPane.showMessageDialog(null, "输入完整的起止时间或不输入时间","错误", JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    if (startTime != null && endTime != null) {
                        WeiboUtils.needFilterDate = true;
                        WeiboUtils.startTime = startTime;
                        WeiboUtils.endTime = endTime;
                    }
                }
                filePath = filePathButton.getText();
                if (type == null || "".equals(type) || name == null || "".equals(name)
                        || filePath == null || "".equals(filePath) || "选择".equals(filePath)) {
                    JOptionPane.showMessageDialog(null, "请输入正确的名称,以及图片保存地址","错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                System.out.println("type is " + type);
                System.out.println("name is " + name);
                System.out.println("startTime is " + startTime);
                System.out.println("end is " + endTime);
                System.out.println("filePath is " + filePath);
                Thread th = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            button.setEnabled(false);
                            WeiboDownloader.downloadCli(type, name, filePath, startTime, endTime);
                        } catch (Exception err) {
                            button.setEnabled(true);
                            System.out.println("出错了。 详细错误信息： ");
                            err.printStackTrace();
                        } finally {
                            button.setEnabled(true);
                            WeiboUtils.needFilterDate = false;
                            WeiboUtils.startTime = null;
                            WeiboUtils.endTime = null;
                        }
                    }
                });
                th.start();
            }
        });
        // 第五行内容
        JLabel label5 = new JLabel("文件保存地址");
        label5.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(label5);
        filePathButton = new JButton("选择");
        filePathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc=new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY );
                jfc.showDialog(new JLabel(), "选择");
                if (jfc.getSelectedFile() != null) {
                    filePathButton.setText(jfc.getSelectedFile().getAbsolutePath());
                }
            }
        });
        this.add(filePathButton);
        this.add(button);

    }
    public static void main(String[] args){
        MainGui mainGui = new MainGui();
        mainGui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainGui.setLocationRelativeTo(null);
        mainGui.setVisible(true);
    }
}
