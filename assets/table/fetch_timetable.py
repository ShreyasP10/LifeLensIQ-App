"""
fetch_timetable.py
==================
Fetches the exact timetable details from 'TE-B.pdf' (Department of Computer Engineering,
Academic Year 2026-2027, SH 2026-ODD SEM, Class TE-Div:B).

Output:
  1. Full TE-Div:B timetable (exactly as printed in the PDF)
  2. Personalized timetable for: Batch B1 + Program Elective-1 (IP),
     without TE-Honor and without OE (Open Elective)

Usage:
    python fetch_timetable.py [path_to_pdf]
"""

import sys
import pdfplumber

PDF_PATH = sys.argv[1] if len(sys.argv) > 1 else 'TE-B.pdf'

# --- Slot / time layout (from PDF header rows) -------------------------------
SLOTS = [
    (1, '8:10-9:05'),
    (2, '9:05-10:00'),
    (3, '10:20-11:15'),
    (4, '11:15-12:10'),
    (5, '12:50-1:45'),
    (6, '1:45-2:40'),
    (7, '2:40-3:35'),
    (8, '3:35-4:30'),
]

BREAK_1 = '10:00-10:20  (Break 1)'
LUNCH = '12:10-12:50  (Lunch Break)'

# --- Raw timetable data, exactly as printed in TE-B.pdf ----------------------
# day -> slot_no -> (subject, room, faculty)  ;  'FREE' = empty cell
RAW_TABLE = {
    'MONDAY': {
        1: ('IP/DWM/DWM/CG', '201/202/203/204', 'AAG/AJK/TCK/BSG'),
        2: ('AISC', '203', 'MVC'),
        3: ('CN-Lab(B1)/SE-Lab(B2)/MDM-Lab(A3)', '314/316/307', 'SHM-SMP/SSA/MPK'),
        4: None,
        5: ('CN', '203', 'SHM-SMP'),          # PDF prints 'SSH-SMP' (typo for SHM-SMP)
        6: ('MDM', '203', 'SVF'),
        7: ('Industrial Training', '', ''),
        8: None,
    },
    'TUESDAY': {
        1: ('SE-Lab(B1)/MDM-Lab(B2)/CN-Lab(B3)', '310/307/314', 'SSA/MPK/SMP-SHM'),
        2: None,
        3: ('SE', '203', 'SSA'),
        4: ('MDM', '203', 'SVF'),
        5: ('IKS-Lab(B1/B2/B3)', '203', 'SMS'),  # PDF prints 'SS' (truncated SMS)
        6: None,
        7: ('IP/DWM/DWM/CG', '201/202/203/204', 'AAG/AJK/TCK/BSG'),
        8: ('TE-Honor-AIML/DS /CS (3:35-5:00)', '413-414/210/213-214', ''),
    },
    'WEDNESDAY': {
        1: ('AISC-Lab(B1)/CN-Lab(B2)/AISC-Lab(B3)', '311/314/312', 'MVC/SMP-SHM/SSD'),
        2: None,
        3: ('SE', '203', 'SSA'),
        4: ('CN', '203', 'SHM-SMP'),
        5: ('AISC', '203', 'MVC'),
        6: ('IKS', '203', 'SMS'),
        7: ('MDM', '203', 'SVF'),
        8: ('TE-Honor-AIML/DS /CS (3:35-4:30)', '413-414/210/213-214', ''),
    },
    'THURSDAY': {
        1: None,
        2: ('CN', '203', 'SHM-SMP'),
        3: ('AISC', '203', 'MVC'),
        4: ('SE', '203', 'SSA'),
        5: ('IKS', '203', 'SMS'),
        6: ('DWM-Lab(TE-B-Batch 1)', '316', 'AJK'),
        7: None,
        8: ('TE-Honor-AIML/DS /CS (3:35-5:00)', '413-414/210/213-214', ''),
    },
    'FRIDAY': {
        1: ('IP-Lab(Batch 1)/CG-Lab(Batch 1)', '310/312', 'AAG/BSG'),
        2: None,
        3: ('MDM-Lab(B1)/AISC-Lab(B2)/SE-Lab(B3)', '307/311/310', 'SVF/MVC/SSA'),
        4: None,
        5: ('IP/DWM/CG', '105/202/203/204', 'AAG/AJK/TCK/BSG'),
        6: ('OE', '203', ''),
        7: ('MENTORING', '', ''),
        8: ('OE', '203', ''),
    },
}

