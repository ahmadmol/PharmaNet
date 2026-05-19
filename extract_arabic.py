import sys
import codecs

filepath = r'feature/profile/src/main/kotlin/com/pharmalink/feature/profile/ProfileScreen.kt'
with codecs.open(filepath, 'r', 'utf-8') as f:
    lines = f.readlines()

# Show lines with Arabic content (non-ASCII)
for i, line in enumerate(lines, 1):
    if any(ord(c) > 127 for c in line):
        print(f'{i}: {line.rstrip()}')
