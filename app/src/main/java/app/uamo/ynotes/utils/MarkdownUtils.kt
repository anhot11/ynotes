package app.uamo.ynotes.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Parses markdown for card and list previews, completely stripping raw markdown
 * characters (*, **, #, >, etc.) while preserving rich styling.
 */
@Composable
fun parseMarkdown(text: String, baseColor: Color = MaterialTheme.colorScheme.onBackground): AnnotatedString {
    return parseMarkdownForPreview(text, baseColor)
}

private val previewCache = android.util.LruCache<String, AnnotatedString>(128)
private val inlinePattern = Regex("(\\*\\*(.*?)\\*\\*)|((?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)|_(.*?)_)|(~~(.*?)~~)|(`(.*?)`)")

/**
 * Strips raw markdown syntax characters (*, **, #, >, [ ], etc.) and renders
 * clean, styled text for note cards and preview lists.
 */
fun parseMarkdownForPreview(text: String, baseColor: Color): AnnotatedString {
    if (text.isBlank()) return AnnotatedString("")
    val cacheKey = "${text.hashCode()}_${baseColor.value}"
    previewCache.get(cacheKey)?.let { return it }

    val builder = AnnotatedString.Builder()
    val lines = text.lines()

    lines.forEachIndexed { index, line ->
        var content = line
        var isHeader = false
        var headerLevel = 0
        var isQuote = false
        var isCheckbox = false
        var isChecked = false
        var isBullet = false

        val headMatch = Regex("^(#{1,3})\\s+(.*)$").find(content)
        if (headMatch != null) {
            isHeader = true
            headerLevel = headMatch.groupValues[1].length
            content = headMatch.groupValues[2]
        } else {
            val quoteMatch = Regex("^>\\s+(.*)$").find(content)
            if (quoteMatch != null) {
                isQuote = true
                content = quoteMatch.groupValues[1]
            } else {
                val checkMatch = Regex("^-\\s*\\[([ xX])\\]\\s*(.*)$").find(content)
                if (checkMatch != null) {
                    isCheckbox = true
                    isChecked = checkMatch.groupValues[1].isNotBlank()
                    content = checkMatch.groupValues[2]
                } else {
                    val bulletMatch = Regex("^[-*+]\\s+(.*)$").find(content)
                    if (bulletMatch != null) {
                        isBullet = true
                        content = bulletMatch.groupValues[1]
                    }
                }
            }
        }

        val lineStart = builder.length

        if (isCheckbox) {
            builder.append(if (isChecked) "☑ " else "☐ ")
        } else if (isBullet) {
            builder.append("• ")
        }

        // Inline formatting
        var lastIdx = 0
        for (m in inlinePattern.findAll(content)) {
            if (m.range.first > lastIdx) {
                builder.append(content.substring(lastIdx, m.range.first))
            }
            if (m.groupValues[1].isNotEmpty()) { // **bold**
                val boldText = m.groupValues[2]
                val start = builder.length
                builder.append(boldText)
                builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, builder.length)
            } else if (m.groupValues[3].isNotEmpty()) { // *italic* or _italic_
                val italicText = m.groupValues[4].ifEmpty { m.groupValues[5] }
                val start = builder.length
                builder.append(italicText)
                builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, builder.length)
            } else if (m.groupValues[6].isNotEmpty()) { // ~~strike~~
                val strikeText = m.groupValues[7]
                val start = builder.length
                builder.append(strikeText)
                builder.addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, builder.length)
            } else if (m.groupValues[8].isNotEmpty()) { // `code`
                val codeText = m.groupValues[9]
                val start = builder.length
                builder.append(codeText)
                builder.addStyle(
                    SpanStyle(
                        background = baseColor.copy(alpha = 0.12f),
                        fontWeight = FontWeight.SemiBold
                    ),
                    start,
                    builder.length
                )
            }
            lastIdx = m.range.last + 1
        }
        if (lastIdx < content.length) {
            builder.append(content.substring(lastIdx))
        }

        val lineEnd = builder.length

        if (isHeader) {
            val (fontSize, weight) = when (headerLevel) {
                1 -> 17.sp to FontWeight.Bold
                2 -> 15.sp to FontWeight.Bold
                else -> 14.sp to FontWeight.SemiBold
            }
            builder.addStyle(SpanStyle(fontWeight = weight, fontSize = fontSize), lineStart, lineEnd)
        } else if (isQuote) {
            builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor.copy(alpha = 0.7f)), lineStart, lineEnd)
        }

        if (index < lines.size - 1) {
            builder.append("\n")
        }
    }

    val res = builder.toAnnotatedString()
    previewCache.put(cacheKey, res)
    return res
}

