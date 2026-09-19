package com.rork.diariointimo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rork.diariointimo.i18n.LocalStrings
import com.rork.diariointimo.i18n.StringsDe
import com.rork.diariointimo.i18n.StringsEn
import com.rork.diariointimo.i18n.StringsEs
import com.rork.diariointimo.i18n.StringsFr
import com.rork.diariointimo.i18n.StringsIt
import com.rork.diariointimo.i18n.StringsPt
import com.rork.diariointimo.ui.components.InkIconButton
import com.rork.diariointimo.ui.components.PaperBackground
import com.rork.diariointimo.ui.components.SealedButton
import com.rork.diariointimo.ui.theme.DiaryDim
import com.rork.diariointimo.ui.theme.LocalDiaryColors
import com.rork.diariointimo.ui.theme.SansField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PRESET_COUNT = 6

/**
 * All preset questions across all supported languages. Used to resolve the
 * correct question index even when the user changes the app language after
 * setting up the recovery question.
 */
private val ALL_QUESTIONS: List<List<String>> = listOf(
    listOf(StringsEs.question1, StringsEn.question1, StringsPt.question1, StringsFr.question1, StringsIt.question1, StringsDe.question1),
    listOf(StringsEs.question2, StringsEn.question2, StringsPt.question2, StringsFr.question2, StringsIt.question2, StringsDe.question2),
    listOf(StringsEs.question3, StringsEn.question3, StringsPt.question3, StringsFr.question3, StringsIt.question3, StringsDe.question3),
    listOf(StringsEs.question4, StringsEn.question4, StringsPt.question4, StringsFr.question4, StringsIt.question4, StringsDe.question4),
    listOf(StringsEs.question5, StringsEn.question5, StringsPt.question5, StringsFr.question5, StringsIt.question5, StringsDe.question5),
    listOf(StringsEs.question6, StringsEn.question6, StringsPt.question6, StringsFr.question6, StringsIt.question6, StringsDe.question6),
)

/**
 * Finds the preset index (0..5) for a stored question text that may be in
 * any supported language. Returns PRESET_COUNT if the question is custom.
 */
internal fun resolveQuestionIndex(storedQuestion: String): Int {
    if (storedQuestion.isBlank()) return 0
    ALL_QUESTIONS.forEachIndexed { index, variants ->
        if (variants.any { it.equals(storedQuestion, ignoreCase = true) }) return index
    }
    return PRESET_COUNT
}

/**
 * Edit the recovery method: the personal question, whose answer is re-entered
 * to be re-hashed.
 */
