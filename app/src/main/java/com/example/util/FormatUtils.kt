package com.example.util

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * 高亮匹配文本关键词
     */
    fun buildHighlightedText(
        text: String,
        query: String,
        colorScheme: ColorScheme
    ): AnnotatedString {
        if (query.isBlank()) return AnnotatedString(text)

        val highlightStyle = SpanStyle(
            background = colorScheme.primaryContainer,
            color = colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )

        return buildAnnotatedString {
            var startIndex = 0
            val lowerText = text.lowercase(Locale.getDefault())
            val lowerQuery = query.lowercase(Locale.getDefault())

            while (startIndex < text.length) {
                val index = lowerText.indexOf(lowerQuery, startIndex)
                if (index == -1) {
                    append(text.substring(startIndex))
                    break
                } else {
                    if (index > startIndex) {
                        append(text.substring(startIndex, index))
                    }
                    val endIndex = index + query.length
                    withStyle(style = highlightStyle) {
                        append(text.substring(index, endIndex))
                    }
                    startIndex = endIndex
                }
            }
        }
    }

    /**
     * 自动格式化内容：清理多余空行、统一行距与尾部空格
     */
    fun formatContent(content: String): String {
        val lines = content.lines().map { it.trimEnd() }
        val result = mutableListOf<String>()
        var previousEmpty = false

        for (line in lines) {
            val isEmpty = line.isBlank()
            if (isEmpty) {
                if (!previousEmpty) {
                    result.add("")
                    previousEmpty = true
                }
            } else {
                result.add(line)
                previousEmpty = false
            }
        }
        return result.joinToString("\n").trim()
    }

    /**
     * 计算字数和字符数
     */
    fun countWordsAndChars(text: String): Pair<Int, Int> {
        val charCount = text.length
        val trimmed = text.trim()
        val wordCount = if (trimmed.isEmpty()) {
            0
        } else {
            // 支持中文单字与英文单词统计
            var cjkCount = 0
            val nonCjkBuilder = StringBuilder()

            for (ch in trimmed) {
                if (ch.code in 0x4E00..0x9FFF || ch.code in 0x3400..0x4DBF) {
                    cjkCount++
                    nonCjkBuilder.append(' ')
                } else {
                    nonCjkBuilder.append(ch)
                }
            }

            val englishWords = nonCjkBuilder.toString()
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
                .size

            cjkCount + englishWords
        }
        return Pair(wordCount, charCount)
    }
}
