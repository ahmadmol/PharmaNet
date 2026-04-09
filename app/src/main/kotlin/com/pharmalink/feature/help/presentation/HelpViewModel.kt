package com.pharmalink.feature.help.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.data.repository.PharmaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class SupportChannelType {
    Operations,
    Email,
    Compliance,
}

data class SupportChannel(
    val title: String,
    val detail: String,
    val availability: String,
    val guidance: String,
    val type: SupportChannelType,
)

data class HelpGuide(
    val title: String,
    val description: String,
)

data class HelpFaqItem(
    val question: String,
    val answer: String,
)

data class HelpContent(
    val pharmacyName: String,
    val unreadNotifications: Int,
    val complianceAttentionCount: Int,
    val channels: List<SupportChannel>,
    val guides: List<HelpGuide>,
    val faq: List<HelpFaqItem>,
)

data class HelpUiState(
    val screenState: ScreenState<HelpContent> = ScreenState.Loading,
)

@HiltViewModel
class HelpViewModel @Inject constructor(
    private val repository: PharmaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HelpUiState())
    val uiState: StateFlow<HelpUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeProfile(),
                repository.observeCompliance(),
                repository.observeNotifications(),
            ) { profile, compliance, notifications ->
                HelpUiState(
                    screenState = ScreenState.Success(
                        HelpContent(
                            pharmacyName = profile.pharmacyName,
                            unreadNotifications = notifications.count { !it.read },
                            complianceAttentionCount = compliance.alerts.size + compliance.documents.count {
                                it.status != com.pharmalink.domain.model.ComplianceDocumentStatus.VALID
                            },
                            channels = listOf(
                                SupportChannel(
                                    title = "الدعم التشغيلي",
                                    detail = "+966 800 100 4422",
                                    availability = "استجابة خلال 15 دقيقة في أوقات العمل",
                                    guidance = "للطلبات المتأخرة أو نقص المخزون أو المشاكل المرتبطة بالموردين.",
                                    type = SupportChannelType.Operations,
                                ),
                                SupportChannel(
                                    title = "بريد المساندة",
                                    detail = "support@pharmalink.sa",
                                    availability = "استجابة خلال ساعتين",
                                    guidance = "للملاحظات العامة ومتابعة الحالات غير العاجلة وإرفاق التفاصيل المكتوبة.",
                                    type = SupportChannelType.Email,
                                ),
                                SupportChannel(
                                    title = "تصعيد الامتثال",
                                    detail = "compliance@pharmalink.sa",
                                    availability = "مراجعة خلال يوم عمل",
                                    guidance = "للرخص المنتهية أو الوثائق الناقصة أو مراجعات الجهات التنظيمية.",
                                    type = SupportChannelType.Compliance,
                                ),
                            ),
                            guides = listOf(
                                HelpGuide(
                                    title = "متابعة الطلبات الحرجة",
                                    description = "ابدأ من شاشة الإشعارات ثم افتح الطلب أو الطلبية المرتبطة مباشرة لمعرفة آخر تحديث ومسؤول المتابعة.",
                                ),
                                HelpGuide(
                                    title = "تحديث وثائق الامتثال",
                                    description = "راجع تنبيهات انتهاء الصلاحية أولًا ثم جهّز الوثيقة البديلة. سيتم ربط الرفع والمراجعة داخل التطبيق في خطوة لاحقة.",
                                ),
                                HelpGuide(
                                    title = "التعامل مع نقص المخزون",
                                    description = "انتقل إلى المستودعات، قارن التوفر وزمن التوريد، ثم أنشئ طلبًا جديدًا من المسار الأنسب زمنيًا.",
                                ),
                            ),
                            faq = listOf(
                                HelpFaqItem(
                                    question = "كيف أتابع حالة الطلب؟",
                                    answer = "افتح شاشة الطلبات أو الإشعارات، ثم راجع آخر تحديث ووقت التوريد المتوقع والطلب المرتبط بالحالة.",
                                ),
                                HelpFaqItem(
                                    question = "ماذا أفعل عند قرب انتهاء وثيقة؟",
                                    answer = "افتح شاشة الامتثال، راجع التنبيه المرتبط، ثم جهّز الوثيقة البديلة قبل تاريخ الانتهاء ونسّق مع المسؤول الإداري.",
                                ),
                                HelpFaqItem(
                                    question = "كيف أبلغ عن مشكلة تشغيلية؟",
                                    answer = "جهّز رقم الطلب أو المورد أو المستودع، ثم استخدم قناة الدعم المناسبة مع وصف مختصر وواضح لما حدث ومتى بدأ.",
                                ),
                            ),
                        ),
                    ),
                )
            }.collect { _uiState.value = it }
        }
    }
}
