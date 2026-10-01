"""GitHub Actions: issue short-lived, encrypted PUT URLs for owner uploads.

The owner's COS credentials never leave Actions. A request contains only object
names and a one-use public key; encrypted tickets are useless without its local key.
"""
import base64, json, os, re, zlib
from pathlib import Path
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from qcloud_cos import CosConfig, CosS3Client

request=json.loads(Path('upload-request.json').read_text())
ident=request['id']
if not re.fullmatch(r'[a-z0-9-]{1,70}',ident): raise ValueError('Invalid request id')
keys=json.loads(zlib.decompress(base64.b64decode(request['keysZlibB64'])))
if not 1<=len(keys)<=5000: raise ValueError('Invalid upload count')
for key in keys:
    if not key.startswith('friends-mc/packs/fool/') or '..' in key.split('/') or '\\' in key:
        raise ValueError('Upload outside Fool prefix')
api=CosS3Client(CosConfig(Region='ap-beijing',SecretId=os.environ['COS_SECRET_ID'].strip(),SecretKey=os.environ['COS_SECRET_KEY'].strip(),Scheme='https'))
bucket='friends-mc-downloads-1318356926'
urls={key:api.get_presigned_url(Method='PUT',Bucket=bucket,Key=key,Expired=7200) for key in keys}
key=AESGCM.generate_key(bit_length=256); nonce=os.urandom(12)
public=serialization.load_pem_public_key(request['publicKey'].encode())
aad=ident.encode()
encrypted=AESGCM(key).encrypt(nonce,json.dumps(urls).encode(),aad)
wrapped=public.encrypt(key,padding.OAEP(mgf=padding.MGF1(hashes.SHA256()),algorithm=hashes.SHA256(),label=None))
b64=lambda b:base64.b64encode(b).decode()
payload=json.dumps({'id':ident,'key':b64(wrapped),'nonce':b64(nonce),'data':b64(encrypted)}).encode()
api.put_object(Bucket=bucket,Key='friends-mc/packs/fool/upload-tickets/'+ident+'.json',Body=payload,CacheControl='no-store')
print('Encrypted upload ticket ready:',ident,'objects:',len(keys))
