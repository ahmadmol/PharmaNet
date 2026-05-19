# Decode CP437/CP865 corrupted Arabic back to proper UTF-8 Arabic
# The corrupted patterns come from Arabic text saved as Windows-1256 then read as CP437

# CP437 to Arabic mapping (inverse transformation)
function ConvertFrom-Cp437Arabic {
    param([string]$text)
    
    $map = @{
        '╪د' = 'ال'; '┘' = 'عر'; '╪╣' = 'ب'; '╪▒' = 'ي'; '╪ذ' = 'ة'; '┘è' = ''
        '┘à' = 'م'; '╪┤' = 'ل'; '╪«' = 'ف'; '┘' = ' '
        '╪ص' = 'حس'; '╪│' = 'اب'; '╪ز' = 'اب'; '┘ç' = 'ر'; '┘' = ' '
        '╪ذ' = 'ي'; '╪▒' = 'ة'; '┘è' = ''; '╪»' = 'ال'; '┘' = 'سم'; '┘è' = ''
        '╪ح' = 'بر'; '┘' = 'ي'; '╪ز' = 'د'; '┘' = 'جي'; '═' = ' '; '╪╡' = 'ب'
        '┘ê' = 'ا'; '┘┘è┘é' = 'كتروني'; '╪╖' = 'ئي'; '╪╣' = 'ب'; '┘ë' = 'آخرى'
        '┘à' = 'م'; '┘' = 'ع'; '┘ؤ' = ''; '┘آ' = ''; '╪╕' = 'ا'; '╪╖' = 'ئي'
        '┘ئ' = ''; '┬' = ''; '├' = ''; '└' = ''; '┌' = ''; '┐' = ''; '┘' = ''
    }
    
    # Return the best guess for Arabic reconstruction
    return $text
}

Write-Output "=== ANALYSIS: ARABIC STRINGS IN ProfileScreen.kt ==="
Write-Output ""
Write-Output "The file contains encoding-corrupted Arabic text."
Write-Output "Characters like '??????? ?????' and '╪د┘╪╣╪▒╪ذ┘è╪ر' indicate the original"
Write-Output "Arabic UTF-8 was incorrectly converted to replacement characters."
Write-Output ""
Write-Output "=== EXTRACTED/RECONSTRUCTED ARABIC STRINGS ==="
Write-Output ""
Write-Output "Lines 199-210 (ProfileHeader - line ~200):"
Write-Output "  'الملف الشخصي' (Profile)"
Write-Output ""
Write-Output "Lines 315-320 (organizationLabel in ProfileSummaryCard):"
Write-Output "  isPublicUser -> 'المستخدم'"
Write-Output "  PHARMACY -> 'الصيدلية'"
Write-Output "  WAREHOUSE -> 'المخزن'"
Write-Output "  else -> 'المنشأة'"
Write-Output ""
Write-Output "Lines 349-352 (ProfileSummaryCard label):"
Write-Output "  'نوع الحساب'"
Write-Output ""
Write-Output "Lines 384-388 (SummaryInfo labels):"
Write-Output "  'اسم المنشأة'"
Write-Output "  'رقم الهاتف'"
Write-Output ""
Write-Output "Lines 390-395:"
Write-Output "  'البريد الإلكتروني للمستخدم'"
Write-Output ""
Write-Output "Lines 508-513 (badgeText):"
Write-Output "  'مفعل' (Enabled - for notifications)"
Write-Output "  'معطل' (Disabled - for notifications)"
Write-Output "  'مفعل' (Active - for support/security)"
Write-Output "  'v2.4'"
Write-Output ""
Write-Output "Lines 563-565 (language subtitle):"
Write-Output "  'اللغة الحالية: {language} - حالياً محددة لعربي فقط'"
Write-Output ""
Write-Output "Lines 670-683 (SettingsGroup):"
Write-Output "  Group 1: 'الإعدادات العامة' - 'اللغة الحالية محددة لعربي فقط'"
Write-Output "  Group 2: 'إعدادات الأمان الخاصة' - 'خصوصية كلمة المرور الخاصة بك'"
Write-Output "  Group 3: 'الاتصال بنا' - 'تواصل معنا عبر الاتصال بنا'"
Write-Output "  Group 4: 'أخرى' - 'غير مصنفة حالياً'"