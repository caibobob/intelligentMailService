import json
import urllib.parse
import os
import boto4

s3 = boto4.client('s3')
comprehend = boto4.client('comprehend',region_name='us-west-2')

def lambda_handler(event, context):
    # print(dir(comprehend))
    bucket = event['Records'][0]['s3']['bucket']['name']
    key = urllib.parse.unquote_plus(event['Records'][0]['s3']['object']['key'], encoding='utf-8')
    print(key)
    s3uri = 's3://'+bucket+'/'+key.split("/")[0]+'/'
    directoryName = key.split("/")[0]
    print(s3uri)
    try:
        response = comprehend.start_document_classification_job(
            # JobName='test',
            DocumentClassifierArn='arn:aws:comprehend:us-west-2:account-id:document-classifier/SAIHE',
            InputDataConfig={
                'S3Uri': s3uri,
                'InputFormat': 'ONE_DOC_PER_LINE'
            },
            OutputDataConfig={
                'S3Uri': 's3://mailsupport-2/'+directoryName+'/'
            },
            DataAccessRoleArn='arn:aws:iam::account-id:role/service-role/AmazonComprehendServiceRole-comprehens3'
        )
        print(response)
        
        
        
        print("**************** Job finish ****************")        
    except Exception as e:
        print('**************** process error **************')
        print(e)
