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

import kotlin.math.absoluteValue
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

open class FVVV(
	value: Any? = null,
	var nodes: MutableMap<String, FVVV> = mutableMapOf(),
	var desc: String = "",
	var link: String = "",
) {
	companion object {
		@PublishedApi internal val intCls by lazy { setOf(Long::class, Int::class) }
		@PublishedApi internal val fpCls by lazy { setOf(Double::class, Float::class) }

		@PublishedApi
		@Suppress("UNCHECKED_CAST")
		internal inline fun <reified T> List<*>.cast() = when {
			all { it is T }                                                 -> this

			T::class in fpCls && all { it != null && it::class in fpCls }   -> when (T::class) {
				Double::class -> map { (it as Number).toDouble() }
				Float::class  -> map { (it as Number).toFloat() }
				else          -> null
			}

			T::class in intCls && all { it != null && it::class in intCls } -> when (T::class) {
				Long::class -> map { (it as Number).toLong() }
				Int::class  -> map { (it as Number).toInt() }
				else        -> null
			}

			else                                                            -> null
		} as? List<T>?

		private val _escapeTable by lazy {
			IntArray(1 shl Byte.SIZE_BITS).apply {
				setOf(
					'b' to '\b',
					'f' to '\u000C' /*\f*/,
					'n' to '\n',
					'r' to '\r',
					't' to '\t',
					'\\' to '\\'
				).forEach { (src, tgt) ->
					this[src.code] = tgt.code
				}
			}
		}

		private fun getEscapedChar(ch: Char) = ch.code.let { chc ->
			if (chc < _escapeTable.size) _escapeTable[chc].takeIf { it != 0 }?.toChar() else null
		}
	}

	enum class FormatOpt(val mask: Int) {
		Common(0),

		UseWrapper(1 shl 0), Minify(1 shl 1),

		UseCRLF(1 shl 2), UseCR(1 shl 3),

		UseSpace2(1 shl 4), UseSpace4(1 shl 5),

		IntBinary(1 shl 6), IntOctal(1 shl 7), IntHex(1 shl 8),

		DigitSep3(1 shl 9), DigitSep4(1 shl 10),

		UseColon(1 shl 11), FullWidth(1 shl 12),

		KeepListSingle(1 shl 13), ForceUseSeparator(1 shl 14), RawMultilineString(1 shl 15),

		NoDescs(1 shl 16), NoLinks(1 shl 17), FlattenPaths(1 shl 18), FwwStyle(1 shl 19);

		companion object {
			infix fun FormatOpt.or(other: FormatOpt) = this.mask or other.mask
			infix fun FormatOpt.or(other: Int) = this.mask or other
			infix fun Int.or(other: FormatOpt) = this or other.mask

			fun Int.toFmtOpts() = entries.filterTo(mutableSetOf()) { (this and it.mask) != Common.mask }
		}
	}

	var value: Any? = null
		set(tgt) = (if (tgt is FVVV) tgt.value else tgt).let { field = it }

	init {
		this.value = value
	}

	operator fun get(key: String) =
		key.split('.').fold(this) { tgt, path -> tgt.nodes.getOrPut(path) { FVVV() } }

	operator fun set(key: String, tgt: Any?) = tgt.also { this[key].value = it }

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
			(value as List<*>).takeIf { list ->
				list.all {
					it != null && tpCls.isInstance(it)
				} || (tpCls in fpCls && list.all {
					it != null && it::class in fpCls
				}) || (tpCls in intCls && list.all {
					it != null && it::class in intCls
				})
			}
		} != null) || (value !is List<*> && value is T)

	inline fun <reified T> isType() = `is`<T>()
	inline fun <reified T> isList() = `as`<List<*>>()?.takeIf { list ->
		list.all { it is T } || (T::class in fpCls && list.all {
			it != null && it::class in fpCls
		}) || (T::class in intCls && list.all {
			it != null && it::class in intCls
		})
	} != null

	val type get() = value?.run { this::class }

	inline fun <reified T> `as`() =
		if (`is`<T>()) value as? T? ?: (value as? Double? ?: value as? Float?)?.let { fp ->
			when (T::class) {
				Double::class -> fp.toDouble()
				Float::class  -> fp.toFloat()
				else          -> null
			} as? T?
		} ?: (value as? Long? ?: value as? Int?)?.let { int ->
			when (T::class) {
				Long::class -> int.toLong()
				Int::class  -> int.toInt()
				else        -> null
			} as? T?
		} else null

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

	override fun toString() = toString(FormatCtx())

	fun toString(vararg flags: FormatOpt) =
		toString(FormatCtx(flags.fold(FormatOpt.Common.mask) { tgt, idx -> tgt or idx.mask }))

	fun toString(vararg flags: Int) =
		toString(FormatCtx(flags.fold(FormatOpt.Common.mask) { tgt, idx -> tgt or idx }))

	private fun toString(ctx: FormatCtx) = buildString {
		if (ctx.useWrapper) {
			append(ctx.fwvBegin)
			if (!ctx.minify) append(ctx.newline)
		}
		toStringRoot(ctx, this, if (ctx.useWrapper) 1 else 0)
		if (ctx.useWrapper) {
			if (!ctx.minify) append(ctx.newline)
			append(ctx.fwvEnd)
		}
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

	private class FormatCtx(flags: Int = FormatOpt.Common.mask) {
		var newline = "\n"
		var indentUnit = "\t"
		var assignOp = " = "
		var listBegin = '['
		var listEnd = ']'
		var fwvBegin = '{'
		var fwvEnd = '}'
		var itemSep = ','
		var stmtSep = ';'

		var intBase = 10

		var digitSepStep = 0
		var digitSepChar = null as Char?

		var useWrapper = false

		var minify = false

		var fullWidth = false

		var listSingle = false
		var forceSep = false
		var rawStr = false

		var noDescs = false
		var noLinks = false
		var flattenPaths = false
		var fwwStyle = false

		init {
			fun Int.has(opt: FormatOpt) = (this and opt.mask) != FormatOpt.Common.mask

			useWrapper = flags.has(FormatOpt.UseWrapper)

			when {
				flags.has(FormatOpt.UseCRLF) -> newline = "\r\n"
				flags.has(FormatOpt.UseCR)   -> newline = "\r"
			}

			when {
				flags.has(FormatOpt.UseSpace2) -> indentUnit = "  "
				flags.has(FormatOpt.UseSpace4) -> indentUnit = "    "
			}

			when {
				flags.has(FormatOpt.IntHex)    -> intBase = 16
				flags.has(FormatOpt.IntOctal)  -> intBase = 8
				flags.has(FormatOpt.IntBinary) -> intBase = 2
			}

			when {
				flags.has(FormatOpt.DigitSep3) -> digitSepStep = 3
				flags.has(FormatOpt.DigitSep4) -> digitSepStep = 4
			}

			fullWidth = flags.has(FormatOpt.FullWidth)
			if (fullWidth) {
				if (flags.has(FormatOpt.UseColon)) assignOp = "："
				listBegin = '［'
				listEnd = '］'
				fwvBegin = '｛'
				fwvEnd = '｝'
				itemSep = '，'
				stmtSep = '；'
				if (digitSepStep > 0) digitSepChar = '’'
			} else {
				if (flags.has(FormatOpt.UseColon)) assignOp = ": "
				if (digitSepStep > 0) digitSepChar = '\''
			}

			listSingle = flags.has(FormatOpt.KeepListSingle)
			forceSep = flags.has(FormatOpt.ForceUseSeparator)
			rawStr = flags.has(FormatOpt.RawMultilineString)

			noDescs = flags.has(FormatOpt.NoDescs)
			noLinks = flags.has(FormatOpt.NoLinks)
			flattenPaths = flags.has(FormatOpt.FlattenPaths)
			fwwStyle = flags.has(FormatOpt.FwwStyle)

			minify = flags.has(FormatOpt.Minify)
			if (minify) {
				newline = ""
				indentUnit = ""

				assignOp = assignOp.trim()
			}
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
					)?.takeIf { it.isType<String>() }?.also { target ->
						desc.clear().append(target.get<String>())
					}.let { break }

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
				"$text".trimIndent().trim().also { tmpStr ->
					text.clear().append(tmpStr)
				}
				return
			}

			val isFullWidth = ctx.match('“') || !ctx.match('"')
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
						tryParseNumber(tmpStr)?.also { tmpNum ->
							if (tgtFwv.value == null) tgtFwv.value = tmpNum
							else tgtFwv.apply {
								link = ""
								value = "${tgtFwv.value}$tmpStr"
							}
						} ?: findKey(tmpStr, scopeStack)?.also { target ->
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
				}.also { tmpList ->
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

	private fun toStringRoot(ctx: FormatCtx, ret: StringBuilder, level: Int) {
		if (nodes.isEmpty()) return

		nodes.entries.forEachIndexed { idx, (key, value) ->
			value.toStringMain(ctx, key, ret, level, idx == nodes.size - 1)
		}
	}

	private fun toStringMain(
		ctx: FormatCtx,
		name: String,
		ret: StringBuilder,
		level: Int,
		isBack: Boolean,
	) {
		fun escapeString(str: String, isDesc: Boolean, fullWidth: Boolean = false) =
			buildString(str.length + 6) {
				if (isDesc) append('<')
				else append(if (fullWidth) '“' else '"')

				str.forEach { ch ->
					when (ch) {
						'\\'            -> "\\\\"
						'\b'            -> "\\b"
						'\u000C' /*\f*/ -> "\\f"
						'\n'            -> "\\n"
						'\r'            -> "\\r"
						'\t'            -> "\\t"
						'"'             -> if (!fullWidth && !isDesc) "\\\"" else ch
						'”'             -> if (fullWidth && !isDesc) "\\”" else ch
						'>'             -> if (isDesc) "\\>" else ch
						else            -> ch
					}.also { append(it) }
				}

				if (isDesc) append('>')
				else append(if (fullWidth) '”' else '"')
			}

		fun toStringFWV(ctx: FormatCtx, tgtFwv: FVVV, ret: StringBuilder, indent: String, level: Int) =
			ret.apply {
				append(ctx.fwvBegin)
				if (!ctx.minify) append(ctx.newline)
				tgtFwv.toStringRoot(ctx, ret, level + 1)
				if (!ctx.minify) append(ctx.newline).append(indent)
				append(ctx.fwvEnd)
			}

		@Suppress("ReturnCount")
		fun toStringValue(
			ctx: FormatCtx, tgtVal: Any, ret: StringBuilder, indent: String, level: Int = 0
		) {
			when (tgtVal) {
				is Boolean -> ret.append("$tgtVal")
				is Number  -> ret.apply {
					if (tgtVal is Long && ctx.intBase != 10) {
						if (tgtVal == 0L) when (ctx.intBase) {
							16   -> "0x0"
							8    -> "0o0"
							2    -> "0b0"
							else -> TODO()
						}.also {
							append(it)
							return
						}

						if (tgtVal < 0) append('-')
						val tgtVal = tgtVal.absoluteValue

						when (ctx.intBase) {
							2    -> "0b"
							8    -> "0o"
							16   -> "0x"
							else -> TODO()
						}.also {
							append(it, tgtVal.toString(ctx.intBase))
						}
						return
					}

					val rawNum = "$tgtVal"
					if (ctx.digitSepStep == 0) append(rawNum).also { return }

					val parts = rawNum.split('.')
					var intPart = parts[0]
					var hasSign = false
					if (intPart.startsWith('-') || intPart.startsWith('+')) {
						hasSign = true
						intPart = intPart.substring(1)
					}
					val intLen = intPart.length

					if (intLen <= ctx.digitSepStep) append(rawNum).also { return }

					ret.ensureCapacity(ret.length + rawNum.length + intLen / ctx.digitSepStep + 1)
					if (hasSign) append(rawNum[0])
					intPart.forEachIndexed { idx, ch ->
						if (idx > 0 && (intLen - idx) % ctx.digitSepStep == 0) append(ctx.digitSepChar!!)
						append(ch)
					}

					if (parts.size >= 2) append('.').append(parts[1])
				}

				is String  -> ret.apply {
					if (!ctx.minify && ctx.rawStr && tgtVal.length >= 3 && !tgtVal.contains('`') && tgtVal.trim()
							.any { it in "\r\n" }
					) {
						val strIndent = indent + ctx.indentUnit
						ret.ensureCapacity(ret.length + tgtVal.length + strIndent.length * 6)

						append('`').append(ctx.newline)
						tgtVal.trimIndent().trim().lineSequence().forEach { line ->
							if (line.isNotEmpty()) append(strIndent)
							append(line).append(ctx.newline)
						}
						append(indent).append('`')
						return
					}

					if (level == 0 && ctx.fullWidth && ret.last() == ' ') ret.setLength(ret.lastIndex)
					append(escapeString(tgtVal, isDesc = false, fullWidth = ctx.fullWidth))
				}

				is FVVV    -> ret.apply {
					if (ctx.fwwStyle && tgtVal.desc.isNotEmpty()) {
						append(escapeString(tgtVal.desc, isDesc = true, fullWidth = ctx.fullWidth))
						if (!ctx.minify && !ctx.fullWidth) append(' ')
					}
					toStringFWV(ctx, tgtVal, ret, indent, level)
					if (!ctx.noDescs && !ctx.fwwStyle && tgtVal.desc.isNotEmpty()) {
						if (!ctx.minify && !ctx.fullWidth) append(' ')
						append(escapeString(tgtVal.desc, isDesc = true, fullWidth = ctx.fullWidth))
					}
				}
			}
		}

		if (name.isEmpty() || (value !is String && isEmpty() && nodes.isEmpty())) return
		var name = name
		ret.ensureCapacity(ret.length + nodes.size * 6)

		var tgtNode = this
		if (ctx.flattenPaths) name = buildString(name.length + 6) {
			append(name)

			while (tgtNode.nodes.size == 1 && (ctx.noDescs || tgtNode.desc.isEmpty()) && (ctx.noLinks || tgtNode.link.isEmpty())) {
				val nodePair = tgtNode.nodes.entries.first()

				append('.').append((nodePair.key))
				tgtNode = nodePair.value
			}
		}

		var indent = ""
		if (!ctx.minify && level > 0) indent = ctx.indentUnit.repeat(level).also { ret.append(it) }
		ret.append(name).append(ctx.assignOp)

		if (!ctx.noLinks && tgtNode.link.isNotEmpty()) ret.append(tgtNode.link)
		else if (tgtNode.nodes.isNotEmpty()) {
			if (ctx.fwwStyle && tgtNode.desc.isNotEmpty()) {
				ret.append(escapeString(tgtNode.desc, isDesc = true))
				if (!ctx.minify) ret.append(' ')
			}
			if (ctx.fullWidth && ret.last() == ' ') ret.setLength(ret.lastIndex)
			toStringFWV(ctx, tgtNode, ret, indent, level)
		} else if (tgtNode.value !is List<*>) toStringValue(ctx, tgtNode.value!!, ret, indent)
		else {
			var multiline = false
			if (!ctx.minify && !ctx.listSingle) multiline = tgtNode.`is`<List<FVVV>>() || run {
				var longItems = 0
				(tgtNode.value as List<*>).any { item ->
					when (item) {
						is String -> if (item.length + 2 >= 16) ++longItems
						else      -> if ("$item".length >= 16) ++longItems
					}
					longItems >= 6
				}
			}

			val valueIndent = indent + ctx.indentUnit
			val valueLevel = level + 1

			if (ctx.fullWidth && ret.last() == ' ') ret.setLength(ret.lastIndex)
			ret.append(ctx.listBegin)
			if (multiline) ret.append(ctx.newline)

			(tgtNode.value as List<*>).forEachIndexed { idx, item ->
				if (multiline) ret.append(valueIndent)
				toStringValue(ctx, item!!, ret, valueIndent, valueLevel)
				if (if (multiline) ctx.forceSep else idx != (tgtNode.value as List<*>).lastIndex) {
					ret.append(ctx.itemSep)
					if (!multiline && !ctx.fullWidth && !ctx.minify) ret.append(' ')
				}
				if (multiline) ret.append(ctx.newline)
			}

			if (multiline) ret.append(indent)
			ret.append(ctx.listEnd)
		}

		if (!ctx.noDescs && tgtNode.desc.isNotEmpty() && ((tgtNode.nodes.isEmpty() && (!tgtNode.`is`<List<FVVV>>())) || tgtNode.link.isNotEmpty() || !ctx.fwwStyle)) {
			if (!ctx.minify && (!ctx.fullWidth || tgtNode.link.isNotEmpty() || (tgtNode.nodes.isEmpty() && tgtNode.value !is List<*> && tgtNode.value !is String) || (tgtNode.value is String && ret.last() == '`'))) ret.append(
				' '
			)
			ret.append(escapeString(tgtNode.desc, isDesc = true))
		}

		if (ctx.minify || ctx.forceSep) ret.append(ctx.stmtSep)
		if (!ctx.minify && !isBack) ret.append(ctx.newline)
	}
}