# Merged annotation cell under Friday row, as printed in the PDF:
FRIDAY_ANNOTATION = 'Industrial Training'

# --- Legend (abbreviation / faculty / subject / batch), exactly as printed ---
LEGEND = [
    ('SSA',   'Dr. Sukhada S. Aloni',                         'Software Engineering (SE)',                 'B1'),
    ('SHM',   'Dr. Sachin H. Malave',                         'Computer Network (CN)',                     'B2'),
    ('MVC',   'PROF. Manasi V. Chouk / PROF Suchita S. Dange','Artificial Intelligence & Soft Computing (AISC)', 'B3'),
    ('SVF/MPK', 'PROF. Selvin V.F. / Dr. Mamta P. Kurvey',    'Multidisiplinary Minor (MDM)',             ''),
    ('AAG/RRK', 'PROF Ashwini A. Gaikwad / Dr. Reshma R. Koli','Program Elective - 1 (Internet Programming) (IP)', ''),
    ('AJK/TCK', 'PROF. Archana J. Kotangale / Dr. Tanvi Kapdi','Program Elective- 1 (Data Warehousing & Mining) (DWM)', ''),
    ('BSG',   'Dr. Babita S. Gawate',                         'Program Elective- 1 (Computer Graphics) (CG)', ''),
    ('SMS',   'PROF. Sambhaji M. Shirsat',                    'Indian Knowledge System (IKS)',            ''),
    ('AJK',   'PROF. Archana J. Kotangale',                   'Quantitaive Aptitude',                     ''),
]

HEADER = {
    'department': 'DEPARTMENT OF COMPUTER ENGINEERING',
    'academic_year': 'Academic Year - 2026-2027 (SH 2026-ODD SEM)',
    'class': 'CLASS TIME TABLE TE-DIV:B',
    'advisor': 'CLASS ADVISOR: Prof. Sayali Poojari (w.e.f. 20 July 2026)',
    'tt_coordinator': 'Time Table Co-Ordinator: Prof. Vishakha Chaudhari / Dr. Reshma Koli',
    'hod': 'HOD, Computer Engineering: Dr. Sachin Malave',
}

# --- Personalization settings ------------------------------------------------
BATCH = 'B1'
ELECTIVE = 'IP'
HAS_TE_HONOR = False
HAS_OE = False

# --- Batch/elective pickers for lab and elective slots ------------------------
import re


def _split_spec(spec):
    """Split 'A(B1)/B(B2)/C(B3)' on '/' that is OUTSIDE parentheses."""
    parts, depth, current = [], 0, ''
    for ch in spec:
        if ch == '(':
            depth += 1
        elif ch == ')':
            depth -= 1
        if ch == '/' and depth == 0:
            parts.append(current)
            current = ''
        else:
            current += ch
    if current:
        parts.append(current)
    return parts


def pick(spec, batch=BATCH, elective=ELECTIVE):
    """Pick the option relevant to the given batch/elective from a 'X(B1)/Y(B2)' spec."""
    if spec is None:
        return None
    if '/' in spec:
        for part in _split_spec(spec):
            tag = part[part.index('(') + 1: part.index(')')] if '(' in part else ''
            if tag and (batch in tag or (elective in tag and 'Batch' in tag)):
                return part
        return spec  # no match -> return as-is
    return spec


def personalize(day_data, slot):
    """Return personalized entry for a slot: (subject, room, faculty, note)."""
    raw = day_data.get(slot)
    if raw is None:
        return None
    subject, room, faculty = raw

    if subject == 'TE-Honor-AIML/DS /CS (3:35-5:00)' or subject == 'TE-Honor-AIML/DS /CS (3:35-4:30)':
        if not HAS_TE_HONOR:
            return ('FREE (no TE-Honor)', '', '', 'TE-Honor slot - not applicable to you')
    if subject == 'OE':
        if not HAS_OE:
            return ('FREE (no OE)', '', '', 'Open Elective slot - not applicable to you')

    if subject == 'Industrial Training':
        return ('INDUSTRIAL TRAINING', '', '', 'Training / mentoring session')
    if subject == 'MENTORING':
        return ('MENTORING / TRAINING', '', '', 'Training & mentoring session (as per your note)')

    # Elective slots
    if 'IP/DWM/DWM/CG' in subject or 'IP/DWM/CG' in subject:
        return (f'{ELECTIVE} (Program Elective-1)', room, 'AAG/RRK', '')
    if 'IP-Lab(Batch 1)/CG-Lab(Batch 1)' in subject:
        return (f'{ELECTIVE}-Lab (Batch 1)', room, 'AAG', '')
    if 'DWM-Lab(TE-B-Batch 1)' in subject:
        return (subject, room, faculty, 'PDF prints DWM-Lab for TE-B Batch 1')

    # Batch split lab slots -> pick B1 branch
    picked = pick(subject)
    return (picked, room, faculty, '')


