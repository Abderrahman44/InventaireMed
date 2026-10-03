package org.abdat.inventory.util

import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.abdat.inventory.domain.model.InstitutionalHeader
import org.abdat.inventory.domain.model.InstitutionalHeaderHolder

actual object DocxExporter {

    private fun escapeXml(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> {
                    // Filter out invalid XML 1.0 control characters
                    val code = c.code
                    if (code == 0x9 || code == 0xA || code == 0xD || (code in 0x20..0xD7FF) || (code in 0xE000..0xFFFD)) {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun isNumericOrAmount(text: String): Boolean {
        val trimmed = text.trim()
            .replace(" ", "")
            .replace("DZD", "")
            .replace("DA", "")
            .replace("%", "")
            .replace(",", ".")
        if (trimmed.isEmpty()) return false
        return trimmed.toDoubleOrNull() != null
    }

    actual fun generateGenericTableDocx(
        filePath: String,
        documentTitle: String,
        subtitle: String,
        metadata: Map<String, String>,
        headers: List<String>,
        rows: List<List<String>>,
        institutionalHeader: InstitutionalHeader
    ) {
        val file = File(filePath)
        file.parentFile?.mkdirs()

        val isLandscape = headers.size > 5
        val pageWidth = if (isLandscape) 16838 else 11906
        val pageHeight = if (isLandscape) 11906 else 16838
        val orientation = if (isLandscape) "landscape" else "portrait"

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            // 1. [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>""".trimIndent()
            zip.write(contentTypes.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 2. _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            val packageRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()
            zip.write(packageRels.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 3. word/_rels/document.xml.rels
            zip.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            val docRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".trimIndent()
            zip.write(docRels.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 4. word/styles.xml
            zip.putNextEntry(ZipEntry("word/styles.xml"))
            val styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Segoe UI" w:hAnsi="Segoe UI" w:cs="Segoe UI"/>
        <w:sz w:val="20"/>
        <w:color w:val="1E293B"/>
      </w:rPr>
    </w:rPrDefault>
  </w:docDefaults>
</w:styles>""".trimIndent()
            zip.write(styles.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 5. word/document.xml
            zip.putNextEntry(ZipEntry("word/document.xml"))
            val docBuilder = StringBuilder()
            docBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <w:body>
""")

            // Institutional Header
            if (!institutionalHeader.isBlank) {
                if (institutionalHeader.line1.isNotBlank()) {
                    docBuilder.append("""
    <w:p>
      <w:pPr><w:jc w:val="center"/><w:spacing w:before="0" w:after="40"/></w:pPr>
      <w:r>
        <w:rPr><w:b/><w:sz w:val="22"/><w:color w:val="1E293B"/></w:rPr>
        <w:t>${escapeXml(institutionalHeader.line1.uppercase())}</w:t>
      </w:r>
    </w:p>""")
                }
                if (institutionalHeader.line2.isNotBlank()) {
                    docBuilder.append("""
    <w:p>
      <w:pPr><w:jc w:val="center"/><w:spacing w:before="0" w:after="30"/></w:pPr>
      <w:r>
        <w:rPr><w:b/><w:sz w:val="18"/><w:color w:val="475569"/></w:rPr>
        <w:t>${escapeXml(institutionalHeader.line2)}</w:t>
      </w:r>
    </w:p>""")
                }
                if (institutionalHeader.line3.isNotBlank()) {
                    docBuilder.append("""
    <w:p>
      <w:pPr><w:jc w:val="center"/><w:spacing w:before="0" w:after="30"/></w:pPr>
      <w:r>
        <w:rPr><w:sz w:val="18"/><w:color w:val="475569"/></w:rPr>
        <w:t>${escapeXml(institutionalHeader.line3)}</w:t>
      </w:r>
    </w:p>""")
                }
                if (institutionalHeader.line4.isNotBlank()) {
                    docBuilder.append("""
    <w:p>
      <w:pPr><w:jc w:val="center"/><w:spacing w:before="0" w:after="100"/></w:pPr>
      <w:r>
        <w:rPr><w:b/><w:sz w:val="18"/><w:color w:val="2563EB"/></w:rPr>
        <w:t>${escapeXml(institutionalHeader.line4)}</w:t>
      </w:r>
    </w:p>""")
                }
                // Separator rule
                docBuilder.append("""
    <w:p>
      <w:pPr>
        <w:pBdr><w:bottom w:val="single" w:sz="8" w:space="1" w:color="CBD5E1"/></w:pBdr>
        <w:spacing w:before="0" w:after="160"/>
      </w:pPr>
    </w:p>""")
            }

            // Document Title
            docBuilder.append("""
    <w:p>
      <w:pPr><w:spacing w:before="120" w:after="60"/></w:pPr>
      <w:r>
        <w:rPr><w:b/><w:sz w:val="28"/><w:color w:val="0F172A"/></w:rPr>
        <w:t>${escapeXml(documentTitle)}</w:t>
      </w:r>
    </w:p>""")

            // Subtitle
            if (subtitle.isNotBlank()) {
                docBuilder.append("""
    <w:p>
      <w:pPr><w:spacing w:before="0" w:after="120"/></w:pPr>
      <w:r>
        <w:rPr><w:i/><w:sz w:val="18"/><w:color w:val="64748B"/></w:rPr>
        <w:t>${escapeXml(subtitle)}</w:t>
      </w:r>
    </w:p>""")
            }

            // Metadata summary card
            if (metadata.isNotEmpty()) {
                docBuilder.append("""
    <w:tbl>
      <w:tblPr>
        <w:tblW w:w="5000" w:type="pct"/>
        <w:tblBorders>
          <w:top w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
          <w:left w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
          <w:bottom w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
          <w:right w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
          <w:insideH w:val="single" w:sz="4" w:space="0" w:color="F1F5F9"/>
          <w:insideV w:val="none"/>
        </w:tblBorders>
        <w:tblCellMar>
          <w:top w:w="80" w:type="dxa"/>
          <w:bottom w:w="80" w:type="dxa"/>
          <w:left w:w="120" w:type="dxa"/>
          <w:right w:w="120" w:type="dxa"/>
        </w:tblCellMar>
      </w:tblPr>""")
                for ((k, v) in metadata) {
                    docBuilder.append("""
      <w:tr>
        <w:tc>
          <w:tcPr><w:tcW w:w="1800" w:type="dxa"/><w:shd w:val="clear" w:color="auto" w:fill="F8FAFC"/></w:tcPr>
          <w:p><w:r><w:rPr><w:b/><w:sz w:val="16"/><w:color w:val="475569"/></w:rPr><w:t>${escapeXml(k)}</w:t></w:r></w:p>
        </w:tc>
        <w:tc>
          <w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="FFFFFF"/></w:tcPr>
          <w:p><w:r><w:rPr><w:sz w:val="16"/><w:color w:val="0F172A"/></w:rPr><w:t>${escapeXml(v)}</w:t></w:r></w:p>
        </w:tc>
      </w:tr>""")
                }
                docBuilder.append("""
    </w:tbl>
    <w:p><w:pPr><w:spacing w:before="0" w:after="140"/></w:pPr></w:p>""")
            }

            // Data Table
            docBuilder.append("""
    <w:tbl>
      <w:tblPr>
        <w:tblW w:w="5000" w:type="pct"/>
        <w:jc w:val="center"/>
        <w:tblBorders>
          <w:top w:val="single" w:sz="6" w:space="0" w:color="2563EB"/>
          <w:left w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
          <w:bottom w:val="single" w:sz="6" w:space="0" w:color="94A3B8"/>
          <w:right w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
          <w:insideH w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
          <w:insideV w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
        </w:tblBorders>
        <w:tblCellMar>
          <w:top w:w="90" w:type="dxa"/>
          <w:bottom w:w="90" w:type="dxa"/>
          <w:left w:w="120" w:type="dxa"/>
          <w:right w:w="120" w:type="dxa"/>
        </w:tblCellMar>
      </w:tblPr>""")

            // Table Header Row
            docBuilder.append("""
      <w:tr>
        <w:trPr><w:tblHeader/></w:trPr>""")
            for (header in headers) {
                val align = if (isNumericOrAmount(header)) "right" else "left"
                docBuilder.append("""
        <w:tc>
          <w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="2563EB"/></w:tcPr>
          <w:p>
            <w:pPr><w:jc w:val="$align"/></w:pPr>
            <w:r>
              <w:rPr><w:b/><w:sz w:val="17"/><w:color w:val="FFFFFF"/></w:rPr>
              <w:t>${escapeXml(header)}</w:t>
            </w:r>
          </w:p>
        </w:tc>""")
            }
            docBuilder.append("""
      </w:tr>""")

            // Data Rows
            rows.forEachIndexed { index, row ->
                val bg = if (index % 2 == 1) "F8FAFC" else "FFFFFF"
                docBuilder.append("""
      <w:tr>""")
                headers.indices.forEach { colIndex ->
                    val cellText = if (colIndex < row.size) row[colIndex] else ""
                    val align = if (isNumericOrAmount(cellText)) "right" else "left"
                    docBuilder.append("""
        <w:tc>
          <w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="$bg"/></w:tcPr>
          <w:p>
            <w:pPr><w:jc w:val="$align"/></w:pPr>
            <w:r>
              <w:rPr><w:sz w:val="16"/><w:color w:val="1E293B"/></w:rPr>
              <w:t>${escapeXml(cellText)}</w:t>
            </w:r>
          </w:p>
        </w:tc>""")
                }
                docBuilder.append("""
      </w:tr>""")
            }

            docBuilder.append("""
    </w:tbl>""")

            // Document Footer / Page Setup
            docBuilder.append("""
    <w:sectPr>
      <w:pgSz w:w="$pageWidth" w:h="$pageHeight" w:orient="$orientation"/>
      <w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134" w:header="720" w:footer="720" w:gutter="0"/>
    </w:sectPr>
  </w:body>
</w:document>""")

            zip.write(docBuilder.toString().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
    }
}
