package mailservice.caibc.aws;


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import java.util.Date;
import java.util.Properties;

public class MailCollector {


    private String bucketNameMailAddress = "mailsupport-1-mailaddress";
    private String bucketNameContent = "mailsupport-1";

    private String clientRegion = "us-west-2";

    public static void main(String[] args) {

        MailCollector mc = new MailCollector();
        mc.loadMailIntoS3();

    }

    private IMAPStore initMailIMAPStore() {
        IMAPStore store = null;
        try {
            String user = "";// your mail box account
            String password = ""; // your mail box password

            Properties prop = System.getProperties();
            prop.put("mail.store.protocol", "imap");
            prop.put("mail.imap.host", "imap.qq.com");
            store = (IMAPStore) Session.getInstance(prop).getStore("imap"); // 使用imap会话机制，连接服务器
            store.connect(user, password);
        } catch (Exception e) {
            System.out.println("Exception in method initMailIMAPStore");
            e.printStackTrace();
        }
        return store;

    }

    private AmazonS3 initS3Client() {
        /**
         * before start ,you need a AK/SK which has s3 access right
         */
        String AWS_ACCESS_KEY = ""; // 【Your AWS access_key】
        String AWS_SECRET_KEY = ""; // 【Your AWS secret_key】

        /**
         * initial S3 client.
         */
        BasicAWSCredentials s3Cre = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY);
        return AmazonS3ClientBuilder.standard()
                .withRegion(clientRegion)
                .withCredentials(new AWSStaticCredentialsProvider(s3Cre))
                .build();


    }

    private void loadMailIntoS3() {

        AmazonS3 s3Client = this.initS3Client();

        IMAPStore store = this.initMailIMAPStore();

        IMAPFolder folder = null;
        try {
            folder = (IMAPFolder) store.getFolder("INBOX"); // Read mail from INBOX
            folder.open(Folder.READ_WRITE);
            Message[] messages = folder.getMessages();
            System.out.println("Number of mail List : " + messages.length);

            for (int i = 0; i < messages.length; i++) {
                System.out.println("The processing mail index is " + i);
                Message msg = messages[i];
                Flags flags = msg.getFlags();
                /**
                 * Only handle new Email
                 */
                if (flags.contains(Flags.Flag.SEEN)) {
                    System.out.println("Old mail number: " + i);
                } else {
                    System.out.println("New Coming mail number: " + i);
                    String subject = msg.getSubject();
                    System.out.println("the mail subject: " + subject);

                    String content = "";
                    if (msg.isMimeType("text/plain")) {
                        content = (String) msg.getContent();
                        System.out.println("mail content text: " + content);
                    }
                    if (msg.isMimeType("multipart/alternative")) {

                        Multipart mp = (Multipart) msg.getContent();
                        int bodynum = mp.getCount();
                        for (int j = 0; j < bodynum; j++) {
                            if (mp.getBodyPart(j).isMimeType("text/html")) {
                                content = (String) mp.getBodyPart(j).getContent();
                                System.out.println("mail content multi-part: " + content);
                            }
                        }
                    }


                    String jobContent = jobContentBuild(subject, content);

                    String from = "";
                    Address[] froms = msg.getFrom();
                    if (froms.length > 0) {
                        // get mail from
                        InternetAddress address = (InternetAddress) froms[0];
                        from = address.getAddress();
                        System.out.println("Mail From is Not Empty :" + from);

                    }
                    /**
                     * use timestamp as S3 full Object Name to avoid conflict
                     * each mail per Object just for demo
                     */
                    String t = String.valueOf(new Date().getTime());
                    String jobContentKey = t + "/" + t + ".csv";
                    String jobAddressKey = t + "/" + t + ".txt";

                    s3Client.putObject(bucketNameContent, jobContentKey, jobContent);
                    System.out.println("S3 content upload: " + bucketNameContent + jobContentKey + ":" + jobContent);

                    s3Client.putObject(bucketNameMailAddress, jobAddressKey, from);
                    System.out.println("S3 mail address upload: " + bucketNameMailAddress + jobAddressKey + ":" + from);


                    msg.setFlag(Flags.Flag.SEEN, true);

                }
            }
            /**
             * release S3Client resource
             */
            s3Client.shutdown();
        } catch (Exception e) {
            System.out.println("Collect mail Exception: " + e.getMessage());
        } finally {
            if (null != folder) {
                try {
                    folder.close(false);// Close mail folder
                } catch (Exception ex) {
                    System.out.println("Failed: folder close: " + ex.getMessage());
                }
            }

            if (null != store) {
                try {
                    store.close();// close mail store
                } catch (Exception ex) {
                    System.out.println("Failed: store close: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * You may need to format mail content in production env,need to remove ',' and html code in content
     * @param subject
     * @param content
     * @return
     */
    private String jobContentBuild(String subject, String content) {
        String str = subject + " " + content;
        //For production env you need to reformat mail content remove ',' and html code
        return str;
    }


}
