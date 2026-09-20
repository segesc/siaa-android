#!/usr/bin/env python3
import json, sys
from pathlib import Path
root=Path(__file__).resolve().parents[1]
errors=[]
required=[
 'settings.gradle.kts','app/build.gradle.kts','app/src/main/AndroidManifest.xml',
 'app/src/main/assets/content/kcs.json','app/src/main/assets/content/edges.json','app/src/main/assets/content/exercises.json','app/src/main/assets/content/lexemes.json',
 'core/model/build.gradle.kts','core/algorithm/build.gradle.kts','core/runtime/build.gradle.kts','core/data/build.gradle.kts','core/audio/build.gradle.kts','core/content/build.gradle.kts'
]
for rel in required:
    if not (root/rel).exists(): errors.append(f'missing:{rel}')
try:
    kcs=json.loads((root/'app/src/main/assets/content/kcs.json').read_text())
    edges=json.loads((root/'app/src/main/assets/content/edges.json').read_text())
    ex=json.loads((root/'app/src/main/assets/content/exercises.json').read_text())
    lex=json.loads((root/'app/src/main/assets/content/lexemes.json').read_text())
    ids={x['id'] for x in kcs}
    if len(ids)!=len(kcs): errors.append('duplicate KC ids')
    exids={x['id'] for x in ex}
    if len(exids)!=len(ex): errors.append('duplicate exercise ids')
    for e in edges:
        if e['fromId'] not in ids: errors.append(f"edge missing from {e['fromId']}")
        if e['toId'] not in ids: errors.append(f"edge missing to {e['toId']}")
    for q in ex:
        for kid in q['kcIds']:
            if kid not in ids: errors.append(f"exercise {q['id']} missing KC {kid}")
        if q['type'] not in {'TEACH','SELF_ASSESS','SPELL_FROM_AUDIO'} and q.get('correctOption') not in {'A','B'}:
            errors.append(f"exercise {q['id']} lacks A/B key")
    # Hard prerequisite cycle check
    adj={i:[] for i in ids}
    for e in edges:
        if e.get('hardPrerequisite', True) and e['fromId'] in ids and e['toId'] in ids:
            adj[e['fromId']].append(e['toId'])
    color={}
    stack=[]
    def dfs(n):
        color[n]=1; stack.append(n)
        for m in adj.get(n,[]):
            if color.get(m,0)==0: dfs(m)
            elif color.get(m)==1:
                errors.append('hard prerequisite cycle: '+' -> '.join(stack[stack.index(m):]+[m]))
        stack.pop(); color[n]=2
    for n in ids:
        if color.get(n,0)==0: dfs(n)
    for item in lex:
        if item['id'] not in ids: errors.append(f"lexeme missing KC {item['id']}")
        if not item.get('lemma'): errors.append(f"lexeme lacks lemma {item.get('id')}")
    estimated_generated=sum(3+min(3,len(item.get('chunks',[]))) for item in lex)
    print(f"content: {len(kcs)} KCs, {len(edges)} edges, {len(ex)} static exercises, {len(lex)} lexemes, ~{estimated_generated} generated variants")
except Exception as e: errors.append(f'content parse: {e}')
kt=list(root.glob('**/*.kt'))
print(f'kotlin files: {len(kt)}')
if errors:
    print('\n'.join('ERROR '+x for x in errors)); sys.exit(1)
print('project validation OK')
