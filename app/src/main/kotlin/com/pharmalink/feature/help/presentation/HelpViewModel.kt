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
                            unreadNotifications = if (profile.notificationsEnabled) {
                                notifications.count { !it.read }
                            } else {
                                0
                            },
                            complianceAttentionCount = compliance.alerts.size + compliance.documents.count {
                                it.status != com.pharmalink.domain.model.ComplianceDocumentStatus.VALID
                            },
                            channels = listOf(
                                SupportChannel(
                                    title = "مركز العمليات",
                                    detail = "+966 800 100 4422",
                                    availability = "متاح خلال 15 دقيقة في أوقات العمل",
                                    guidance = "للاستفسارات التشغيلية عن حالة الطلبات أو متابعة الشحنات المعلّقة.",
                                    type = SupportChannelType.Operations,
                                ),
                                SupportChannel(
                                    title = "البريد الإلكتروني",
                                    detail = "support@pharmalink.sa",
                                    availability = "متاح على مدار الساعة",
                                    guidance = "استخدمه للطلبات العامة ومشاركة المستندات أو تفاصيل المشكلة.",
                                    type = SupportChannelType.Email,
                                ),
                                SupportChannel(
                                    title = "فريق الامتثال",
                                    detail = "compliance@pharmalink.sa",
                                    availability = "أيام العمل من 8 ص إلى 5 م",
                                    guidance = "للاستفسارات المتعلقة بالتراخيص والسياسات أو الوثائق التنظيمية.",
                                    type = SupportChannelType.Compliance,
                                ),
                            ),
                            guides = listOf(
                                HelpGuide(
                                    title = "إعداد الحساب لأول مرة",
                                    description = "ابدأ من ملف التعريف وأكمل بيانات المنشأة ثم فعّل الإشعارات لضمان وصول التنبيهات.",
                                ),
                                HelpGuide(
                                    title = "متابعة حالة الطلبات",
                                    description = "راقب مراحل الطلب من لوحة الطلبات اليومية، وتحقق من التحديثات فور حدوثها.",
                                ),
                                HelpGuide(
                                    title = "الإبلاغ عن مشكلة تقنية",
                                    description = "التقط وصفاً واضحاً للمشكلة، وأرفق لقطة شاشة إن أمكن، ثم تواصل مع الدعم لتسريع المعالجة.",
                                ),
                            ),
                            faq = listOf(
                                HelpFaqItem(
                                    question = "كيف أتابع حالة طلبي؟",
                                    answer = "يمكنك فتح شاشة الطلبات ومراجعة حالة كل طلب مع آخر تحديث مسجل على الطلب.",
                                ),
                                HelpFaqItem(
                                    question = "ماذا أفعل عند عدم وصول إشعار؟",
                                    answer = "تحقق من إعدادات الإشعارات داخل التطبيق، ثم تأكد من منح التطبيق صلاحية الإشعارات على جهازك.",
                                ),
                                HelpFaqItem(
                                    question = "كيف أطلب دعماً تقنياً؟",
                                    answer = "استخدم قسم التواصل أو البريد المخصص للدعم، مع وصف واضح للمشكلة وأي تفاصيل تساعد الفريق.",
                                ),
                            ),
                        ),
                    ),
                )
            }.collect { _uiState.value = it }
        }
    }
}
