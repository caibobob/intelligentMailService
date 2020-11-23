package mailservice.caibc.aws;


import java.io.IOException;
import java.net.URLDecoder;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;


import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

public class S3EventProcessorMail implements RequestHandler<S3Event, String>
{

    private static final String TYPE_logistics = "logistics";
    private static final String TYPE_invoice = "invoice";

    private static final double STANDARDSCORE = 0.7;//配置认可的自信度值

    private static final String BUCKET_NAME_MailAddress = "";//需要输入邮件所在桶

    private static final String MANUAL_SERVICE_MAIL_ADDRESS = "";//人工处理收件箱

    private static final String AI_CONTENT_logistics = "Thanks for your mail---logistics---This is a AI Request for logistics.";
    private static final String AI_CONTENT_invoice = "Thanks for your mail---invoice---This is a AI Request for invoice.";
    private static final String MANUAL_TRANSFER_CONTENT = "This mail should be processed manually.";


    private static String myEmailAccount = "";//邮箱帐号需要输入
    private static String myEmailPassword = "";//邮箱需要输入
    private static String myEmailSMTPHost = "";//邮箱SMTP服务器地址需要输入



    public String handleRequest(S3Event s3event, Context context) {

        try {
            S3EventNotificationRecord record = s3event.getRecords().get(0);

            String srcBucket = record.getS3().getBucket().getName();
            // Object key may have spaces or unicode non-ASCII characters.
            String srcKey = record.getS3().getObject().getKey().replace('+', ' ');
            srcKey = URLDecoder.decode(srcKey, "UTF-8");
            // Download the image from S3 into a stream
            AmazonS3 s3Client = new AmazonS3Client();
            // Read the source file as text
            String strBody = s3Client.getObjectAsString(srcBucket, srcKey);
            System.out.println("Body: " + strBody);

            System.out.println("srcKey: " + srcKey);
            String[] strArr = srcKey.split("\\.");

            String newKeyMailAddress = strArr[0] + ".txt";
            System.out.println("newKeyMailAddress: " + newKeyMailAddress);

            AmazonS3 s3ClientMail = new AmazonS3Client();
            // Read the source file as text
            String bodyMailAddress = s3ClientMail.getObjectAsString(BUCKET_NAME_MailAddress, newKeyMailAddress);
            System.out.println("Body Mail Address: " + bodyMailAddress);

            // bodyMailAddress = "f ";
            // strBody = "{\"File\": \"logistictestdata.csv\", \"Line\": \"0\", \"Classes\": [{\"Name\": \"logistics\", \"Score\": 0.5985}, {\"Name\": \"invoice\", \"Score\": 0.4015}]}";
            JSONObject jsonObject = JSON.parseObject(strBody);
            JSONArray array = jsonObject.getJSONArray("Classes");

            double maxscore = 0.0;
            String maxscoreName = "";
            for (int i = 0; i < array.size(); i++)
            {
                JSONObject o = array.getJSONObject(i);
                double score = Double.parseDouble(o.get("Score").toString());
                if (maxscore < score)
                {
                    maxscore = score;
                    maxscoreName = o.get("Name").toString();
                }
            }

            String dstMailAddress = "";
            String dstContent = "";

            if (maxscore >= STANDARDSCORE)
            {
                dstMailAddress = bodyMailAddress;
                if (maxscoreName.equals(TYPE_logistics) )
                {
                    dstContent = AI_CONTENT_logistics + "----" + strBody;
                }
                else if (maxscoreName.equals(TYPE_invoice))
                {
                    dstContent = AI_CONTENT_invoice + "----" + strBody;
                }
            }
            else
            {
                //send mail to manual service
                dstMailAddress = MANUAL_SERVICE_MAIL_ADDRESS;
                dstContent = MANUAL_TRANSFER_CONTENT + "----" + strBody;
            }


            Properties props = new Properties();
            props.setProperty("mail.transport.protocol", "smtp");
            props.setProperty("mail.smtp.host", myEmailSMTPHost);
            props.setProperty("mail.smtp.auth", "true");

            String smtpPort = "465";
            props.setProperty("mail.smtp.port", smtpPort);
            props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.smtp.socketFactory.fallback", "false");
            props.setProperty("mail.smtp.socketFactory.port", smtpPort);


            Session session = Session.getDefaultInstance(props);
            session.setDebug(true);

            // 3. 创建一封邮件
            // 3.1. 创建一封邮件
            MimeMessage message = new MimeMessage(session);
            // 3.2. From: 发件人（昵称有广告嫌疑，避免被邮件服务器误认为是滥发广告以至返回失败，请修改昵称）
            message.setFrom(new InternetAddress(myEmailAccount, "AICS007", "UTF-8"));
            // 3.3. To: 收件人（可以增加多个收件人、抄送、密送）
            message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(dstMailAddress, "dear", "UTF-8"));
            // 3.4. Subject: 邮件主题（标题有广告嫌疑，避免被邮件服务器误认为是滥发广告以至返回失败，请修改标题）
            message.setSubject("[auto reply By AICS007]", "UTF-8");
            // 3.5. Content: 邮件正文（可以使用html标签）（内容有广告嫌疑，避免被邮件服务器误认为是滥发广告以至返回失败，请修改发送内容）
            message.setContent(dstContent, "text/html;charset=UTF-8");
            // 3.6. 设置发件时间
            message.setSentDate(new Date());
            // 3.7. 保存设置
            message.saveChanges();






            // 4. 根据 Session 获取邮件传输对象
            Transport transport = session.getTransport();
            //  5. 使用 邮箱账号 和 密码 连接邮件服务器
            transport.connect(myEmailAccount, myEmailPassword);
            // 6. 发送邮件, 发到所有的收件地址, message.getAllRecipients() 获取到的是在创建邮件对象时添加的所有收件人, 抄送人, 密送人
            transport.sendMessage(message, message.getAllRecipients());
            // 7. 关闭连接
            transport.close();



            return "OK--OK";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


}
