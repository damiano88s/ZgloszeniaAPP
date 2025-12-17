package com.example.zgloszeniaapp

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory

import java.io.InputStream

object GrafikExcelReader {

    fun read(input: InputStream): List<GrafikRow> {
        val result = mutableListOf<GrafikRow>()

        input.use { stream ->
            val wb = WorkbookFactory.create(stream)
            val sheet = wb.getSheet("Grafik") ?: wb.getSheetAt(0)

            val headerRowIndex = findHeaderRow(sheet) ?: return emptyList()
            val headerRow = sheet.getRow(headerRowIndex) ?: return emptyList()

            val colMap = buildColumnMap(headerRow)
            val cImie = colMap["imie"] ?: return emptyList()
            val cDzien = colMap["dzien"] ?: return emptyList()
            val cUlica = colMap["ulica"] ?: return emptyList()

            for (r in (headerRowIndex + 1)..sheet.lastRowNum) {
                val row = sheet.getRow(r) ?: continue

                val imie = cellString(row.getCell(cImie)).trim()
                val dzien = cellString(row.getCell(cDzien)).trim()
                val ulica = cellString(row.getCell(cUlica)).trim()

                if (imie.isBlank() && dzien.isBlank() && ulica.isBlank()) continue
                if (imie.isBlank() || dzien.isBlank() || ulica.isBlank()) continue

                result += GrafikRow(imie, dzien, ulica)
            }

            wb.close()
        }

        return result
    }

    private fun findHeaderRow(sheet: org.apache.poi.ss.usermodel.Sheet): Int? {
        val max = minOf(sheet.lastRowNum, 30)
        for (i in 0..max) {
            val row = sheet.getRow(i) ?: continue
            val values = (0..row.lastCellNum).mapNotNull { idx ->
                val s = cellString(row.getCell(idx)).trim().lowercase()
                if (s.isBlank()) null else s
            }.toSet()

            if (values.contains("imie") && values.contains("dzien") && values.contains("ulica")) {
                return i
            }
        }
        return null
    }

    private fun buildColumnMap(headerRow: org.apache.poi.ss.usermodel.Row): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for (c in 0 until headerRow.lastCellNum) {
            val key = cellString(headerRow.getCell(c)).trim().lowercase()
            when (key) {
                "imie" -> map["imie"] = c
                "dzien" -> map["dzien"] = c
                "ulica" -> map["ulica"] = c
            }
        }
        return map
    }

    private fun cellString(cell: org.apache.poi.ss.usermodel.Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue ?: ""
            CellType.NUMERIC -> {
                val v = cell.numericCellValue
                if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try { cell.stringCellValue ?: "" } catch (_: Exception) {
                    try { cell.numericCellValue.toString() } catch (_: Exception) { "" }
                }
            }
            else -> ""
        }
    }
}