def print_full_table():
    print('=' * 100)
    print(HEADER['department'])
    print(HEADER['academic_year'])
    print(HEADER['class'])
    print(HEADER['advisor'])
    print('=' * 100)
    print(f"{'DAY':<10}{'SLOT':<6}{'TIME':<14}{'SUBJECT':<44}{'ROOM':<16}{'FACULTY'}")
    print('-' * 100)
    for day, data in RAW_TABLE.items():
        for slot, time in SLOTS:
            if slot == 3:
                print(f"{day:<10}{'B1':<6}{BREAK_1:<14}{'BREAK':<44}{'':<16}{''}")
            if slot == 5:
                print(f"{'':<10}{'':<6}{LUNCH:<14}{'LUNCH BREAK':<44}{'':<16}{''}")
            entry = data.get(slot)
            if entry is None:
                print(f"{day if slot == 1 else '':<10}{slot:<6}{time:<14}{'FREE':<44}{'':<16}{''}")
            else:
                subj, room, fac = entry
                print(f"{day if slot == 1 else '':<10}{slot:<6}{time:<14}{subj:<44}{room:<16}{fac}")
        if day == 'FRIDAY':
            print(f"{'':<10}{'':<6}{'':<14}{'(merged cell) ' + FRIDAY_ANNOTATION:<44}{'':<16}{''}")
        print('-' * 100)


def print_personal_table():
    print('\n' + '=' * 100)
    print(f'PERSONALIZED TIMETABLE - Batch {BATCH} | Elective: {ELECTIVE} (IP)')
    print('TE-Honor: NOT TAKEN   |   OE (Open Elective): NOT TAKEN')
    print('=' * 100)
    print(f"{'DAY':<10}{'SLOT':<6}{'TIME':<14}{'SUBJECT':<44}{'ROOM':<16}{'FACULTY'}")
    print('-' * 100)
    for day, data in RAW_TABLE.items():
        for slot, time in SLOTS:
            if slot == 3:
                print(f"{day:<10}{'B1':<6}{BREAK_1:<14}{'BREAK':<44}{'':<16}{''}")
            if slot == 5:
                print(f"{'':<10}{'':<6}{LUNCH:<14}{'LUNCH BREAK':<44}{'':<16}{''}")
            entry = personalize(data, slot)
            if entry is None:
                print(f"{day if slot == 1 else '':<10}{slot:<6}{time:<14}{'FREE PERIOD':<44}{'':<16}{''}")
            else:
                subj, room, fac, note = entry
                if note:
                    subj = f'{subj}  ({note})'
                print(f"{day if slot == 1 else '':<10}{slot:<6}{time:<14}{subj:<44}{room:<16}{fac}")
        if day == 'FRIDAY':
            print(f"{'':<10}{'':<6}{'':<14}{'Industrial Training (annotated cell under Friday row)':<44}{'':<16}{''}")
        print('-' * 100)


def print_legend():
    print(f"\n{'Abbreviation':<12}{'Faculty':<52}{'SUBJECT':<50}{'Batch'}")
    print('-' * 100)
    for abbr, fac, subj, batch in LEGEND:
        print(f'{abbr:<12}{fac:<52}{subj:<50}{batch}')


def main():
    with pdfplumber.open(PDF_PATH) as pdf:
        if len(pdf.pages) >= 1:
            text = pdf.pages[0].extract_text()
            if 'CLASS TIME TABLE TE-DIV:B' in text:
                print(f'[OK] Source verified: {PDF_PATH} contains the TE-Div:B timetable.\n')

    print_full_table()
    print_personal_table()
    print_legend()


if __name__ == '__main__':
    main()
