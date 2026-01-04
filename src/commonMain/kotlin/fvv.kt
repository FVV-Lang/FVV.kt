//====================================================================================================
// Copyright (C) 2016-present ShIroRRen <http://shiror.ren>.                                         =
//                                                                                                   =
// Licensed under the F2DLPR License.                                                                =
//                                                                                                   =
// YOU MAY NOT USE THIS FILE EXCEPT IN COMPLIANCE WITH THE LICENSE.                                  =
// Provided "AS IS", WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,                                   =
// unless required by applicable law or agreed to in writing.                                        =
//                                                                                                   =
// For the F2DLPR License terms and conditions, visit: <http://license.fileto.download>.             =
//====================================================================================================

@file:Suppress("PackageDirectoryMismatch", "unused")

package ren.shiror.fvv

import kotlin.reflect.KClass
import kotlin.reflect.typeOf

class FVVV(
	value: Any? = null,
	var nodes: MutableMap<String, FVVV> = mutableMapOf(),
	var desc: String = "",
	var link: String = "",
) {
	companion object {
		@PublishedApi internal val numCls by lazy {
			setOf(Long::class, Int::class, Double::class, Float::class)
		}

		@PublishedApi
		@Suppress("UNCHECKED_CAST")
		internal inline fun <reified T> List<*>.cast() = when {
			all { it is T }                            -> this
			T::class in numCls && all { it is Number } -> when (T::class) {
				Long::class   -> map { (it as Number).toLong() }
				Int::class    -> map { (it as Number).toInt() }
				Double::class -> map { (it as Number).toDouble() }
				Float::class  -> map { (it as Number).toFloat() }
				else          -> null
			}

			else                                       -> null
		} as? List<T>?

		private val _escapeTable by lazy {
			IntArray(1 shl Byte.SIZE_BITS).apply {
				setOf(
					'b' to '\b', 'f' to '\u000C', // \f
					'n' to '\n', 'r' to '\r', 't' to '\t', '\\' to '\\'
				).forEach { (src, tgt) ->
					this[src.code] = tgt.code
				}
			}
		}

		private fun getEscapedChar(ch: Char) = ch.code.let { chc ->
			if (chc < _escapeTable.size) _escapeTable[chc].takeIf { it != 0 }?.toChar() else null
		}
	}

	var value: Any? = null
		set(tgt) {
			field = if (tgt is FVVV) tgt.value else tgt
		}

	init {
		this.value = value
	}

	operator fun get(key: String) =
		key.split('.').fold(this) { tgt, path -> tgt.nodes.getOrPut(path) { FVVV() } }

	operator fun set(key: String, tgt: Any?) {
		this[key].value = if (tgt is FVVV) tgt.value else tgt
	}

	override fun equals(other: Any?) = when {
		this === other -> true
		other !is FVVV -> false
		else           -> value == other.value && nodes == other.nodes
	}

	override fun hashCode() = listOf(value, nodes).hashCode()

	fun isEmpty() = when (value) {
		null       -> true
		is String  -> (value as String).isEmpty()
		is List<*> -> (value as List<*>).isEmpty()
		else       -> false
	}

	fun isNotEmpty() = !isEmpty()

	inline fun <reified T> `is`() =
		(value is List<*> && (typeOf<T>().arguments.firstOrNull()?.type?.classifier as? KClass<*>?)?.let { tpCls ->
			`as`<List<*>>()?.takeIf { list ->
				list.all {
					it?.let { tpCls.isInstance(it) } ?: false
				} || (tpCls in numCls && list.all { it is Number })
			}
		} != null) || (value !is List<*> && value is T)

	inline fun <reified T> isType() = `is`<T>()
	inline fun <reified T> isList() =
		`as`<List<*>>()?.takeIf { list -> list.all { it is T } || (T::class in numCls && list.all { it is Number }) } != null

	val type get() = value?.run { this::class }

	inline fun <reified T> `as`() = value as? T? ?: (value as? Number)?.let { num ->
		when (T::class) {
			Long::class   -> num.toLong()
			Int::class    -> num.toInt()
			Double::class -> num.toDouble()
			Float::class  -> num.toFloat()
			else          -> null
		} as? T?
	}

	inline fun <reified T> `as`(default: T) = `as`() ?: default
	inline fun <reified T> asType() = `as`<T>()
	inline fun <reified T> asType(default: T) = `as`(default)
	inline fun <reified T> get() = `as`<T>()!!
	inline fun <reified T> list(default: List<T> = emptyList()) = `as`<List<*>>()?.cast<T>() ?: default

	val bool get() = `as`(false)
	val boolean get() = bool
	val int get() = `as`(0L)
	val integer get() = int
	val double get() = `as`(0.0)
	val float get() = double
	val string get() = `as`("")
	val bools get() = list<Boolean>()
	val ints get() = list<Long>()
	val doubles get() = list<Double>()
	val strings get() = list<String>()
	val fvvvs get() = list<FVVV>()

	fun unlink(): Unit = nodes.forEach { (_, value) -> value.unlink() }.also { link = "" }

	fun parse(text: String) {
		if (text.trim().isEmpty()) return

		val ctx = TextCtx(text)
		val scopeStack = mutableListOf<FVVV>()

		ctx.skipBlanks()
		val hasWrapper = ctx.match('{', '｛')
		parseMain(ctx, scopeStack)

		if (hasWrapper) {
			ctx.skipBlanks()
			if (!ctx.match('}', '｝')) throw ctx.err.notFound("wrapper")
		}
		ctx.skipBlanks()
		if (!ctx.isEof) throw ctx.err.whyNotEOF()
	}

	private class TextCtx(val input: String) {
		var index = 0
		val linesStart = mutableListOf(0)
		val err by lazy { ErrHandler(this) }

		init {
			if (input.startsWith('\uFEFF')) index = 1
		}

		fun preview() = if (isEof) null else input[index]
		fun prematch(vararg tgts: Char) = preview()?.let { ch -> tgts.any { ch == it } } ?: false

		fun next() = if (isEof) null else input[index++].also {
			when (it) {
				'\r' -> linesStart.add(index)
				'\n' -> if (index >= 2 && input[index - 2] == '\r') linesStart[linesStart.size - 1] =
					index
				else linesStart.add(index)
			}
		}

		fun match(vararg tgts: Char, skipBlanks: Boolean = true, sameLine: Boolean = false): Boolean {
			if (skipBlanks) this.skipBlanks(sameLine = sameLine)
			return prematch(*tgts).also { if (it) next() }
		}

		fun skipBlanks(sameLine: Boolean = false) {
			while (!isEof && input[index].isWhitespace()) if (sameLine && prematch('\n', '\r')) break
			else next()
		}

		val isEof get() = index >= input.length
		fun isSameLine() = linesStart.size.let { before ->
			skipBlanks()
			before == linesStart.size
		}

		class ErrHandler(private val ctx: TextCtx) {
			private fun makeError(msg: String) =
				ParseException("${ctx.linesStart.size}:${ctx.index - ctx.linesStart.last() + 1}: $msg")

			fun unknown() = makeError("Why??? IDK!!!")
			fun whyEOF() = makeError("Why EOF???")
			fun whyNotEOF() = makeError("Why not EOF???")
			fun notFound(tgt: String) =
				makeError("Where is the ${if (tgt.length > 1) tgt else "'$tgt'"}?")

			fun noValue(tgt: String) = makeError("Cannot find the value of '$tgt'")
			fun plusList() = makeError("Why plus with list?")
			fun valuePlusFVVV() = makeError("Why value plus with FVVV?")
		}
	}

	class ParseException(msg: String) : Exception(msg) {
		override fun toString(): String = "ParseException: $message"
	}

	private fun parseMain(ctx: TextCtx, scopeStack: MutableList<FVVV>) {
		fun findKey(path: String, scopeStack: List<FVVV>) =
			path.split('.').takeIf { it.isNotEmpty() }?.let { paths ->
				scopeStack.asReversed().firstNotNullOfOrNull { index ->
					paths.fold(index as FVVV?) { target, idxPath ->
						target?.nodes?.get(idxPath)
					}
				}
			}

		fun parseName(ctx: TextCtx) = ctx.skipBlanks().let {
			buildString {
				while (!ctx.isEof && !ctx.prematch('=', ':', '：', '<')) append(ctx.next()!!)
			}.trimEnd()
		}

		fun parseDesc(
			ctx: TextCtx,
			desc: StringBuilder,
			scopeStack: List<FVVV>,
			skipBlanks: Boolean = true,
			sameLine: Boolean = false,
		) {
			while (true) {
				val (origIdx, origLine) = ctx.index to ctx.linesStart.size
				if (!ctx.match('<', sameLine = sameLine)) {
					if (!skipBlanks) {
						ctx.index = origIdx
						while (ctx.linesStart.size > origLine) ctx.linesStart.removeLast()
					}
					break
				}

				desc.clear()
				while (true) when {
					ctx.isEof                           -> throw ctx.err.whyEOF()

					ctx.match('>', skipBlanks = false)  -> findKey(
						"$desc", scopeStack
					)?.takeIf { it.isType<String>() }?.let { target ->
						desc.apply {
							clear()
							append(target.get<String>())
						}
					}.also { break }

					ctx.match('\\', skipBlanks = false) -> when {
						ctx.isEof                          -> throw ctx.err.whyEOF()

						ctx.match('>', skipBlanks = false) -> desc.append('>')
						else                               -> {
							val ch = ctx.next()!!
							getEscapedChar(ch)?.let { tgt -> desc.append(tgt) } ?: desc.append(
								'\\', ch
							)
						}
					}

					else                                -> desc.append(ctx.next()!!)
				}
			}
		}

		fun parseText(ctx: TextCtx, text: StringBuilder) {
			if (ctx.match('`')) {
				while (true) when {
					ctx.isEof                          -> throw ctx.err.whyEOF()
					ctx.match('`', skipBlanks = false) -> break
					else                               -> text.append(ctx.next()!!)
				}
				"$text".trimIndent().trim().let { tmpStr ->
					text.apply {
						clear()
						append(tmpStr)
					}
				}
				return
			}

			val isFullWidth = ctx.match('“')
			if (!isFullWidth && !ctx.match('"')) throw ctx.err.unknown()
			while (true) when {
				ctx.isEof                               -> throw ctx.err.whyEOF()

				if (isFullWidth) ctx.match('”', skipBlanks = false)
				else ctx.match('"', skipBlanks = false) -> return

				ctx.match('\\', skipBlanks = false)     -> when {
					ctx.isEof                                          -> throw ctx.err.whyEOF()

					isFullWidth && ctx.match('”', skipBlanks = false)  -> text.append('”')
					!isFullWidth && ctx.match('"', skipBlanks = false) -> text.append('"')

					else                                               -> {
						val ch = ctx.next()!!
						getEscapedChar(ch)?.let { tgt -> text.append(tgt) } ?: text.append('\\', ch)
					}
				}

				else                                    -> text.append(ctx.next()!!)
			}
		}

		fun tryParseNumber(tgtStr: String): Number? {
			val tgtStr = tgtStr.filterNot { it in "'’" }
			if (tgtStr.isEmpty()) return null

			var sign = 1
			var idx = 0
			if (tgtStr.startsWith('-')) sign = (-1).also { ++idx }
			else if (tgtStr.startsWith('+')) ++idx

			var radix = 10
			if (idx < tgtStr.length && tgtStr[idx] == '0' && idx + 1 < tgtStr.length) when (tgtStr[idx + 1]) {
				'x', 'X'                               -> radix = 16.also { idx += 2 }
				'o', 'O'                               -> radix = 8.also { idx += 2 }
				'b', 'B'                               -> radix = 2.also { idx += 2 }
				'0', '1', '2', '3', '4', '5', '6', '7' -> radix = 8.also { ++idx }
			}
			val digitStr = tgtStr.substring(idx)
			return digitStr.takeIf { it.isNotEmpty() }?.let {
				when {
					radix != 10                                          -> digitStr.toLongOrNull(radix)
					digitStr.any { it == '.' || it == 'e' || it == 'E' } -> digitStr.toDoubleOrNull()
					else                                                 -> digitStr.toLongOrNull()
				}?.let {
					when (it) {
						is Long   -> it * sign
						is Double -> it * sign
						else      -> null
					}
				}
			}
		}

		fun parseValue(
			ctx: TextCtx,
			scopeStack: List<FVVV>,
			tgtFwv: FVVV,
			idxDesc: StringBuilder,
			inList: Boolean = false,
		) {
			while (true) {
				parseDesc(ctx, idxDesc, scopeStack, skipBlanks = inList, sameLine = !inList)
				if (ctx.isEof || (if (inList) ctx.match(',', '，') || ctx.prematch(']', '］')
					else !ctx.isSameLine() || ctx.match(';', '；') || ctx.prematch('}', '｝'))
				) throw ctx.err.notFound("value")

				val tmpSb = StringBuilder()
				if (ctx.prematch('"', '“', '`')) {
					parseText(ctx, tmpSb)
					if (tgtFwv.value == null) tgtFwv.value = "$tmpSb"
					else tgtFwv.apply {
						link = ""
						value = "${tgtFwv.value}$tmpSb"
					}
				} else {
					while (!ctx.isEof && !ctx.prematch('<', '+') && !ctx.prematch(
							'\r', '\n'
						)
					) {
						if (if (inList) ctx.prematch(',', '，', ']', '］') else ctx.prematch(
								';', '；', '}', '｝'
							)
						) break
						tmpSb.append(ctx.next()!!)
					}

					val tmpStr = "$tmpSb".trimEnd()
					if (tmpStr.isEmpty()) throw ctx.err.notFound("value")

					val isTrue = tmpStr.toBoolean()

					if (isTrue || tmpStr.equals("false", true)) {
						if (tgtFwv.value == null) tgtFwv.value = isTrue
						else tgtFwv.apply {
							link = ""
							value = "${tgtFwv.value}$tmpStr"
						}
					} else {
						tryParseNumber(tmpStr)?.let { tmpNum ->
							if (tgtFwv.value == null) tgtFwv.value = tmpNum
							else tgtFwv.apply {
								link = ""
								value = "${tgtFwv.value}$tmpStr"
							}
						} ?: findKey(tmpStr, scopeStack)?.let { target ->
							if (tgtFwv.value != null && target.value is List<*>) throw ctx.err.plusList()
							if (tgtFwv.value == null) tgtFwv.apply {
								link = tmpStr
								value = target.value
							}
							else tgtFwv.apply {
								link = ""
								value = "${tgtFwv.value}${target.value}"
							}
							tgtFwv.nodes = target.nodes
						} ?: throw ctx.err.noValue(tmpStr)
					}
				}
				parseDesc(ctx, idxDesc, scopeStack, skipBlanks = false, sameLine = true)
				if (ctx.isEof || !ctx.isSameLine() || (if (inList) ctx.match(
						',', '，'
					) || ctx.prematch(']', '］')
					else ctx.match(';', '；') || ctx.prematch('}', '｝'))
				) return

				if (ctx.match('+')) continue
				else throw ctx.err.notFound("+")
			}
		}

		scopeStack.add(this)

		while (true) {
			val idxDesc = StringBuilder()
			parseDesc(ctx, idxDesc, scopeStack, skipBlanks = false)
			if (!ctx.isSameLine()) idxDesc.clear()

			if (ctx.isEof || ctx.prematch('}', '｝')) break

			val name = parseName(ctx)
			if (name.isEmpty()) throw ctx.err.notFound("name")
			parseDesc(ctx, idxDesc, scopeStack)
			if (!ctx.match('=', ':', '：')) throw ctx.err.notFound("=")
			parseDesc(ctx, idxDesc, scopeStack)

			var goto = false
			val tgtKey = this[name]
			if (ctx.match('[', '［')) {
				val tgtList = mutableListOf<Any>()
				var listType = null as KClass<*>?
				while (true) {
					val valueDesc = StringBuilder()
					parseDesc(ctx, valueDesc, scopeStack, skipBlanks = false)
					if (!ctx.isSameLine()) valueDesc.clear()

					if (ctx.isEof) throw ctx.err.whyEOF()
					if (ctx.match('{', '｛')) {
						listType = FVVV::class
						val tmpValue = FVVV().apply { parseMain(ctx, scopeStack) }
						if (!ctx.match('}', '｝')) throw ctx.err.notFound("}")
						parseDesc(ctx, valueDesc, scopeStack, skipBlanks = false, sameLine = true)
						tmpValue.desc = "$valueDesc"
						tgtList.add(tmpValue)
						if (ctx.isSameLine() && !ctx.match(',', '，') && !ctx.prematch(
								']', '］'
							)
						) throw ctx.err.notFound("EOL")
					} else {
						val tgtFwv = FVVV()
						parseValue(ctx, scopeStack, tgtFwv, idxDesc, inList = true)

						@Suppress("UNCHECKED_CAST") if (tgtFwv.value is List<*>) tgtList.addAll(tgtFwv.value as List<Any>)
						else if (tgtFwv.value != null) tgtList.add(tgtFwv.value!!)
						else tgtList.add(tgtFwv)

						if (listType == null) listType = tgtList.last()::class
						else if (listType != tgtList.last()::class) {
							if (listType == FVVV::class || tgtList.last()::class == FVVV::class) throw ctx.err.valuePlusFVVV()
							when (tgtList.last()::class) {
								String::class -> listType = String::class
								Double::class -> if (listType != String::class) listType = Double::class
								Long::class   -> if (listType != String::class && listType != Double::class) listType =
									Long::class
							}
						}
					}
					if (ctx.match(']', '］')) break
				}
				if (listType != FVVV) tgtList.map { item ->
					if (item::class == listType) item else when (listType) {
						String::class -> "$item"
						Double::class -> when (item) {
							is Long    -> item.toDouble()
							is Boolean -> if (item) 1.0 else 0.0
							else       -> item
						}

						Long::class   -> when (item) {
							is Boolean -> if (item) 1 else 0
							else       -> item
						}

						else          -> item
					}
				}.let { tmpList ->
					tgtList.apply {
						clear()
						addAll(tmpList)
					}
				}
				tgtKey.value = when (listType) {
					FVVV::class    -> tgtList.cast<FVVV>()!!
					String::class  -> tgtList.cast<String>()!!
					Double::class  -> tgtList.cast<Double>()!!
					Long::class    -> tgtList.cast<Long>()!!
					Boolean::class -> tgtList.cast<Boolean>()!!
					else           -> TODO()
				}
			} else if (ctx.match('{', '｛')) {
				tgtKey.parseMain(ctx, scopeStack)
				if (!ctx.match('}', '｝')) throw ctx.err.notFound("}")
			} else {
				parseValue(ctx, scopeStack, tgtKey, idxDesc)
				goto = true
			}

			if (!goto) {
				parseDesc(ctx, idxDesc, scopeStack, skipBlanks = false, sameLine = true)
				if (ctx.isSameLine() && !ctx.isEof && !ctx.match(';', '；') && !ctx.prematch(
						'}', '｝'
					)
				) throw ctx.err.notFound("EOL")
			}
			tgtKey.desc = "$idxDesc"
		}

		scopeStack.removeLast()
	}
}