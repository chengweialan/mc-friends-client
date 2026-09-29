"""Integration checks for actual Java synchronizer; uses verified cached mod JARs."""
import argparse,hashlib,json,subprocess,zipfile,uuid,shutil
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--java',required=True);p.add_argument('--jar',required=True);p.add_argument('--root',required=True);a=p.parse_args()
repo=Path(__file__).resolve().parent;root=Path(a.root).resolve();game=root/'game';catalog=repo/'pack/mods.lock.json';c=json.loads(catalog.read_text(encoding='utf8'))
def run(cat=catalog,minimal=False,ok=True):
 cmd=[a.java,'-Dstdout.encoding=UTF-8','-Dstderr.encoding=UTF-8','-jar',a.jar,'test',str(root),str(game),str(cat)]
 if minimal:cmd.append('minimal')
 r=subprocess.run(cmd,capture_output=True,text=True,encoding='utf8');assert (r.returncode==0)==ok,r.stdout+r.stderr;return r
run();assert len(list((game/'mods').glob('*.jar')))==len(c['mods'])
personal=game/'mods/personal-test.jar'
with zipfile.ZipFile(personal,'w') as z:z.writestr('META-INF/neoforge.mods.toml','[[mods]]\nmodId="personal_test"\nversion="1"\n')
config=game/'options.txt';config.write_text('personal settings sentinel')
run(minimal=True)
required=[m for m in c['mods'] if m['required']]
assert len(list((game/'mods').glob('*.jar')))==len(required)+1
assert personal.exists() and config.read_text()=='personal settings sentinel'
target=game/'mods'/required[0]['filename'];expected=required[0]['sha256']
target.write_bytes(b'corrupted file');run(minimal=True);assert hashlib.sha256(target.read_bytes()).hexdigest()==expected
# Recovery of an interrupted commit, without deleting the personal mod.
state=game/'.friends-sync';backup=state/('backup-'+str(uuid.uuid4()));backup.mkdir();shutil.copy2(target,backup/target.name);shutil.copy2(state/'managed.json',backup/'receipt.json')
(state/'transaction.json').write_text(json.dumps({'backup':backup.name,'files':[{'name':target.name,'existed':True}]}))
target.write_bytes(b'interrupted transaction');run(minimal=True);assert not (state/'transaction.json').exists();assert hashlib.sha256(target.read_bytes()).hexdigest()==expected
# An unmanaged duplicate must be reported, not silently deleted.
duplicate=game/'mods/my-own-corpse.jar';shutil.copy2(target,duplicate);r=run(minimal=True,ok=False);assert 'my-own-corpse.jar' in r.stderr;duplicate.unlink()
bad=json.loads(json.dumps(c));bad['mods'][0]['filename']='../escape.jar';badpath=root/'invalid.json';badpath.write_text(json.dumps(bad));run(badpath,minimal=True,ok=False)
assert personal.exists() and config.read_text()=='personal settings sentinel'
print('PASS: full sync, minimal choices, dependency removal, hash repair, interrupted transaction recovery, personal file preservation, duplicate detection, unsafe path rejection')
