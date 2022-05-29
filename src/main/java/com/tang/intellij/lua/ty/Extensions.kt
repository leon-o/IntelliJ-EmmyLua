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

import com.tang.intellij.lua.ext.recursionGuard
import com.tang.intellij.lua.search.SearchContext

fun ITy.hasMember(name: String, context: SearchContext): Boolean {
    if (this is TyClass) {
        val member = recursionGuard(this, {
            this.findMember(name, context)
        }
        )
        return member != null
    }
    if (this is TyUnion) {
        for (iTy in this.getChildTypes()) {
            val has = recursionGuard(this, { iTy.hasMember(name, context) }) == true
            if (has)
                return true
        }
    }
    return false
}

fun ITyClass.isAllMemberFitTo(target:ITyClass, context:SearchContext, deep:Boolean = false):Boolean{
    val myMemberChain = this.getMemberChain(context)
    val targetMemberChain = target.getMemberChain(context)
    return targetMemberChain.all(true){
        _,name,member->
        val targetType = member.guessType(context)
        if(targetType==Ty.UNKNOWN)
            return@all true

        val myMember = myMemberChain.findMember(name)
        if(myMember!=null){
            val myType = myMember.guessType(context)
            if(deep && targetType is TyClass && myType is TyClass){
                return@all myType.isAllMemberFitTo(targetType,context,true)
            }else{
                return@all targetType==myType
            }
        }
        else{ // Can't find that member, this is an unfit type.
            return@all false
        }
    }
}
