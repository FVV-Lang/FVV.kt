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

class FVVV(
	var value: Any? = null,
	var sub: MutableMap<String, FVVV> = mutableMapOf(),
	var desc: String = "",
	var link: String = "",
) {
	operator fun get(key: String) = sub.getOrPut(key) { FVVV() }
	operator fun set(key: String, v: Any?) {
		sub.getOrPut(key) { FVVV() }.value = v
	}

	override fun equals(other: Any?) = when {
		this === other -> true
		other !is FVVV -> false
		else           -> value == other.value
	}

	override fun hashCode() = value?.hashCode() ?: 0
	override fun toString() = "$value"

	inline fun <reified T> asType(default: T? = null): T? {
		var v = value
		while (v is FVVV) v = v.value
		return v as? T ?: default
	}

	fun asBool(default: Boolean = false) = asType(default) ?: default
	fun asInt(default: Int = 0) = asType(default) ?: default
	fun asDouble(default: Double = 0.0) = asType(default) ?: default
	fun asString(default: String = "") = asType(default) ?: default
	fun asBools(default: List<Boolean> = emptyList()) =
		(asType<List<*>>()?.mapNotNull { it as? Boolean } ?: default)

	fun asInts(default: List<Int> = emptyList()) =
		(asType<List<*>>()?.mapNotNull { it as? Int } ?: default)

	fun asDoubles(default: List<Double> = emptyList()) =
		(asType<List<*>>()?.mapNotNull { it as? Double } ?: default)

	fun asStrings(default: List<String> = emptyList()) =
		(asType<List<*>>()?.mapNotNull { it as? String } ?: default)

	val bool get() = asType<Boolean>() ?: false
	val int get() = asType<Int>() ?: 0
	val double get() = asType<Double>() ?: 0.0
	val string get() = asType<String>() ?: ""
	val bools get() = (asType<List<*>>()?.mapNotNull { it as? Boolean } ?: emptyList())
	val ints get() = (asType<List<*>>()?.mapNotNull { it as? Int } ?: emptyList())
	val doubles get() = (asType<List<*>>()?.mapNotNull { it as? Double } ?: emptyList())
	val strings get() = (asType<List<*>>()?.mapNotNull { it as? String } ?: emptyList())

	val isEmpty: Boolean
		get() = when (value) {
			null       -> true
			is String  -> (value as String).isEmpty()
			is List<*> -> (value as List<*>).isEmpty()
			is FVVV    -> (value as FVVV).isEmpty
			else       -> false
		}
	val isNotEmpty get() = !isEmpty

	inline fun <reified T> isType(): Boolean {
		var v = value
		while (v is FVVV) v = v.value
		return v is T
	}

	fun getType(): KClass<*>? = when (value) {
		is FVVV -> (value as FVVV).getType()
		else    -> value?.let { it::class }
	}

	fun print(type: String = "common", indentLv: Int = 0): String {
		var isMin = false
		var isBiglist = false
		var isNodesc = false
		when (type) {
			"min"     -> isMin = true
			"biglist" -> isBiglist = true
			"nodesc"  -> isNodesc = true
		}
		val result = StringBuilder()
		fun printFunc(path: String, node: FVVV, indentLv: Int) {
			if (path.isEmpty() || (node.isEmpty && node.sub.isEmpty())) return
			val indent = " ".repeat(indentLv * 2)
			if (node.sub.isNotEmpty() && node.link.isEmpty()) {
				if (isMin) result.append("$path={")
				else result.append("$indent$path = {\n")
			}
			if (node.link.isNotEmpty() || node.value != null) {
				if (isMin) result.append("$path=")
				else result.append("$indent$path = ")
				if (node.link.isNotEmpty()) result.append(node.link)
				else {
					when (val v = node.value) {
						is String  -> result.append('"').append(v.replace("\"", "\\\"")).append('"')
						is List<*> -> {
							val listIndent = " ".repeat((indentLv + 1) * 2)
							result.append('[')
							if (isBiglist || v.first() is FVVV) result.appendLine()
							if (v.first() is FVVV) v.forEach {
								if (!isMin) result.append(listIndent)
								result.append('{')
								if (!isMin) result.appendLine()
								result.append((it as FVVV).print(type, indentLv + 2))
								if (isMin) result.append(";}")
								else result.apply {
									appendLine()
									append(listIndent)
									append('}')
								}
								if (it.desc.isNotEmpty()) {
									if (!isMin) result.append(' ')
									result.append('<').append(it.desc.replace(">", "\\>")).append('>')
								}
								if (isMin) result.append(',')
								else result.appendLine()
							}
							else (v.takeIf { it.first() !is String } ?: v.map {
								"\"${
									"$it".replace("\"", "\\\"")
								}\""
							}).forEach { item ->
								if (isBiglist) result.append(listIndent)
								result.append(item)
								if (isBiglist) result.appendLine()
								else {
									result.append(',')
									if (!isMin) result.append(' ')
								}
							}
							if (isBiglist || v.first() is FVVV) result.append(indent)
							else if (v.isNotEmpty()) {
								result.setLength(result.length - 1)
								if (!isMin) result.setLength(result.length - 1)
							}
							result.append(']')
						}

						else       -> result.append(v)
					}
				}
			} else node.sub.forEach { (k, v) -> printFunc(k, v, indentLv + 1) }
			if (node.sub.isNotEmpty() && node.link.isEmpty()) {
				if (!isMin) result.append(indent)
				result.append('}')
			}
			if (node.desc.isNotEmpty() && !isMin && !isNodesc) result.append(" <")
				.append(node.desc.replace(">", "\\>")).append('>')
			if (isMin) result.append(';') else result.appendLine()
		}
		sub.forEach { (k, v) -> printFunc(k, v, indentLv) }
		if (result.isNotEmpty()) result.setLength(result.length - 1)
		return "$result"
	}

	fun addFromString(targetTxt: String) {
		var txt = targetTxt.trimStart { it == '﻿' }.trim().replace("\r\n", "\n").replace("\r", "\n")
		if (txt.isEmpty()) return
		if (!txt.endsWith("}")) txt += "\n"

		data class FVVVDat(
			val valueName: StringBuilder = StringBuilder(),
			var idxDesc: String = "",
			var valueNames: MutableList<String> = mutableListOf(),
			val groupNames: MutableList<String> = mutableListOf(),
			val lastGroupNames: MutableList<List<String>> = mutableListOf(),
			val tmpFVVs: MutableList<FVVV> = mutableListOf(),
			var inValue: Boolean = false,
			var inList: Boolean = false,
			var isList: Boolean = false,
			var groupNum: Int = 0,
			val rootKey: FVVV = FVVV()
		) {
			var idxKey = rootKey
		}

		fun getKey(paths: List<String>, rootKey: FVVV): FVVV {
			var tmpKey = rootKey
			for (path in paths) tmpKey = tmpKey[path]
			return tmpKey
		}

		fun findKey(path: String, stackDat: List<FVVVDat>): FVVV {
			val tmpNames = path.trim().split('.')
			lateinit var tmpKey: FVVV
			fun find(idxDat: FVVVDat): Boolean {
				tmpKey = idxDat.idxKey
				for (key in tmpNames) if (tmpKey.sub.containsKey(key)) tmpKey = tmpKey[key]
				else break
				if (tmpKey.isEmpty && tmpKey.sub.isEmpty()) {
					tmpKey = idxDat.rootKey
					for (key in tmpNames) if (tmpKey.sub.containsKey(key)) tmpKey = tmpKey[key]
					else break
				}
				return tmpKey.isNotEmpty || tmpKey.sub.isNotEmpty()
			}
			for (idx in stackDat.size - 1 downTo 0) {
				find(stackDat[idx])
				if (tmpKey.isNotEmpty || tmpKey.sub.isNotEmpty()) return tmpKey
			}
			return tmpKey
		}

		val tmpDesc = StringBuilder()
		val value = StringBuilder()
		val values = mutableListOf<String>()
		var oldFVV = false
		var endGroup = false
		var isRealChar: Boolean
		var inDesc = false
		var inStr = false
		var isStr = false
		var isAllStr = false
		var isEmptyStr = false
		var idxChar: Char
		var lastChar = '\u0000'
		var idx = 0
		val fvvStack = mutableListOf(FVVVDat(rootKey = this))
		val runes = txt.toCharArray()
		while (idx < runes.size) {
			idxChar = runes[idx]
			isRealChar = lastChar != '\\'
			val idxDat = fvvStack.last().apply { idxKey = rootKey }
			if (let {
					if (inDesc) {
						if (idxChar != '>' || !isRealChar) {
							if (idxChar == '>') tmpDesc.setLength(tmpDesc.length - 1)
							if (idxDat.inValue || idxDat.groupNum > 0) tmpDesc.append(idxChar)
							return@let false
						} else {
							idxDat.idxDesc = "$tmpDesc"
							tmpDesc.clear()
							inDesc = false
							return@let false
						}
					} else {
						if (!inStr && idxChar in listOf(' ', '\t')) return@let false
						if (idxChar == '<') {
							inDesc = true
							return@let false
						}
					}
					if (idxDat.inValue) {
						if (inStr) {
							if (idxChar == '"') {
								if (isRealChar) {
									isEmptyStr = value.isEmpty()
									inStr = false
									return@let false
								} else {
									value.apply {
										setLength(length - 1)
										append(idxChar)
									}
									return@let false
								}
							} else {
								value.append(idxChar)
								return@let false
							}
						} else {
							when {
								idxChar == '"'                                     -> {
									inStr = true
									isStr = true
									isAllStr = true
									return@let false
								}

								idxChar == '['                                     -> {
									idxDat.inList = true
									idxDat.isList = true
									return@let false
								}

								idxDat.inList && idxChar == '{'                    -> {
									fvvStack.add(FVVVDat())
									return@let false
								}

								idxDat.inList && idxChar in listOf(']', ',', '\n') -> {
									if (idxChar == ']') {
										idxDat.inList = false
										var pos = 1
										var inListDesc = false
										while (true) {
											if (when (txt.getOrNull(idx - pos) ?: break) {
													'<'       -> if (!inListDesc) true else (txt.getOrNull(
														idx - pos - 1
													) != '\\').also {
														inListDesc = false
													}

													'>'       -> false.also { inListDesc = true }
													' ', '\t' -> false
													',', '\n' -> !inListDesc
													else      -> !inListDesc
												}
											) break
											++pos
										}
										if (txt.getOrNull(idx - pos) in listOf(
												',', '\n'
											)
										) return@let false
									} else if (idxDat.tmpFVVs.isEmpty() && value.isEmpty() && (!isAllStr || !isEmptyStr)) return@let false
									val valueStr = "$value"
									if ((isAllStr && isStr) || valueStr in listOf(
											"true", "false"
										) || valueStr.toIntOrNull() != null || valueStr.toDoubleOrNull() != null
									) {
										values.add(valueStr)
									} else {
										idxDat.idxKey = getKey(
											listOf(valueStr), getKey(idxDat.groupNames, idxDat.rootKey)
										)
										when (val v = idxDat.idxKey.value) {
											is String  -> values.add(v)
											is List<*> -> @Suppress(
												"UNCHECKED_CAST"
											) if (v.first() is FVVV) idxDat.tmpFVVs.addAll(
												(v as List<FVVV>).toList()
											)
											else values.addAll(v.map { "$it" })

											else       -> values.add("$v")
										}
									}
									if (idxDat.tmpFVVs.isNotEmpty() && idxDat.idxDesc.isNotEmpty()) {
										idxDat.tmpFVVs.last().desc = idxDat.idxDesc
										idxDat.idxDesc = ""
									}
									if (isEmptyStr) isEmptyStr = false
									else value.clear()
									isStr = false
									return@let false
								}

								idxChar == '{'                                     -> {
									idxDat.groupNames.addAll(idxDat.valueNames)
									idxDat.lastGroupNames.add(idxDat.valueNames.toList())
									idxDat.valueNames.clear()
									++idxDat.groupNum
									idxDat.inValue = false
									return@let false
								}

								idxChar in listOf(';', '\n')                       -> {
									idxDat.idxKey = getKey(
										idxDat.valueNames, getKey(idxDat.groupNames, idxDat.rootKey)
									)
									if (idxDat.isList) {
										idxDat.idxKey.value = when {
											values.isEmpty() && idxDat.tmpFVVs.isEmpty() -> null
											idxDat.tmpFVVs.isNotEmpty()                  -> idxDat.tmpFVVs.toList()
											isAllStr                                     -> values.toList()
											values[0] in listOf(
												"true", "false"
											)                                            -> values.map { it == "true" }

											values[0].toIntOrNull() != null              -> values.mapNotNull { it.toIntOrNull() }
											values[0].toDoubleOrNull() != null           -> values.mapNotNull { it.toDoubleOrNull() }
											else                                         -> null
										}
									} else {
										val valueStr = "$value"
										idxDat.idxKey.value = when {
											isAllStr                            -> valueStr
											valueStr in listOf("true", "false") -> valueStr == "true"
											valueStr.toIntOrNull() != null      -> valueStr.toInt()
											valueStr.toDoubleOrNull() != null   -> valueStr.toDouble()
											else                                -> {
												val tmpKey = findKey(valueStr, fvvStack)
												if (tmpKey.isNotEmpty || tmpKey.sub.isNotEmpty()) {
													idxDat.idxKey.link = valueStr
													if (tmpKey.sub.isEmpty()) tmpKey.value else {
														idxDat.idxKey.sub = tmpKey.sub
														null
													}
												} else null
											}
										}
									}
									idxDat.idxKey.desc = idxDat.idxDesc
									idxDat.idxDesc = ""
									value.clear()
									values.clear()
									idxDat.valueNames.clear()
									idxDat.tmpFVVs.clear()
									idxDat.inValue = false
									isStr = false
									isAllStr = false
									idxDat.isList = false
									return@let false
								}

								else                                               -> {
									value.append(idxChar)
									return@let false
								}
							}
						}
					} else if (!oldFVV && idxChar == '{' && idxDat.valueName.isEmpty()) {
						oldFVV = true
						return@let false
					} else if (idxChar == '=') {
						idxDat.valueNames = "${idxDat.valueName}".trim().split(".").toMutableList()
						idxDat.valueName.clear()
						idxDat.inValue = true
						return@let false
					} else if (endGroup && idxChar in listOf(';', '\n') && idxDat.groupNum > 0) {
						endGroup = false
						if (idxDat.idxDesc.isNotEmpty()) {
							getKey(idxDat.groupNames, idxDat.rootKey).desc = idxDat.idxDesc
							idxDat.idxDesc = ""
						}
						repeat(idxDat.lastGroupNames.last().size) {
							idxDat.groupNames.removeAt(idxDat.groupNames.size - 1)
						}
						idxDat.lastGroupNames.removeAt(idxDat.lastGroupNames.size - 1)
						--idxDat.groupNum
						return@let false
					} else if (idxChar == '}') {
						if (idxDat.groupNum == 0) {
							if (fvvStack.size > 1) {
								fvvStack.last().rootKey.let {
									fvvStack.removeLast()
									fvvStack.last().tmpFVVs.add(it)
								}
								return@let false
							} else return@let true
						} else {
							endGroup = true
							return@let false
						}
					} else {
						idxDat.valueName.append(idxChar)
						return@let false
					}
				}) break
			lastChar = idxChar
			++idx
		}
	}
}