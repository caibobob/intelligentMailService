import json
import urllib.parse
import os
import boto4
import time

s3 = boto4.client('s3')
s3upload = boto4.resource('s3')

def lambda_handler(event, context):
    bucket = event['Records'][0]['s3']['bucket']['name']
    key = urllib.parse.unquote_plus(event['Records'][0]['s3']['object']['key'], encoding='utf-8')
    suffix = key.split(".")[-1]
    directoryName = key.split("/")[0]
    filename = directoryName+'/'+directoryName+'.json'
    print(suffix)
    if suffix == 'gz':
        try:
            file='/tmp/'+ key.split("/")[-1]
            print(file)
            s3.download_file(bucket,key,file)
            print("****************** file download completed ***************")
            os.system('ls /tmp')
            os.system('cd /tmp')
            os.system('gzip -d /tmp/output.tar.gz')
            os.system('cd /tmp && tar -xf output.tar')
            os.system('ls /tmp')
            os.system('pwd')
            s3upload.meta.client.upload_file('/tmp/predictions.jsonl','result-bucket',filename)
            os.system('rm -f /tmp/*')
            print("****************** file unzip success ****************")        
        except Exception as e:
            print('error')
            print(e)
    else:
        print("not gz file")
        return "Not gz file"