/**
 * VisualTransformation for live editing.
 * When hideMarkdownSyntax is true (default):
 * - On the line currently being edited (cursor line), the markdown syntax (*, **, #, etc.) turns gray.
 * - On other lines, the markdown syntax symbols are hidden (transparent & near-zero size).
 * When hideMarkdownSyntax is false:
 * - All markdown syntax symbols are visible in gray/subtle tint across all lines.
 */
class MarkdownLiveVisualTransformation(
    private val baseColor: Color,
    private val cursorPosition: Int,
    private val hideMarkdownSyntax: Boolean
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val currentText = text.text
        val formattedText = formatMarkdownLive(currentText, baseColor, cursorPosition, hideMarkdownSyntax)
        return TransformedText(formattedText, OffsetMapping.Identity)
    }
}

class MarkdownVisualTransformation(private val baseColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val currentText = text.text
        val formattedText = formatMarkdownLive(currentText, baseColor, cursorPosition = 0, hideMarkdownSyntax = false)
        return TransformedText(formattedText, OffsetMapping.Identity)
    }
}

private val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
private val italicRegex = Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)|_(.*?)_")
private val strikeRegex = Regex("~~(.*?)~~")
private val headerRegex = Regex("(?m)^(#{1,3})\\s+(.*)$")
private val codeRegex = Regex("`(.*?)`")
private val quoteRegex = Regex("(?m)^>\\s+(.*)$")

fun formatMarkdownLive(
    text: String,
    baseColor: Color,
    cursorPosition: Int,
    hideMarkdownSyntax: Boolean
): AnnotatedString {
    val cursor = cursorPosition.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
    val activeLineRange = lineStart..lineEnd

    val subtleGray = baseColor.copy(alpha = 0.38f)
    val hiddenStyle = SpanStyle(color = Color.Transparent, fontSize = 0.01.sp)

    fun getDelimiterStyle(matchRange: IntRange): SpanStyle {
        val isActive = matchRange.first in activeLineRange || matchRange.last in activeLineRange
        return if (hideMarkdownSyntax) {
            if (isActive) SpanStyle(color = subtleGray) else hiddenStyle
        } else {
            SpanStyle(color = subtleGray)
        }
    }

    return buildAnnotatedString {
        append(text)

        // Bold: **text**
        boldRegex.findAll(text).forEach { match ->
            if (match.value.length >= 4) {
                val dStyle = getDelimiterStyle(match.range)
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first + 2, match.range.last - 1)
                addStyle(dStyle, match.range.first, match.range.first + 2)
                addStyle(dStyle, match.range.last - 1, match.range.last + 1)
            }
        }

        // Italic: *text* or _text_
        italicRegex.findAll(text).forEach { match ->
            if (match.value.length >= 2) {
                val dStyle = getDelimiterStyle(match.range)
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first + 1, match.range.last)
                addStyle(dStyle, match.range.first, match.range.first + 1)
                addStyle(dStyle, match.range.last, match.range.last + 1)
            }
        }

        // Strikethrough: ~~text~~
        strikeRegex.findAll(text).forEach { match ->
            if (match.value.length >= 4) {
                val dStyle = getDelimiterStyle(match.range)
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), match.range.first + 2, match.range.last - 1)
                addStyle(dStyle, match.range.first, match.range.first + 2)
                addStyle(dStyle, match.range.last - 1, match.range.last + 1)
            }
        }

        // Headers: # Header (1, 2, or 3)
        headerRegex.findAll(text).forEach { match ->
            val level = match.groupValues[1].length
            val prefixLen = level + 1
            val fontSize = when (level) {
                1 -> 24.sp
                2 -> 20.sp
                else -> 18.sp
            }
            val dStyle = getDelimiterStyle(match.range)
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize), match.range.first + prefixLen, match.range.last + 1)
            addStyle(dStyle, match.range.first, match.range.first + prefixLen)
        }

        // Code inline: `code`
        val codeBgColor = baseColor.copy(alpha = 0.1f)
        codeRegex.findAll(text).forEach { match ->
            if (match.value.length >= 2) {
                val dStyle = getDelimiterStyle(match.range)
                addStyle(SpanStyle(background = codeBgColor, fontWeight = FontWeight.SemiBold), match.range.first + 1, match.range.last)
                addStyle(dStyle, match.range.first, match.range.first + 1)
                addStyle(dStyle, match.range.last, match.range.last + 1)
            }
        }

        // Quotes: > Quote
        val quoteColor = baseColor.copy(alpha = 0.65f)
        quoteRegex.findAll(text).forEach { match ->
            val dStyle = getDelimiterStyle(match.range)
            addStyle(SpanStyle(color = quoteColor, fontStyle = FontStyle.Italic), match.range.first + 2, match.range.last + 1)
            addStyle(dStyle, match.range.first, match.range.first + 2)
        }
    }
}

fun parseMarkdownSync(text: String, baseColor: Color): AnnotatedString {
    return formatMarkdownLive(text, baseColor, cursorPosition = 0, hideMarkdownSyntax = false)
}
