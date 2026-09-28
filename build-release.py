"""Build the Windows portable client from pinned upstream archives (Python 3.11+)."""
import argparse, hashlib, json, os, shutil, struct, urllib.request, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent

def download(url, dest, sha=None):
    dest.parent.mkdir(parents=True, exist_ok=True)
    if not dest.exists():
        req = urllib.request.Request(url, headers={'User-Agent': 'FriendsMC-release-builder'})
        with urllib.request.urlopen(req, timeout=120) as response, dest.open('wb') as output:
            shutil.copyfileobj(response, output)
    if sha and hashlib.sha256(dest.read_bytes()).hexdigest() != sha:
        raise RuntimeError(f'Checksum mismatch: {dest.name}')

def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding='utf-8')

def build(cache, destination):
    if destination.exists():
        raise RuntimeError('Output directory already exists; use a fresh directory.')
    sources = json.loads((ROOT/'client-source/THIRD-PARTY.json').read_text(encoding='utf-8-sig'))
    files = [
        ('pcl-2.13.1.1.zip', sources['PCL']['url'], sources['PCL']['sha256'], 'launcher'),
        ('java.zip', sources['Zulu Java']['url'], sources['Zulu Java']['sha256'], 'runtime'),
        ('packwiz-installer.jar', 'https://github.com/packwiz/packwiz-installer/releases/download/v0.5.14/packwiz-installer.jar', sources['packwiz-installer']['sha256'], None),
        ('packwiz-installer-bootstrap.jar', 'https://github.com/packwiz/packwiz-installer-bootstrap/releases/download/v0.0.3/packwiz-installer-bootstrap.jar', sources['packwiz-installer-bootstrap']['sha256'], None),
    ]
    client = destination/'FriendsMC'
    shutil.copytree(ROOT/'client-source', client, ignore=shutil.ignore_patterns('state', 'logs', 'accounts.json', '*.log'))
    for name,url,sha,folder in files:
        archive = cache/name
        download(url,archive,sha)
        if folder:
            target = (client/folder).resolve()
            with zipfile.ZipFile(archive) as z:
                for member in z.infolist():
                    if not (target/member.filename).resolve().is_relative_to(target):
                        raise RuntimeError('Unsafe ZIP member')
                z.extractall(target)
        else:
            (client/'tools').mkdir(exist_ok=True)
            shutil.copy2(archive,client/'tools'/name)
    if hashlib.sha256((client/'launcher/Plain Craft Launcher 2.exe').read_bytes()).hexdigest() != sources['PCL']['exeSha256']:
        raise RuntimeError('Unexpected PCL executable in official archive')
    def nbtstr(s):
        b=s.encode(); return struct.pack('>H',len(b))+b
    data=b'\x0a\x00\x00\x09'+nbtstr('servers')+b'\x0a'+struct.pack('>i',1)
    for name,value in [('name','Friends Minecraft'),('ip','wze.rainplay.cn:21250')]:
        data+=b'\x08'+nbtstr(name)+nbtstr(value)
    (client/'templates').mkdir(exist_ok=True)
    (client/'templates/servers.dat').write_bytes(data+b'\x00\x00')
    license_urls={
        'PCL-LICENCE':'https://raw.githubusercontent.com/Meloong-Git/PCL/0ce35fe7e5cb939500ef754ff177b082927374ad/LICENCE',
        'packwiz-installer-LICENSE':'https://raw.githubusercontent.com/packwiz/packwiz-installer/v0.5.14/LICENSE',
        'packwiz-bootstrap-LICENSE':'https://raw.githubusercontent.com/packwiz/packwiz-installer-bootstrap/v0.0.3/LICENSE',
    }
    for name,url in license_urls.items():
        download(url,client/'licenses'/name)
    shutil.copy2(ROOT/'PLAYER-GUIDE.md',client/'使用说明.md')
    archive=destination/'FriendsMC-Windows-x64.zip'
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=5) as z:
        for p in sorted(client.rglob('*')):
            if p.is_file(): z.write(p,p.relative_to(destination))
    digest=hashlib.sha256(archive.read_bytes()).hexdigest()
    patch=destination/'FriendsMC-UI-Update.zip'
    with zipfile.ZipFile(patch,'w',zipfile.ZIP_DEFLATED) as z:
        z.write(ROOT/'client-source/Start.cmd','Start.cmd')
        for p in sorted((ROOT/'client-source/scripts').glob('*.ps1')):
            z.write(p,'scripts/'+p.name)
        z.write(ROOT/'UI-UPDATE.md','更新说明.md')
    patch_digest=hashlib.sha256(patch.read_bytes()).hexdigest()
    write(destination/'SHA256SUMS.txt',digest+'  '+archive.name+'\n'+patch_digest+'  '+patch.name+'\n')
    print(f'Built {archive}: {archive.stat().st_size} bytes, SHA256 {digest}')

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--cache',type=Path,default=ROOT/'.build-cache')
    parser.add_argument('--output',type=Path,default=ROOT/'dist')
    args=parser.parse_args()
    build(args.cache.resolve(),args.output.resolve())
