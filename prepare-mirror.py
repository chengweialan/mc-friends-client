"""Prepare a mirror without modifying the GitHub channel or downloading unapproved mods.

Mod approval is an explicit SHA-256 -> license/evidence URL map, reviewed by the owner.
Upload immutable objects first and channel.json last using publish-cos.py.
"""
import argparse, hashlib, json, re, shutil, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent

def prepare(output, approvals, cache):
    channel = json.loads((ROOT/'channel.json').read_text(encoding='utf-8-sig'))
    match = re.fullmatch(r'https://raw\.githubusercontent\.com/chengweialan/mc-friends-client/([a-f0-9]{40})/pack/pack\.toml',channel['packUrl'])
    if not match: raise ValueError('Channel must refer to an immutable pack')
    catalog_bytes=(ROOT/'pack/mods.lock.json').read_bytes()
    if hashlib.sha256(catalog_bytes).hexdigest()!=channel['catalogSha256']: raise ValueError('Catalog hash mismatch; do not publish')
    if output.exists(): raise ValueError('Use a fresh output directory')
    target=output/'releases'/match[1]/'pack'
    shutil.copytree(ROOT/'pack',target)
    approved=json.loads(approvals.read_text(encoding='utf-8')) if approvals else {}
    report={'mirrored':[], 'upstreamOnly':[]}
    for mod in json.loads(catalog_bytes)['mods']:
        sha=mod['sha256'];evidence=approved.get(sha)
        if not evidence:
            report['upstreamOnly'].append(mod['id']);continue
        if not isinstance(evidence,str) or not evidence.startswith('https://'): raise ValueError('Approval needs a license/evidence URL')
        source=cache/mod['filename']
        data=source.read_bytes() if source.exists() else urllib.request.urlopen(mod['url'],timeout=120).read()
        if hashlib.sha256(data).hexdigest()!=sha or len(data)!=mod['size']: raise ValueError('Bad mod: '+mod['id'])
        dest=output/'mods'/(sha+'.jar');dest.parent.mkdir(exist_ok=True);dest.write_bytes(data)
        report['mirrored'].append({'id':mod['id'],'sha256':sha,'source':mod['source'],'licenseEvidence':evidence})
    (output/'mirror-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    shutil.copyfile(ROOT/'channel.json',output/'channel.json')
    print(json.dumps(report,ensure_ascii=False,indent=2))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);p.add_argument('--approvals',type=Path);p.add_argument('--cache',type=Path,default=ROOT/'.mod-cache');a=p.parse_args();prepare(a.output,a.approvals,a.cache)
