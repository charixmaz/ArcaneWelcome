"""Dependency-free smoke check of SQL extracted from the actual Java store.
Does not compile Java or replace Gradle/JUnit and live Paper verification.
"""
import re
import sqlite3
import tempfile
from pathlib import Path
source = (Path(__file__).parent / 'src/main/java/dev/arcane/welcome/Store.java').read_text()
statements = re.findall(r'"((?:CREATE TABLE|INSERT OR IGNORE|UPDATE|SELECT|PRAGMA)[^"\n]+)"', source)
def sql(prefix):
    return next(s for s in statements if s.startswith(prefix))
with tempfile.TemporaryDirectory() as tmp:
    file = Path(tmp) / 'welcome.db'
    db = sqlite3.connect(file)
    for s in statements:
        if s.startswith(('CREATE TABLE', 'PRAGMA')):
            db.execute(s)
    insert = sql('INSERT OR IGNORE INTO players')
    claim = sql('INSERT OR IGNORE INTO claims')
    db.execute(insert, ('new', 'Steve', 100, 400))
    assert db.execute(claim, ('helper', 'new')).rowcount == 1
    assert db.execute(claim, ('helper', 'new')).rowcount == 0
    db.commit()
    db.close()
    db = sqlite3.connect(file)
    db.execute(insert, ('new', 'Steve', 999, 2000))
    assert db.execute('SELECT starts,ends FROM players WHERE id=?', ('new',)).fetchone() == (100, 400)
    assert db.execute(claim, ('helper', 'new')).rowcount == 0
    assert db.execute(claim, ('other', 'new')).rowcount == 1
    db.execute(insert, ('old', 'Old', 0, 0))
    assert db.execute('SELECT ends FROM players WHERE id=?', ('old',)).fetchone()[0] == 0
    db.execute(sql('UPDATE claims SET status'), ('failed', 'helper', 'new'))
    assert db.execute(claim, ('helper', 'new')).rowcount == 0
    db.commit()
    db.close()
print('PASS: persisted timestamps, duplicate blocking, independent helpers, returning players, failed-claim blocking')
