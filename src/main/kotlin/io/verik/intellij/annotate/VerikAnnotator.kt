/*
 * Copyright (c) 2022 Francis Wang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.verik.intellij.annotate

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class VerikAnnotator : Annotator {

    private val annotatedKeywords = listOf("unknown", "floating")

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is KtOperationReferenceExpression -> annotateInfixFunctions(element, holder)
            is KtTypeElement -> annotateCardinalTypeElements(element, holder)
            is KtSimpleNameExpression -> annotateKeywords(element, holder)
            is KtStringTemplateExpression -> annotateBitLiterals(element, holder)
        }
    }

    private fun annotateInfixFunctions(
        operationReferenceExpression: KtOperationReferenceExpression,
        holder: AnnotationHolder
    ) {
        if (!operationReferenceExpression.isConventionOperator()) {
            val operationSignTokenType = operationReferenceExpression.operationSignTokenType
            if (operationSignTokenType !is KtSingleValueToken) {
                holder
                    .newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .textAttributes(KotlinHighlightingColors.EXTENSION_FUNCTION_CALL)
                    .create()
            }
        }
    }

    private fun annotateCardinalTypeElements(typeElement: KtTypeElement, holder: AnnotationHolder) {
        val text = typeElement.text
        if (text.startsWith("`") && text.endsWith("`")) {
            val trimmedText = text.substring(1, text.length - 1)
            if (trimmedText.toIntOrNull() != null || trimmedText == "*") {
                splitAnnotate(typeElement, holder, KotlinHighlightingColors.NUMBER)
            }
        }
    }

    private fun annotateKeywords(simpleNameExpression: KtSimpleNameExpression, holder: AnnotationHolder) {
        if (simpleNameExpression.getReferencedName() in annotatedKeywords) {
            splitAnnotate(simpleNameExpression, holder, KotlinHighlightingColors.KEYWORD)
        }
    }

    private fun annotateBitLiterals(stringTemplateExpression: KtStringTemplateExpression, holder: AnnotationHolder) {
        val callExpression = stringTemplateExpression.parent.parent.parent
        if (callExpression !is KtCallExpression)
            return
        val referenceExpression = callExpression.referenceExpression()
        if (referenceExpression is KtReferenceExpression && referenceExpression.text in listOf("u", "s")) {
            splitAnnotate(stringTemplateExpression, holder, KotlinHighlightingColors.NUMBER)
        }
    }

    private fun splitAnnotate(element: PsiElement, holder: AnnotationHolder, textAttributesKey: TextAttributesKey) {
        val startOffset = element.startOffset
        val endOffset = element.endOffset
        val startTextRange = TextRange(startOffset, startOffset)
        val endTextRange = TextRange(startOffset + 1, endOffset)
        holder
            .newSilentAnnotation(HighlightSeverity.INFORMATION)
            .textAttributes(textAttributesKey)
            .range(startTextRange)
            .create()
        holder
            .newSilentAnnotation(HighlightSeverity.INFORMATION)
            .textAttributes(textAttributesKey)
            .range(endTextRange)
            .create()
    }
}
