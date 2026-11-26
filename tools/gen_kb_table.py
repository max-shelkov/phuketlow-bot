# -*- coding: utf-8 -*-
"""Generate a markdown table of the rental catalog for the knowledge base."""
import openpyxl

wb = openpyxl.load_workbook('src/main/xlsx/real_estate.xlsx', data_only=True)
ws = wb['rental']
rows = list(ws.iter_rows(values_only=True))
hdr = [str(h).strip() if h else '' for h in rows[0]]
data = [dict(zip(hdr, r)) for r in rows[1:] if any(r)]


def norm_bed(v):
    try:
        return int(v)
    except (TypeError, ValueError):
        return 0


def norm_price(v):
    if v is None:
        return None
    s = ''.join(ch for ch in str(v) if ch.isdigit())
    return int(s) if s else None


data.sort(key=lambda d: (norm_bed(d.get('bedrooms')), norm_price(d.get('price_from')) or 0))

def clean(v):
    return str(v or '').replace('|', '/').strip()


lines = []
for d in data:
    name = clean(d.get('name'))
    cat = clean(d.get('category'))
    bed = norm_bed(d.get('bedrooms'))
    beach = clean(d.get('beach'))
    price = norm_price(d.get('price_from'))
    lines.append('| {} | {} | {} | {} | {} |'.format(
        name, cat, bed, beach, price if price else '—'))

with open('tools/_kb_table.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines))
print('rows', len(lines))
