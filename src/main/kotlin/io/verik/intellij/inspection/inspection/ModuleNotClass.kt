/*
 * Copyright (c) 2021 Francis Wang
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

package io.verik.intellij.inspection.inspection

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import io.verik.intellij.inspection.common.AbstractVerikInspection
import org.jetbrains.kotlin.psi.classOrObjectVisitor

class ModuleNotClass : AbstractVerikInspection() {

    override fun getID(): String {
        return "VerikModuleNotClass"
    }

    override fun getDisplayName(): String {
        return "Module not class"
    }

    override fun getStaticDescription(): String {
        return "Reports modules that should be declared as class but are not."
    }

    override fun getDefaultLevel(): HighlightDisplayLevel {
        return HighlightDisplayLevel.ERROR
    }

    override fun buildEnabledVisitor(holder: ProblemsHolder): PsiElementVisitor {
        return classOrObjectVisitor { classOrObject ->
            val declarationKeyword = classOrObject.getDeclarationKeyword()
            val isModule = classOrObject.superTypeListEntries.any { it.text.removeSuffix("()") == "Module" }
            if (declarationKeyword != null && isModule) {
                val isObject = declarationKeyword.text == "object"
                val isSimTop = classOrObject.annotationEntries.any { it.shortName.toString() == "SimTop" }
                if (isObject && !isSimTop) {
                    holder.registerProblem(declarationKeyword, "Module must be declared as class")
                }
            }
        }
    }
}
