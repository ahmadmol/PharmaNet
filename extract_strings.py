import codecs

# Read the Kotlin file
with codecs.open('feature/profile/src/main/kotlin/com/pharmalink/feature/profile/ProfileScreen.kt', 'r', 'utf-8') as f:
    content = f.read()

# Extract all quoted strings that contain Arabic
import re
arabic_strings = re.findall(r'"([\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]+(?: [\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]+)*)"', content)

print("Found Arabic strings:")
for s in arabic_strings:
    print(f"- {s}")
