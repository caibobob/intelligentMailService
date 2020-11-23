# intelligentMailService
intelligent mail service基于AWS comprehend NLP服务，通过对邮件内容进行自定义标签，训练模型，再利用模型对日常邮件内容进行分类及后续处理
程序的主入口是 MailCollector：
需要在MailCollector采用Java Mail API收取邮件，并生成CSV文件上传到S3中，需要配置收件邮箱的帐号信息：


            String user = "";// your mail box account
            String password = ""; // your mail box password
            
            prop.put("mail.store.protocol", "imap");
            prop.put("mail.imap.host", "imap.qq.com");配置邮箱服务器地址，Sample使用腾讯邮箱进行调试通过。

配置S3文件保存bucket name，AWS bucket name在全球唯一，需要按具体创建          
            bucketNameMailAddress = "mailsupport-1-mailaddress";
            bucketNameContent = "mailsupport-1";
    
    
配置AWS访问的AKSK
        String AWS_ACCESS_KEY = ""; // 【Your AWS access_key】
        String AWS_SECRET_KEY = ""; // 【Your AWS secret_key】
        
        
由于不同的邮件服务器会在邮件正文插入HTML内容，为提高识别率，需要将邮件内容格式化，可以在方案jobContentBuild(String subject, String content) 方法中进行处理。

程序comprehendPythonCall.py是comprehend 服务调用的lambda程序，邮件内容写入S3后，通过event触发程序运行，需要配置训练好完成的模型地址：

            DocumentClassifierArn='arn:aws:comprehend:us-west-2:account-id:document-classifier/SAIHE',
            配置结果保存的S3 Bucket 名称：
                'S3Uri': 's3://mailsupport-2/'+directoryName+'/'
            配置Lambda访问的角色
            DataAccessRoleArn='arn:aws:iam::account-id:role/service-role/AmazonComprehendServiceRole-comprehens3'
            
程序decompressPython.py是comprehend结果解压程序，需要配置解压后的S3 bucket 名称
            s3upload.meta.client.upload_file('/tmp/predictions.jsonl','result-bucket',filename)
            
程序S3EventProcessorMail是邮件回复程序，读取comprehend结果后，对相关标签邮件进行回复：
程序demo采用了‘logistics','invoice'两个标签，可以根据实际业务进行调整
    private static final String TYPE_logistics = "logistics";
    private static final String TYPE_invoice = "invoice";

根据需要调整comprehend中自信度的分数
    private static final double STANDARDSCORE = 0.7;//配置认可的自信度值

配置邮件所在的bucket名称
    private static final String BUCKET_NAME_MailAddress = "";//需要输入邮件所在桶
配置人工处理的邮箱地址
    private static final String MANUAL_SERVICE_MAIL_ADDRESS = "";//人工处理收件箱
回复的内容，根据业务需求进行调整
    private static final String AI_CONTENT_logistics = "Thanks for your mail---logistics---This is a AI Request for logistics.";
    private static final String AI_CONTENT_invoice = "Thanks for your mail---invoice---This is a AI Request for invoice.";
    private static final String MANUAL_TRANSFER_CONTENT = "This mail should be processed manually.";

发件箱的邮箱地址和密码
    private static String myEmailAccount = "";//邮箱帐号需要输入
    private static String myEmailPassword = "";//邮箱需要输入
发件箱的服务器地址    
    private static String myEmailSMTPHost = "";//邮箱SMTP服务器地址需要输入







            




        
        
