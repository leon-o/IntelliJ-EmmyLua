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

package com.tang.intellij.lua.stubs.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.IntStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import com.tang.intellij.lua.comment.psi.LuaDocTagField
import com.tang.intellij.lua.psi.LuaClassMember
import com.tang.intellij.lua.psi.LuaClassMethod
import com.tang.intellij.lua.psi.LuaTableField
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*

class LuaClassMemberIndex : IntStubIndexExtension<LuaClassMember>() {
    override fun getKey() = StubKeys.CLASS_MEMBER

    override fun get(s: Int, project: Project, scope: GlobalSearchScope): Collection<LuaClassMember> =
            StubIndex.getElements(StubKeys.CLASS_MEMBER, s, project, scope, LuaClassMember::class.java)

    companion object {
        val instance = LuaClassMemberIndex()

        fun process(key: String, context: SearchContext, processor: Processor<LuaClassMember>): Boolean {
            if (context.isDumb)
                return false
            val all = LuaClassMemberIndex.instance.get(key.hashCode(), context.project, context.scope)
            return ContainerUtil.process(all, processor)
        }

        private fun process(cls: ITyClass?, className: String, fieldName: String, context: SearchContext, processor: Processor<LuaClassMember>, deep: Boolean): Boolean {
            val key = "$className*$fieldName"
            if (!process(key, context, processor))
                return false

            if (deep) {
                val type = cls ?: LuaClassIndex.find(className, context)?.type

                if (type != null) {
                    // from alias
                    type.lazyInit(context)
                    val notFound = type.processAlias(Processor {
                        process(it, fieldName, context, processor, false)
                    })
                    if (!notFound)
                        return false

                    return Ty.processSuperClass(type, context) { superType ->
                        val superClass = (if (superType is ITyGeneric) superType.base else superType) as? ITyClass
                        if (superClass != null) {
                            process(superClass, fieldName, context, processor, false)
                        } else true
                    }
                }
            }
            return true
        }

        fun process(cls: ITyClass, fieldName: String, context: SearchContext, processor: Processor<LuaClassMember>, deep: Boolean = true): Boolean {
            return process(cls, cls.className, fieldName, context, processor, deep)
        }

        fun process(className: String, fieldName: String, context: SearchContext, processor: Processor<LuaClassMember>, deep: Boolean = true): Boolean {
            return process(null, className, fieldName, context, processor, deep)
        }

        fun find(type: ITyClass, fieldName: String, context: SearchContext): LuaClassMember? {
            var perfect: LuaClassMember? = null
            var tagField: LuaDocTagField? = null
            var tableField: LuaTableField? = null
            processAll(type, fieldName, context, Processor {
                when (it) {
                    is LuaDocTagField -> {
                        tagField = it
                        false
                    }
                    is LuaTableField -> {
                        tableField = it
                        true
                    }
                    else -> {
                        if (perfect == null)
                            perfect = it
                        true
                    }
                }
            })
            if (tagField != null) return tagField
            if (tableField != null) return tableField
            return perfect
        }

        fun processAll(type: ITyClass, fieldName: String, context: SearchContext, processor: Processor<LuaClassMember>): Boolean {
            return if (type is TyParameter)
                (type.superClass as? ITyClass)?.let { process(it, fieldName, context, processor) } ?: true
            else process(type, fieldName, context, processor)
        }

        fun processAll(type: ITyClass, context: SearchContext, processor: Processor<LuaClassMember>) {
            if (process(type.className, context, processor)) {
                type.lazyInit(context)
                type.processAlias(Processor {
                    process(it, context, processor)
                })
            }
        }

        fun findMethod(className: String, memberName: String, context: SearchContext, deep: Boolean = true): LuaClassMethod? {
            var target: LuaClassMethod? = null
            process(className, memberName, context, Processor {
                if (it is LuaClassMethod) {
                    target = it
                    return@Processor false
                }
                true
            }, deep)
            return target
        }

        fun indexStub(indexSink: IndexSink, className: String, memberName: String) {
            indexSink.occurrence(StubKeys.CLASS_MEMBER, className.hashCode())
            indexSink.occurrence(StubKeys.CLASS_MEMBER, "$className*$memberName".hashCode())
        }
    }
}
