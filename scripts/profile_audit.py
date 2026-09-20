#!/usr/bin/env python3
import csv, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
content=ROOT/'app/src/main/assets/content'
ref=ROOT/'app/src/main/assets/reference/cefrj'
kcs={x['id']:x for x in json.loads((content/'kcs.json').read_text())}
lex=json.loads((content/'lexemes.json').read_text())
profile={}
with (ref/'cefrj-vocabulary-profile-1.5.csv').open(newline='',encoding='utf-8-sig') as f:
    for row in csv.DictReader(f):
        profile.setdefault(row['headword'].lower(), set()).add(row['CEFR'])
with (ref/'octanove-vocabulary-profile-c1c2-1.0.csv').open(newline='',encoding='utf-8-sig') as f:
    for row in csv.DictReader(f):
        profile.setdefault(row['headword'].lower(), set()).add(row['CEFR'])
rows=[]
for x in lex:
    app=kcs[x['id']].get('cefr','')
    levels=sorted(v for v in profile.get(x['lemma'].lower(),set()) if v)
    rows.append((x['lemma'],app,'/'.join(levels) or 'not_found', app in levels if levels else None))
print('lemma,app_cefr,reference_cefr,match')
for r in rows: print(','.join(map(str,r)))
found=sum(1 for r in rows if r[2]!='not_found')
match=sum(1 for r in rows if r[3] is True)
print(f'\nfound={found}/{len(rows)} exact_level_match={match}/{found or 1}')
