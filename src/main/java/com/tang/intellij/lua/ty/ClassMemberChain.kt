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

package com.tang.intellij.lua.ty

import com.tang.intellij.lua.psi.LuaClassMember

class ClassMemberChain(val ty: ITyClass, var superChains: Array<ClassMemberChain>) {
    constructor(ty: ITyClass, superChain: ClassMemberChain?):this(ty,superChain?.run{ arrayOf(this) }?: arrayOf())

    private val members = mutableMapOf<String, LuaClassMember>()

    fun add(member: LuaClassMember) {
        val name = member.name ?: return
        val superExist = superChains.mapNotNull { it.findMember(name) } //所有基类的同名成员
        val override = superExist.isEmpty() || superExist.all { canOverride(member, it) } //没有或者都可以重写
        if (override) {
            val selfExist = members[name]
            if (selfExist == null || member.worth > selfExist.worth)
                members[name] = member
        }
    }

    fun findMember(name: String): LuaClassMember? {
        return members.getOrElse(name) {
            var member:LuaClassMember? = null
            for (superChain in superChains) {
                member = superChain.findMember(name)
                if(member!=null)
                    break
            }
            member
        }
    }

    private fun process(deep: Boolean, processor: (ITyClass, String, LuaClassMember) -> Unit) {
        for ((t, u) in members) {
            processor(ty, t, u)
        }
        if (deep)
            superChains.forEach { it.process(true, processor) }
    }

    fun process(deep: Boolean, processor: (ITyClass, LuaClassMember) -> Unit) {
        val cache = mutableSetOf<String>()
        process(deep) { clazz, name, member ->
            if (cache.add(name))
                processor(clazz, member)
        }
    }

    private fun canOverride(member: LuaClassMember, superMember: LuaClassMember): Boolean {
        return member.worth > superMember.worth || (member.worth == superMember.worth && member.worth > LuaClassMember.WORTH_ASSIGN)
    }
}