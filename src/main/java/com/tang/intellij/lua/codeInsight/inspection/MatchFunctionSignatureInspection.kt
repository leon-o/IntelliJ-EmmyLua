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

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*

class MatchFunctionSignatureInspection : StrictInspection() {
    data class ConcreteTypeInfo(val param: LuaExpr, val ty: ITy)
    override fun buildVisitor(myHolder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor =
            object : LuaVisitor() {
                override fun visitIndexExpr(o: LuaIndexExpr) {
                    super.visitIndexExpr(o)
                    val id = o.id
                    if (id != null) {
                        if (o.parent is LuaCallExpr && o.colon != null) {
                            // Guess parent types
                            val context = SearchContext.get(o.project)
                            o.exprList.forEach { expr ->
                                if (expr.guessType(context) == Ty.NIL) {
                                    // If parent type is nil add error
                                    myHolder.registerProblem(expr, "Trying to index a nil type.")
                                }
                            }
                        }
                    }
                }

                override fun visitCallExpr(o: LuaCallExpr) {
                    super.visitCallExpr(o)

                    val searchContext = SearchContext.get(o.project)
                    val prefixExpr = o.expr
                    val type = prefixExpr.guessType(searchContext)

                    val concreteParams = o.argList
                    val concreteTypes = mutableListOf<ConcreteTypeInfo>()
                    concreteParams.forEachIndexed { index, luaExpr ->
                        val ty = luaExpr.guessType(searchContext)
                        if (ty is TyTuple) {
                            if (index == concreteParams.lastIndex) {
                                concreteTypes.addAll(ty.list.map { ConcreteTypeInfo(luaExpr, it) })
                            } else {
                                concreteTypes.add(ConcreteTypeInfo(luaExpr, ty.list.first()))
                            }
                        } else concreteTypes.add(ConcreteTypeInfo(luaExpr, ty))
                    }

                    if (type is TyFunction) {
                        val perfectSig = type.findPerfectSignature(concreteTypes.map { it.ty },searchContext)
                        annotateCall(o, perfectSig,concreteTypes, searchContext)
                    } else if (prefixExpr is LuaIndexExpr) {
                        // Get parent type
                        val parentType = prefixExpr.guessParentType(searchContext)
                        if (parentType is TyClass) {
                            val fType = prefixExpr.name?.let { parentType.findSuperMember(it, searchContext) }
                            if (fType == null)
                                myHolder.registerProblem(o, "Unknown function '%s'.".format(prefixExpr.lastChild.text))
                        }
                    } else if (type == Ty.NIL) {
                        myHolder.registerProblem(o, "Unknown function '%s'.".format(prefixExpr.lastChild.text))
                    }
                }

                private fun annotateCall(call: LuaCallExpr, signature: IFunSignature,concreteTypes:List<ConcreteTypeInfo>, searchContext: SearchContext) {

                    var nArgs = 0
                    val substitutor = call.createSubstitutor(signature, searchContext)
                    signature.processArgs(call) { i, pi ->
                        nArgs = i + 1
                        val typeInfo = concreteTypes.getOrNull(i)
                        if (typeInfo == null) {
                            myHolder.registerProblem(call.lastChild.lastChild, "Missing argument: ${pi.name}: ${pi.ty}")
                            return@processArgs true
                        }

                        val passInType = typeInfo.ty
                        val paramRealType = if (substitutor==null) pi.ty else pi.ty.substitute(substitutor)// Support generic argument
                        if (!passInType.isSuitableFor(paramRealType, searchContext, true))
                            myHolder.registerProblem(typeInfo.param, "Type mismatch. Required: '${paramRealType}' Found: '$passInType'")
                        true
                    }
                    if (nArgs < call.argList.size && !signature.hasVarargs()) {
                        for (i in nArgs until call.argList.size) {
                            myHolder.registerProblem(call.argList[i], "Too many arguments.")
                        }
                    }
                }
            }
}