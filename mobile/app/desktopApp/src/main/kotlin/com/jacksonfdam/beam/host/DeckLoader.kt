package com.jacksonfdam.beam.host

import com.jacksonfdam.beam.pdf.PdfDocument
import com.jacksonfdam.beam.pdf.openPdfDocument
import com.jacksonfdam.beam.protocol.DeckInfo
import com.jacksonfdam.beam.protocol.DeckNotes
import java.io.File

/** A loaded deck: its rendered pages plus the host-owned speaker-notes sidecar. */
class HostDeck(
    val info: DeckInfo,
    val document: PdfDocument,
    val notes: DeckNotes,
)

/** Loads a PDF from disk and pairs it with a `<deck>.notes.json` sidecar if present. */
object DeckLoader {
    fun load(file: File): HostDeck {
        val document = openPdfDocument(file.readBytes())
        val sidecar = File(file.parentFile, "${file.nameWithoutExtension}.notes.json")
        val notes = if (sidecar.exists()) {
            DeckNotes.parse(sidecar.readText()) ?: DeckNotes.EMPTY
        } else {
            DeckNotes.EMPTY
        }
        val info = DeckInfo(
            id = file.absolutePath,
            title = file.nameWithoutExtension,
            slideCount = document.pageCount,
            hasNotes = notes.notes.isNotEmpty(),
        )
        return HostDeck(info, document, notes)
    }
}
