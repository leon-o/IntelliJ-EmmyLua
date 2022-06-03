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

package com.tang.intellij.lua.stubs

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.tang.intellij.lua.comment.psi.LuaDocNilableTy
import com.tang.intellij.lua.comment.psi.LuaDocTagNilable
import com.tang.intellij.lua.comment.psi.LuaDocTagType
import com.tang.intellij.lua.comment.psi.impl.LuaDocNilableTyImpl
import com.tang.intellij.lua.comment.psi.impl.LuaDocTagNilableImpl
import com.tang.intellij.lua.comment.psi.impl.LuaDocTagTypeImpl
import com.tang.intellij.lua.psi.LuaElementType

class LuaDocTagNilableStubType : LuaStubElementType<LuaDocTagNilableStub, LuaDocTagNilable>("DOC_TAG_NILABLE"){
    override fun indexStub(stub: LuaDocTagNilableStub, sink: IndexSink) {
    }

    override fun deserialize(inputStream: StubInputStream, stubElement: StubElement<*>?): LuaDocTagNilableStub {
        return LuaDocNilableStubImpl(stubElement)
    }

    override fun serialize(stub: LuaDocTagNilableStub, stubElement: StubOutputStream) {
    }

    override fun createPsi(stub: LuaDocTagNilableStub) = LuaDocTagNilableImpl(stub, this)

    override fun createStub(tagType: LuaDocTagNilable, stubElement: StubElement<*>?): LuaDocTagNilableStub {
        return LuaDocNilableStubImpl(stubElement)
    }
}

interface LuaDocTagNilableStub : StubElement<LuaDocTagNilable>

class LuaDocNilableStubImpl(parent: StubElement<*>?)
    : LuaDocStubBase<LuaDocTagNilable>(parent, LuaElementType.DOC_NILABLE), LuaDocTagNilableStub