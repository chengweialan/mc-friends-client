"""Publish prepared artifacts. Credentials only from environment; no ACL changes.
Requires cos-python-sdk-v5. A dedicated public-read bucket/prefix is configured separately.
"""
import argparse, concurrent.futures, hashlib, io, json, os, re, time, urllib.request, urllib.parse
from pathlib import Path

def client(bucket,region):
    from qcloud_cos import CosConfig, CosS3Client
    secret_id=os.environ['COS_SECRET_ID'].strip();secret_key=os.environ['COS_SECRET_KEY'].strip()
    if not secret_id or not secret_key:raise ValueError('COS_SECRET_ID or COS_SECRET_KEY is empty')
    if any(c.isspace() for c in secret_id+secret_key) or any(c in secret_id+secret_key for c in '\"\''):
        raise ValueError('COS secrets contain internal whitespace or quotes; paste the raw matching values in GitHub Secrets')
    return CosS3Client(CosConfig(Region=region,SecretId=secret_id,SecretKey=secret_key,Token=os.environ.get('COS_SESSION_TOKEN'),Scheme='https',Timeout=120,EnableOldDomain=False,EnableInternalDomain=False),retry=1)

class UploadStream(io.BytesIO):
    def __init__(self,data,key):
        super().__init__(data);self.key=key;self.total=len(data);self.last=time.monotonic()
    def read(self,size=-1):
        part=super().read(size)
        if time.monotonic()-self.last>=20:
            print('Upload progress:',self.key,self.tell(),'/',self.total,'bytes',flush=True)
            self.last=time.monotonic()
        return part

def put_verified(api,bucket,region,key,data,mutable=False):
    print('Uploading:',key,len(data),'bytes',flush=True)
    # A seekable stream lets requests send bounded chunks instead of timing out while writing one huge byte string.
    api.put_object(Bucket=bucket,Key=key,Body=UploadStream(data,key),CacheControl='no-cache, max-age=0' if mutable else 'public, max-age=31536000, immutable')
    print('Upload complete; checking public download:',key,flush=True)
    url='https://'+bucket+'.cos.'+region+'.myqcloud.com/'+urllib.parse.quote(key,safe='/')
    with urllib.request.urlopen(url,timeout=120) as response: actual=response.read()
    if hashlib.sha256(actual).digest()!=hashlib.sha256(data).digest(): raise RuntimeError('Public verification failed: '+key)
    print('Verified:',key,flush=True)

def publish(directory, bucket, region, prefix):
    api=client(bucket,region)
    channel=directory/'channel.json';manifest=json.loads(channel.read_text(encoding='utf-8-sig'))
    commit=manifest['packUrl'].split('/')[5]
    catalog=directory/'releases'/commit/'pack/mods.lock.json'
    if hashlib.sha256(catalog.read_bytes()).hexdigest()!=manifest['catalogSha256']: raise ValueError('Catalog does not match channel')
    files=sorted(p for p in directory.rglob('*') if p.is_file() and p!=channel)
    for file in files+[channel]:
        key=prefix+'/'+file.relative_to(directory).as_posix();data=file.read_bytes()
        # Reuse previously uploaded immutable single-PUT objects; clients still
        # verify their catalog SHA-256. Avoid re-uploading every mod on pack edits.
        if file.parent!=directory:
            url='https://'+bucket+'.cos.'+region+'.myqcloud.com/'+urllib.parse.quote(key,safe='/')
            try:
                with urllib.request.urlopen(urllib.request.Request(url,method='HEAD'),timeout=20) as response:
                    if int(response.headers.get('Content-Length','-1'))==len(data) and response.headers.get('ETag','').strip('"')==hashlib.md5(data).hexdigest():
                        print('Unchanged:',key,flush=True)
                        continue
            except (OSError,ValueError): pass
        put_verified(api,bucket,region,key,data,mutable=file==channel or file.parent==directory)

def publish_assets(directory, version, bucket, region, prefix):
    if not re.fullmatch(r'\d+\.\d+\.\d+',version):raise ValueError('Invalid client version')
    sums=directory/'SHA256SUMS.txt';files=[]
    for line in sums.read_text().splitlines():
        sha,name=line.split()
        if not re.fullmatch(r'[A-Za-z0-9_.-]+\.zip',name):raise ValueError('Invalid asset name')
        data=(directory/name).read_bytes()
        if hashlib.sha256(data).hexdigest()!=sha:raise ValueError('Asset hash mismatch: '+name)
        files.append((name,data))
    def send(item):
        name,data=item
        put_verified(client(bucket,region),bucket,region,prefix+'/clients/'+version+'/'+name,data)
    # Small patches first; independent large archives can transfer concurrently.
    for item in sorted(files,key=lambda item:len(item[1])):
        if len(item[1])<1024*1024: send(item)
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        for result in pool.map(send,[item for item in files if len(item[1])>=1024*1024]): pass
    send(('SHA256SUMS.txt',sums.read_bytes()))
    print('All client assets verified:',version)

if __name__=='__main__':
    p=argparse.ArgumentParser();group=p.add_mutually_exclusive_group(required=True);group.add_argument('--directory',type=Path);group.add_argument('--assets',type=Path);p.add_argument('--client-version');p.add_argument('--bucket',required=True);p.add_argument('--region',required=True);p.add_argument('--prefix',default='friends-mc');a=p.parse_args()
    if not a.prefix or any(x in ('','..','.') for x in a.prefix.split('/')):raise ValueError('Invalid prefix')
    if a.assets:publish_assets(a.assets,a.client_version,a.bucket,a.region,a.prefix)
    else:publish(a.directory,a.bucket,a.region,a.prefix)