@Composable
fun RecoveryScreen(
    question: String?,
    onSave: (question: String?, answer: String?) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val colors = LocalDiaryColors.current
    val scope = rememberCoroutineScope()
    val presets = remember {
        listOf(
            strings.question1, strings.question2, strings.question3,
            strings.question4, strings.question5, strings.question6
        )
    }

    var useQuestion by remember { mutableStateOf(question != null) }
    var questionIndex by remember {
        mutableIntStateOf(
            if (question != null) {
                val localIndex = presets.indexOf(question)
                if (localIndex >= 0) localIndex else resolveQuestionIndex(question)
            } else 0
        )
    }
    var customQuestion by remember {
        mutableStateOf(
            if (question != null && presets.indexOf(question) < 0 && resolveQuestionIndex(question) >= PRESET_COUNT) {
                question
            } else ""
        )
    }
    var answer by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(true) }

    val chosenQuestion: String? = when {
        questionIndex < PRESET_COUNT -> presets[questionIndex]
        else -> customQuestion.trim().takeIf { it.isNotEmpty() }
    }

    PaperBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DiaryDim.headerPad, vertical = DiaryDim.space3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InkIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = strings.back,
                    onClick = onBack
                )
                Spacer(Modifier.width(DiaryDim.space3))
                Text(
                    text = strings.recoverySection,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.ink
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DiaryDim.screenPad)
            ) {
                Text(
                    text = strings.recoveryIntro,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkWhisper
                )
                Spacer(Modifier.height(DiaryDim.space4))

                RecoveryToggle(strings.recoveryMethodQuestion, useQuestion) {
                    useQuestion = it
                    message = null
                }
                AnimatedVisibility(
                    visible = useQuestion,
                    enter = fadeIn(tween(240)) + expandVertically(tween(280)),
                    exit = fadeOut(tween(180)) + shrinkVertically(tween(220))
                ) {
                    Column {
                        Spacer(Modifier.height(DiaryDim.space3))
                        presets.forEachIndexed { index, preset ->
                            QuestionRow(
                                label = preset,
                                selected = questionIndex == index,
                                onClick = {
                                    questionIndex = index
                                    message = null
                                }
                            )
                            Spacer(Modifier.height(DiaryDim.space2))
                        }
                        QuestionRow(
                            label = strings.customQuestionLabel,
                            selected = questionIndex == PRESET_COUNT,
                            onClick = {
                                questionIndex = PRESET_COUNT
                                message = null
                            }
                        )
                        if (questionIndex == PRESET_COUNT) {
                            Spacer(Modifier.height(DiaryDim.space2))
                            RecoveryField(
                                value = customQuestion,
                                onValueChange = {
                                    customQuestion = it
                                    message = null
                                },
                                placeholder = strings.customQuestionLabel
                            )
                        }
                        Spacer(Modifier.height(DiaryDim.space2))
                        RecoveryField(
                            value = answer,
                            onValueChange = {
                                answer = it
                                message = null
                            },
                            placeholder = strings.answerLabel
                        )
                    }
                }

                Spacer(Modifier.height(DiaryDim.space5))
                Box(modifier = Modifier.heightIn(min = DiaryDim.space6)) {
                    Text(
                        text = message.orEmpty(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontStyle = FontStyle.Italic
                        ),
                        color = if (isError) colors.wax else colors.gold
                    )
                }
                Spacer(Modifier.height(DiaryDim.space2))
                SealedButton(
                    label = strings.save,
                    onClick = {
                        when {
                            !useQuestion -> {
                                isError = true
                                message = strings.recoveryNeeded
                            }
                            chosenQuestion == null -> {
                                isError = true
                                message = strings.recoveryNeeded
                            }
                            answer.trim().length < 2 -> {
                                isError = true
                                message = strings.answerLabel
                            }
                            else -> scope.launch {
                                onSave(chosenQuestion, answer)
                                isError = false
                                message = strings.recoverySaved
                                delay(750)
                                onBack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(DiaryDim.space10))
            }
        }
    }
}

/** Paper toggle used to switch a recovery method on and off. */
@Composable
private fun RecoveryToggle(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val colors = LocalDiaryColors.current
    Surface(
        onClick = { onChecked(!checked) },
        shape = RoundedCornerShape(DiaryDim.radiusPaper),
        color = colors.paperLight.copy(alpha = 0.85f),
        border = BorderStroke(DiaryDim.dividerHeight, colors.edge),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DiaryDim.space4, vertical = DiaryDim.space3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(DiaryDim.iconMedium)
                    .background(
                        if (checked) colors.gold else colors.paperLight,
                        RoundedCornerShape(50)
                    )
            )
        }
    }
}

/** One selectable preset question, or the custom question option. */
@Composable
private fun QuestionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalDiaryColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DiaryDim.radiusPaper),
        color = if (selected) colors.ink else colors.paperLight.copy(alpha = 0.7f),
        border = BorderStroke(
            DiaryDim.dividerHeight,
            if (selected) colors.gold.copy(alpha = 0.6f) else colors.edge
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = if (selected) FontStyle.Italic else FontStyle.Normal
            ),
            color = if (selected) colors.paperLight else colors.inkSoft,
            modifier = Modifier.padding(horizontal = DiaryDim.space3, vertical = DiaryDim.space3)
        )
    }
}

/** Single-line paper field for visible recovery text. */
@Composable
private fun RecoveryField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val colors = LocalDiaryColors.current
    Surface(
        shape = RoundedCornerShape(DiaryDim.radiusInput),
        color = colors.paperLight.copy(alpha = 0.85f),
        border = BorderStroke(DiaryDim.dividerHeight, colors.edge),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DiaryDim.fieldHeight)
                .padding(horizontal = DiaryDim.fieldHorizontalPad),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = SansField,
                    color = colors.inkWhisper
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = SansField.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.gold),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
        }
    }
}
