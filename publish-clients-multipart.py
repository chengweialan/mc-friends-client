"""Mirror existing release archives with parallel parts; no rebuild or redownload checks."""
import concurrent.futures,importlib.util,pathlib,time
root=pathlib.Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('publisher',root/'publish-cos.py');p=importlib.util.module_from_spec(spec);spec.loader.exec_module(p)
bucket='friends-mc-downloads-1318356926';region='ap-beijing';version=(root/'CLIENT-VERSION').read_text().strip()
def upload(path):
    api=p.client(bucket,region);last=[0]
    def progress(done,total):
        if time.monotonic()-last[0]>15 or done==total:
            print(path.name,done,'/',total,flush=True);last[0]=time.monotonic()
    print('Starting multipart:',path.name,flush=True)
    api.upload_file(Bucket=bucket,Key='friends-mc/clients/'+version+'/'+path.name,LocalFilePath=str(path),PartSize=2,MAXThread=10,EnableMD5=False,progress_callback=progress,CacheControl='public, max-age=31536000, immutable')
    print('Uploaded:',path.name,flush=True)
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
    list(pool.map(upload,sorted((root/'dist').glob('*.zip'))))
upload(root/'dist/SHA256SUMS.txt')
