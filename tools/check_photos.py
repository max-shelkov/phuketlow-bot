# -*- coding: utf-8 -*-
"""
Собирает все имена фото из real_estate.xlsx (лист rental, колонки photo1..photo10)
и проверяет наличие каждого файла в репозитории phuketlow-bot-photos через GitHub API.
Выводит список отсутствующих файлов.
"""
import json
import urllib.request
import openpyxl

XLSX = 'src/main/xlsx/real_estate.xlsx'
SHEET = 'rental'
REPO = 'max-shelkov/phuketlow-bot-photos'
BRANCH = 'main'

wb = openpyxl.load_workbook(XLSX)
ws = wb[SHEET]
headers = [c.value for c in ws[1]]

# Собираем все имена фото (нормализуем .png -> .jpg как в боте)
photos = set()
for r in range(2, ws.max_row + 1):
    for i in range(len(headers)):
        h = (headers[i] or '').lower()
        if h.startswith('photo'):
            v = ws.cell(row=r, column=i + 1).value
            if v and str(v).strip():
                name = str(v).strip()
                if name.lower().endswith('.png'):
                    name = name[:-4] + '.jpg'
                photos.add(name)

print('Total unique photo names in xlsx: %d' % len(photos))

# Получаем список файлов репозитория через GitHub API (git/trees recursive)
url = 'https://api.github.com/repos/%s/git/trees/%s?recursive=1' % (REPO, BRANCH)
req = urllib.request.Request(url, headers={'User-Agent': 'phuketlow-check'})
try:
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read().decode('utf-8'))
except Exception as e:
    print('ERROR fetching repo tree: %s' % e)
    raise SystemExit(1)

repo_files = set()
for item in data.get('tree', []):
    if item.get('type') == 'blob':
        repo_files.add(item['path'].split('/')[-1])

print('Total files in repo: %d' % len(repo_files))
print('truncated flag: %s' % data.get('truncated'))

missing = sorted(p for p in photos if p not in repo_files)
print()
print('MISSING (%d):' % len(missing))
for m in missing:
    print('  ' + m)
