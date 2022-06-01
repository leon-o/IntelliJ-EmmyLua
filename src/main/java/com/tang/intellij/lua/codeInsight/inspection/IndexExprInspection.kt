/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.codeInsight.inspection

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*

class IndexExprInspection : StrictInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : LuaVisitor() {
            override fun visitIndexExpr(o: LuaIndexExpr) {
                super.visitIndexExpr(o)
                val context = SearchContext.get(o.project)

                if(o.dot!=null || o.exprList.size<2) return

                val prefixType = o.prefixExpr.guessType(context)
                val indexExpr = o.exprList[1]
                val indexType = indexExpr.guessType(context)
                if(!prefixType.isIndexableBy(indexType,context,true))
                    holder.registerProblem(indexExpr,"Can't index ${prefixType.displayName} with ${indexType.displayName}")
            }
        }
}