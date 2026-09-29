"""Publish prepared artifacts. Credentials only from environment; no ACL changes.
Requires cos-python-sdk-v5. A dedicated public-read bucket/prefix is configured separately.
"""
import argparse, hashlib, json, os, urllib.request
from pathlib import Path

def publish(directory, bucket, region, prefix):
    from qcloud_cos import CosConfig, CosS3Client
    if not prefix or any(p in ('','..','.') for p in prefix.split('/')): raise ValueError('Use a non-empty dedicated prefix')
    client=CosS3Client(CosConfig(Region=region,SecretId=os.environ['COS_SECRET_ID'],SecretKey=os.environ['COS_SECRET_KEY'],Token=os.environ.get('COS_SESSION_TOKEN'),Scheme='https'))
    channel=directory/'channel.json';manifest=json.loads(channel.read_text(encoding='utf-8-sig'))
    commit=manifest['packUrl'].split('/')[5]
    catalog=directory/'releases'/commit/'pack/mods.lock.json'
    if hashlib.sha256(catalog.read_bytes()).hexdigest()!=manifest['catalogSha256']: raise ValueError('Catalog does not match channel')
    files=sorted(p for p in directory.rglob('*') if p.is_file() and p!=channel)
    for file in files+[channel]:
        key=prefix+'/'+file.relative_to(directory).as_posix();data=file.read_bytes()
        client.put_object(Bucket=bucket,Key=key,Body=data,CacheControl='no-cache, max-age=0' if file==channel else 'public, max-age=31536000, immutable')
        # Verify anonymous public retrieval before publishing the mutable channel pointer.
        url='https://'+bucket+'.cos.'+region+'.myqcloud.com/'+urllib.parse.quote(key,safe='/')
        with urllib.request.urlopen(url,timeout=60) as response: actual=response.read()
        if hashlib.sha256(actual).digest()!=hashlib.sha256(data).digest(): raise RuntimeError('Public verification failed: '+key)
        print('Verified:',key)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--directory',type=Path,required=True);p.add_argument('--bucket',required=True);p.add_argument('--region',required=True);p.add_argument('--prefix',default='friends-mc');a=p.parse_args();publish(a.directory,a.bucket,a.region,a.prefix)
