import json
import urllib.parse
import os
import boto3

s3 = boto3.client('s3')
comprehend = boto3.client('comprehend',region_name='ap-northeast-1')

def lambda_handler(event, context):
    print(f"ecent:{event}")
    bucket = event['Records'][0]['s3']['bucket']['name']
    key = urllib.parse.unquote_plus(event['Records'][0]['s3']['object']['key'], encoding='utf-8')
    print(key)
    s3uri = 's3://'+bucket+'/'+key
    directoryName = key.split("/")[0]
    print(f"s3uri:{s3uri}")
    # TODO implement
    response = comprehend.start_document_classification_job(
    JobName='ecommerce',
    DocumentClassifierArn='arn:aws:comprehend:ap-northeast-1:accountid:document-classifier/ecommerce',
    InputDataConfig={
        'S3Uri': s3uri,
        'InputFormat': 'ONE_DOC_PER_LINE'
    },
    OutputDataConfig={
        'S3Uri': 's3://aws-caibc-comprehen-training-tokyo/result/',
    },
    DataAccessRoleArn='arn:aws:iam::accountid:role/service-role/AmazonComprehendServiceRole-comprehens3',
    )
    print(f"Comprehend response:{response}")
