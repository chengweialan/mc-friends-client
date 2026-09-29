"""Small owner-side helper. Never deploys, pushes Git, or touches the live server."""
import argparse, hashlib, io, json, pathlib, re, subprocess, sys, urllib.request, zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
LOCK=ROOT/'pack/mods.lock.json'
def read(): return json.loads(LOCK.read_text(encoding='utf-8-sig'))
def write(path,value): path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
def api(path):
    req=urllib.request.Request('https://api.modrinth.com/v2/'+path,headers={'User-Agent':'FriendsMC-owner-maintenance/1.0'})
    with urllib.request.urlopen(req,timeout=45) as response:return json.load(response)
def version_id(value):
    value=value.rstrip('/').split('/')[-1]
    if not re.fullmatch(r'[A-Za-z0-9]+',value):raise ValueError('Use a Modrinth version ID or version page URL')
    return value
def save(c):
    write(LOCK,c)
    subprocess.run([sys.executable,str(ROOT/'build-pack.py')],check=True)
def upsert(a):
    c=read();v=api('version/'+version_id(a.version))
    if c['minecraft'] not in v['game_versions'] or 'neoforge' not in v['loaders']:
        raise ValueError('Version is not marked for this Minecraft version + NeoForge')
    project=api('project/'+v['project_id'])
    existing=next((m for m in c['mods'] if m['projectId']==v['project_id']),None)
    mod_id=a.id or (existing['id'] if existing else project['slug'])
    if existing and mod_id!=existing['id']:raise ValueError('Keep the existing local ID so dependent mods remain linked')
    if not re.fullmatch(r'[a-z0-9][a-z0-9_-]*',mod_id):raise ValueError('Invalid local mod ID')
    if any(m['id']==mod_id and m['projectId']!=v['project_id'] for m in c['mods']):raise ValueError('ID belongs to another mod')
    deps=[]
    for dep in v.get('dependencies',[]):
        if dep['dependency_type']!='required':continue
        pid=dep.get('project_id');dv=None
        if dep.get('version_id'):dv=api('version/'+dep['version_id']);pid=pid or dv['project_id']
        match=next((m for m in c['mods'] if m['projectId']==pid),None)
        if not match:raise ValueError('Add required dependency first: '+str(pid)+' version '+str(dep.get('version_id')))
        if dv and match['versionId']!=dv['id']:raise ValueError('Dependency needs pinned version '+dv['id']+': '+match['id'])
        deps.append(match['id'])
    f=next((f for f in v['files'] if f.get('primary')),v['files'][0])
    if not f['url'].startswith('https://cdn.modrinth.com/'):raise ValueError('Current updater accepts Modrinth CDN URLs only')
    name=f['filename']
    if not re.fullmatch(r'[A-Za-z0-9_.+ -]+\.jar',name):raise ValueError('Unsafe/unsupported file name')
    # One download supplies the manifest fingerprint and actual mod IDs; no repeated download tests.
    with urllib.request.urlopen(f['url'],timeout=90) as response:data=response.read()
    with zipfile.ZipFile(io.BytesIO(data)) as jar:
        entry='META-INF/neoforge.mods.toml'
        if entry not in jar.namelist():raise ValueError('No NeoForge metadata found; inspect this artifact manually')
        text=jar.read(entry).decode('utf-8-sig')
        ids=[]
        for block in re.findall(r'(?s)\[\[mods\]\](.*?)(?=\n\s*\[|\Z)',text):
            ids+=re.findall(r'(?m)^\s*modId\s*=\s*"([^"]+)"',block)
        if not ids:raise ValueError('Could not determine mod IDs')
    required=(a.kind=='required') if a.kind else bool(existing and existing['required'])
    hidden=(a.kind=='dependency') if a.kind else bool(existing and existing['hidden'])
    server=bool(a.server or (existing and existing['server']))
    m=dict(id=mod_id,name=a.name or (existing['name'] if existing else project['title']),description=a.description or (existing['description'] if existing else project['description']),required=required,server=server,client=True,hidden=hidden,default=not hidden,dependencies=deps,version=v['version_number'],projectId=v['project_id'],versionId=v['id'],filename=name,url=f['url'],sha256=hashlib.sha256(data).hexdigest(),size=len(data),modIds=ids,source='https://modrinth.com/mod/'+project['slug'])
    c['mods']=[m if x['projectId']==v['project_id'] else x for x in c['mods']]
    if not existing:c['mods'].append(m)
    cache=ROOT/'.mod-cache';cache.mkdir(exist_ok=True);(cache/name).write_bytes(data)
    save(c)
    print('Prepared:',mod_id,m['version'],'required='+str(required),'server='+str(server))
    print('Review embedded JAR dependencies and mod-specific requirements. Bump pack version before publishing.')
def main():
    p=argparse.ArgumentParser(description=__doc__);commands=p.add_subparsers(dest='command',required=True)
    a=commands.add_parser('upsert',help='Add/update a client mod from an exact Modrinth version')
    a.add_argument('version');a.add_argument('--id');a.add_argument('--name');a.add_argument('--description');a.add_argument('--kind',choices=['required','optional','dependency']);a.add_argument('--server',action='store_true')
    a=commands.add_parser('remove');a.add_argument('id')
    a=commands.add_parser('bump');a.add_argument('release')
    a=commands.add_parser('channel',help='Point channel at an already committed/pushed pack');a.add_argument('commit')
    commands.add_parser('list')
    a=p.parse_args()
    if a.command=='upsert':upsert(a);return
    c=read()
    if a.command=='list':
        for m in c['mods']:print(m['id'],m['version'],'required='+str(m['required']),'server='+str(m['server']))
    elif a.command=='remove':
        users=[m['id'] for m in c['mods'] if a.id in m['dependencies']]
        if users:raise ValueError('Still required by: '+', '.join(users))
        if not any(m['id']==a.id for m in c['mods']):raise ValueError('Mod ID not found')
        c['mods']=[m for m in c['mods'] if m['id']!=a.id];save(c);print('Removed from managed catalog:',a.id)
    elif a.command=='bump':
        if not re.fullmatch(r'\d+\.\d+\.\d+(?:-[A-Za-z0-9.-]+)?',a.release):raise ValueError('Use a semantic pack version')
        c['release']=a.release;save(c);print('Pack release:',a.release)
    elif a.command=='channel':
        commit=subprocess.check_output(['git','rev-parse',a.commit+'^{commit}'],cwd=ROOT,text=True).strip()
        data=subprocess.check_output(['git','show',commit+':pack/mods.lock.json'],cwd=ROOT)
        manifest=json.loads(data)
        channel=json.loads((ROOT/'channel.json').read_text(encoding='utf-8-sig'))
        channel.update(release=manifest['release'],minecraft=manifest['minecraft'],neoforge=manifest['neoforge'],packUrl='https://raw.githubusercontent.com/chengweialan/mc-friends-client/'+commit+'/pack/pack.toml',catalogSha256=hashlib.sha256(data).hexdigest())
        write(ROOT/'channel.json',channel);print('Prepared channel:',manifest['release'],commit)
if __name__=='__main__':main()
