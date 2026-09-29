import hashlib, json, re, tomllib, urllib.request, concurrent.futures
from pathlib import Path
root=Path(__file__).resolve().parent
channel=json.loads((root/'channel.json').read_text(encoding='utf-8-sig'))
assert channel['schema']==2 and channel['java']==25
assert re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?',channel['release'])
assert re.fullmatch(r'https://raw\.githubusercontent\.com/chengweialan/mc-friends-client/[0-9a-f]{40}/pack/pack\.toml',channel['packUrl'])
with urllib.request.urlopen(channel['packUrl'],timeout=45) as r: raw=r.read()
pack=tomllib.loads(raw.decode())
assert pack['versions']['minecraft']==channel['minecraft']
assert pack['versions']['neoforge']==channel['neoforge']
assert pack['version']==channel['release']
assert pack['index']['file']=='index.toml' and pack['index']['hash-format']=='sha256'
with urllib.request.urlopen(channel['packUrl'].replace('pack.toml','index.toml'),timeout=45) as r: index=r.read()
assert hashlib.sha256(index).hexdigest()==pack['index']['hash']
with urllib.request.urlopen(channel['packUrl'].replace('pack.toml','mods.lock.json'),timeout=45) as r: catalog_bytes=r.read()
assert hashlib.sha256(catalog_bytes).hexdigest()==channel['catalogSha256']
catalog=json.loads(catalog_bytes)
for k in ['release','minecraft','neoforge']:assert catalog[k]==channel[k]
entries=tomllib.loads(index.decode())['files']
assert len(entries)==len(catalog['mods'])
def verify_entry(e):
    with urllib.request.urlopen(channel['packUrl'].replace('pack.toml',e['file']),timeout=45) as r: b=r.read()
    assert hashlib.sha256(b).hexdigest()==e['hash'],e['file']
    m=tomllib.loads(b.decode());locked=next(x for x in catalog['mods'] if x['filename']==m['filename'])
    assert m['download']['hash']==locked['sha256'] and m['download']['url']==locked['url']
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:list(pool.map(verify_entry,entries))
for m in catalog['mods']:
    assert '26.3'==catalog['minecraft']
    assert re.fullmatch(r'[A-Za-z0-9_.+ -]+\.jar',m['filename'])
    assert re.fullmatch(r'[a-f0-9]{64}',m['sha256'])
    assert m['url'].startswith('https://cdn.modrinth.com/')
print('Published immutable pack, versions and index checksum verified.')
