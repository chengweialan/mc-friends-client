"""Derive packwiz metadata from the single, audited mod lock used by both clients."""
import hashlib,json
from pathlib import Path
R=Path(__file__).resolve().parent;P=R/'pack'
def quote(s):return json.dumps(s,ensure_ascii=False)
def build():
 c=json.loads((P/'mods.lock.json').read_text(encoding='utf8'));entries=[]
 for m in c['mods']:
  side='both' if m['server'] else 'client'
  text=f'name = {quote(m["name"])}\nfilename = {quote(m["filename"])}\nside = {quote(side)}\n\n[download]\nurl = {quote(m["url"])}\nhash-format = "sha256"\nhash = {quote(m["sha256"])}\n\n[update.modrinth]\nmod-id = {quote(m["projectId"])}\nversion = {quote(m["versionId"])}\n'
  if not m['required'] and not m['hidden']:
   text+=f'\n[option]\noptional = true\ndefault = true\ndescription = {quote(m["description"])}\n'
  path=P/'mods'/(m['id']+'.pw.toml');path.parent.mkdir(exist_ok=True);path.write_text(text,encoding='utf8',newline='\n')
  entries.append(f'\n[[files]]\nfile = "mods/{m["id"]}.pw.toml"\nhash = "{hashlib.sha256(path.read_bytes()).hexdigest()}"\nmetafile = true\n')
 index='hash-format = "sha256"\n'+''.join(entries);(P/'index.toml').write_text(index,encoding='utf8',newline='\n')
 text=f'name = "Friends MC"\nauthor = "chengweialan"\nversion = "{c["release"]}"\npack-format = "packwiz:1.1.0"\n\n[index]\nfile = "index.toml"\nhash-format = "sha256"\nhash = "{hashlib.sha256(index.encode()).hexdigest()}"\n\n[versions]\nminecraft = "{c["minecraft"]}"\nneoforge = "{c["neoforge"]}"\n'
 (P/'pack.toml').write_text(text,encoding='utf8',newline='\n')
if __name__=='__main__':build()
