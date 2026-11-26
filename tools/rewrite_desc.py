# -*- coding: utf-8 -*-
"""
Перезаписывает колонку description в real_estate.xlsx (лист rental):
- извлекает ключевые факты (спальни/ванны/гости, бассейн, район,
  расстояние до пляжа, ключевые удобства, что включено),
- собирает чистое русское описание,
- ограничивает длину 1024 символами.

Запуск:  python tools/rewrite_desc.py [--dry]
  --dry  только показать результат, не сохранять в xlsx
"""
import re
import sys
import openpyxl

XLSX = 'src/main/xlsx/real_estate.xlsx'
SHEET = 'rental'
LIMIT = 1024

# Перевод ключевых удобств (англ -> рус). Остальные (мелочь) отбрасываем.
AMENITY_RU = {
    'private pool': 'Частный бассейн',
    'communal pool': 'Общий бассейн',
    'outdoor pool': 'Бассейн',
    'swimming pool': 'Бассейн',
    'pool': 'Бассейн',
    'частный бассейн': 'Частный бассейн',
    'общий бассейн': 'Общий бассейн',
    'бассейн': 'Бассейн',
    'free parking': 'Бесплатная парковка',
    'free parking on the street': 'Парковка на улице',
    'free parking on street': 'Парковка на улице',
    'parking': 'Парковка',
    'free high speed wireless internet': 'Wi-Fi',
    'free internet access': 'Wi-Fi',
    'high speed internet': 'Wi-Fi',
    'wireless internet': 'Wi-Fi',
    'internet': 'Wi-Fi',
    'wi-fi': 'Wi-Fi',
    'kitchen': 'Кухня',
    'full kitchen': 'Кухня',
    'kitchenette': 'Кухня',
    'air conditioning': 'Кондиционер',
    'smart tv': 'Smart TV',
    'cable tv': 'ТВ',
    'tv': 'ТВ',
    'refrigerator': 'Холодильник',
    'fridge': 'Холодильник',
    'microwave': 'Микроволновка',
    'oven': 'Духовка',
    'dishwasher': 'Посудомоечная машина',
    'washing machine': 'Стиральная машина',
    'washer': 'Стиральная машина',
    'dryer': 'Сушилка',
    'balcony': 'Балкон',
    'gym': 'Фитнес-зал',
    'elevator': 'Лифт',
    'safe': 'Сейф',
    '24 hour security': 'Охрана 24/7',
    '24/7 security': 'Охрана 24/7',
    'hot water': 'Горячая вода',
    'hair dryer': 'Фен',
    'iron': 'Утюг',
    'cribs available': 'Детская кроватка',
    'high chair': 'Детское кресло',
    'laptop workspace': 'Рабочее место',
    'laptop friendly workspace': 'Рабочее место',
    'bathtub': 'Ванна',
    'pool view': 'Вид на бассейн',
    'village view': 'Вид на деревню',
    'sea view': 'Вид на море',
    'beach view': 'Вид на пляж',
    'pool towels': 'Пляжные полотенца',
    'bed linen & towels': 'Постельное бельё и полотенца',
    'bed linens': 'Постельное бельё',
    'towels provided': 'Полотенца',
    'toiletries': 'Косметика',
    'coffee maker': 'Кофемашина',
    'stove': 'Плита',
    'gas stove': 'Газовая плита',
    'induction cooktop': 'Индукционная плита',
    'cooker': 'Плита',
    'dining area': 'Столовая зона',
    'dining table': 'Стол',
    'seating area': 'Зона отдыха',
    'long term stays': 'Длительное проживание',
    'non-smoking': 'Некурящий',
}

# Приоритетный порядок ключевых удобств в выводе
PRIORITY = [
    'Частный бассейн', 'Общий бассейн', 'Бассейн',
    'Wi-Fi', 'Бесплатная парковка', 'Парковка', 'Парковка на улице',
    'Кухня', 'Кондиционер', 'Smart TV', 'ТВ',
    'Фитнес-зал', 'Лифт', 'Балкон', 'Охрана 24/7', 'Сейф',
    'Холодильник', 'Микроволновка', 'Духовка', 'Посудомоечная машина',
    'Стиральная машина', 'Вид на море', 'Вид на пляж', 'Вид на бассейн',
    'Рабочее место', 'Детская кроватка', 'Детское кресло',
]


def norm(s):
    return re.sub(r'\s+', ' ', (s or '')).strip().lower()


def extract_summary(text):
    """Извлекает 'N спальн. | M ванн. | до X гостей' (уже на русском)."""
    beds = re.search(r'(\d+)\s*(?:\w+\s+){0,2}?(?:спальн|спальни|спальня|спальн\.)', text)
    baths = re.search(r'(\d+)\s*(?:\w+\s+){0,2}?(?:ванн|ванных|ванная|ванн\.)', text)
    guests = re.search(r'до\s*(\d+)\s*гостей', text)
    parts = []
    if beds:
        parts.append('%s спальн.' % beds.group(1))
    if baths:
        parts.append('%s ванн.' % baths.group(1))
    if guests:
        parts.append('до %s гостей' % guests.group(1))
    return ' | '.join(parts)


def extract_pool(text):
    low = text.lower()
    if ('pool' not in low) and ('бассейн' not in low):
        return None
    if 'private pool' in low or 'частный бассейн' in low:
        base = 'Частный бассейн'
    elif 'communal pool' in low or 'общий бассейн' in low or 'outdoor pool' in low:
        base = 'Общий бассейн'
    else:
        base = 'Бассейн'
    size = None
    m = re.search(r'(\d+)\s*(?:meters?|metres?|м)\s*(?:private\s*)?pool', low)
    if m:
        size = '%s м' % m.group(1)
    if not size:
        m = re.search(r'pool[^.]{0,40}?(\d+)\s*(?:sqm|sq\.?m|м²|square)', low)
        if m:
            size = '%s м²' % m.group(1)
    return base + (' ' + size if size else '')


def extract_distance(text):
    """Первое вхождение 'N min(utes) to X' / 'N minutes away'."""
    m = re.search(r'(\d+)\s*(?:min|minutes|мин)\s*(?:to|away|до)\s+([A-Za-zА-Яа-я ]+?)(?:\s+Beach|\s+beach|\.|,|$)', text)
    if m:
        return '%s мин' % m.group(1)
    m = re.search(r'about\s+(\d+)\s*minutes', text, re.I)
    if m:
        return '~%s мин' % m.group(1)
    return None


def extract_amenities(text):
    """Находит строку 'Аmenities:' / 'Amenities:' и переводит ключевые пункты."""
    m = re.search(r'[АA]menities:\s*(.+?)(?:\n\n|\nРасположение|\nОсобенности|$)', text, re.S)
    found = set()
    if m:
        for item in re.split(r',', m.group(1)):
            key = norm(item)
            if key in AMENITY_RU:
                found.add(AMENITY_RU[key])
    # Также ловим частный/общий бассейн из основного текста
    pool = extract_pool(text)
    if pool:
        found.add(pool)
    # Упорядочиваем по приоритету
    ordered = [p for p in PRIORITY if p in found]
    extra = [f for f in found if f not in ordered]
    return ordered + extra


def extract_included(text):
    low = text.lower()
    inc = []
    if re.search(r'electricity\s+and\s+water\s+usage\s+is\s+included', low) or \
       re.search(r'electricity\s+and\s+water\s+included', low):
        inc.append('Электричество и вода')
    elif re.search(r'electricity\s+is\s+included', low):
        inc.append('Электричество')
    if re.search(r'housekeeping', low) or re.search(r'уборк', low):
        inc.append('Уборка и смена белья')
    if re.search(r'pool\s+and\s+garden\s+maintenance', low) or re.search(r'уход за (бассейном|садом)', low):
        inc.append('Уход за бассейном и садом')
    if re.search(r'welcome\s+toiletries', low):
        inc.append('Косметика при заезде')
    return inc


def build_description(row):
    name = (row.get('name') or '').strip()
    category = (row.get('category') or '').strip()
    beach = (row.get('beach') or '').strip()
    desc = (row.get('description') or '')

    summary = extract_summary(desc)
    pool = extract_pool(desc)
    dist = extract_distance(desc)
    amenities = extract_amenities(desc)
    included = extract_included(desc)

    lines = []
    lines.append('🏝️ %s' % name)
    lines.append('')

    if summary:
        lines.append('🛏️ %s' % summary)
    if pool:
        lines.append('🏊 %s' % pool)
    loc = []
    if beach:
        loc.append(beach.split(',')[0].strip())
    if dist:
        loc.append('%s до пляжа' % dist)
    if loc:
        lines.append('📍 %s' % ', '.join(loc))
    lines.append('')

    # Удобства — одной строкой через • (без дубля бассейна)
    pool_words = {'Бассейн', 'Частный бассейн', 'Общий бассейн'}
    key_amen = [a for a in amenities if a not in pool_words]
    # Дедупликация синонимов: оставляем более конкретный вариант
    if 'Бесплатная парковка' in key_amen:
        key_amen = [a for a in key_amen if a not in ('Парковка', 'Парковка на улице')]
    elif 'Парковка на улице' in key_amen:
        key_amen = [a for a in key_amen if a != 'Парковка']
    if 'Smart TV' in key_amen:
        key_amen = [a for a in key_amen if a != 'ТВ']
    if key_amen:
        lines.append('Удобства: ' + ' · '.join(key_amen[:8]))
    if included:
        lines.append('Включено: ' + ' · '.join(included))

    text = '\n'.join(lines).strip()

    # Жёсткий лимит 1024
    if len(text) > LIMIT:
        text = text[:LIMIT - 1].rstrip() + '…'
    return text


def main():
    dry = '--dry' in sys.argv
    wb = openpyxl.load_workbook(XLSX)
    ws = wb[SHEET]
    headers = [c.value for c in ws[1]]
    desc_col = headers.index('description') + 1

    out = []
    for r in range(2, ws.max_row + 1):
        row = {headers[i]: ws.cell(row=r, column=i + 1).value for i in range(len(headers))}
        if not (row.get('description') or '').strip():
            continue
        new = build_description(row)
        if dry:
            out.append('### Row %d | id=%s | old_len=%d -> new_len=%d\n%s\n' % (
                r, row.get('id'), len(row.get('description') or ''), len(new), new))
        else:
            ws.cell(row=r, column=desc_col).value = new

    if dry:
        with open('tools/preview.txt', 'w', encoding='utf-8') as f:
            f.write('\n'.join(out))
        print('Wrote preview to tools/preview.txt (%d rows)' % len(out))
    else:
        wb.save(XLSX)
        print('Saved %d rows to %s' % (ws.max_row - 1, XLSX))


if __name__ == '__main__':
    main()
