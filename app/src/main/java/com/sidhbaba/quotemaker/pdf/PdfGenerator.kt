package com.sidhbaba.quotemaker.pdf

import android.content.Context
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.events.Event
import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.sidhbaba.quotemaker.data.Quotation
import com.sidhbaba.quotemaker.data.QuotationItem
import com.sidhbaba.quotemaker.util.Rupee
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

object PdfGenerator {

    private val NAVY = DeviceRgb(31, 56, 100)      // #1F3864 - matches the template header band
    private val BLACK = DeviceRgb(0, 0, 0)
    private val WHITE = DeviceRgb(255, 255, 255)

    const val OWNER_COMPANY = "SIDH BABA ENTERPRISES"
    const val OWNER_ADDRESS = "DC-97/126, Gali No. 97, New Sabzi Mandi, Dabua Colony,\nN.I.T Faridabad - 121001 (Haryana)"
    const val OWNER_PHONE = "Phone: 9818772664"
    const val OWNER_EMAIL = "Email: sidhbaba111@gmail.com"
    const val OWNER_GSTIN = "GSTIN: 06CRNPK7443K1Z9"

    fun generate(
        context: Context,
        quotation: Quotation,
        items: List<QuotationItem>,
        outFile: File
    ): File {
        val writer = PdfWriter(outFile)
        val pdfDoc = PdfDocument(writer)
        pdfDoc.defaultPageSize = PageSize.A4

        // Keep the quotation number visible on every page, including an overflow 2nd page.
        pdfDoc.addEventHandler(PdfDocumentEvent.START_PAGE, object : IEventHandler {
            override fun handleEvent(event: Event) {
                val docEvent = event as PdfDocumentEvent
                val page: PdfPage = docEvent.page
                if (pdfDoc.getPageNumber(page) == 1) return // first page already shows it in the header block
                val canvas = PdfCanvas(page)
                val font = PdfFontFactory.createFont()
                canvas.beginText()
                    .setFontAndSize(font, 9f)
                    .moveText(36.0, (page.pageSize.top - 24).toDouble())
                    .showText("Quote No.: ${quotation.quoteNo}   (continued)")
                    .endText()
                canvas.release()
            }
        })

        val doc = Document(pdfDoc, PageSize.A4)
        doc.setMargins(28f, 36f, 28f, 36f)

        // Embed Android's Unicode-capable Roboto fonts so the Indian rupee sign (₹)
        // is rendered in the exported PDF instead of being dropped by Base-14 Helvetica.
        val fontRegular = createPdfFont("/system/fonts/Roboto-Regular.ttf")
        val fontBold = createPdfFont("/system/fonts/Roboto-Bold.ttf")

        // ---- Title: centered across the full quotation page ----
        doc.add(
            Paragraph("QUOTATION")
                .setFont(fontBold).setFontSize(20f).setFontColor(NAVY)
                .setTextAlignment(TextAlignment.CENTER)
                .setWidth(UnitValue.createPercentValue(100f))
                .setMarginTop(0f).setMarginBottom(4f)
        )

        // ---- Owner block + date/quote meta, two columns like the template ----
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f))).useAllAvailableWidth()
        headerTable.setBorder(Border.NO_BORDER)

        val ownerCell = Cell().setBorder(Border.NO_BORDER).add(
            compactLine(OWNER_COMPANY, fontBold, 14f, NAVY)
        ).add(
            compactLine(OWNER_ADDRESS, fontRegular, 10f)
        ).add(
            compactLine(OWNER_PHONE, fontRegular, 10f)
        ).add(
            compactLine(OWNER_EMAIL, fontRegular, 10f)
        ).add(
            compactLine(OWNER_GSTIN, fontRegular, 10f)
        )
        headerTable.addCell(ownerCell)

        val dateFmt = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        val metaCell = Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.LEFT)
            .add(metaLine(fontBold, fontRegular, "DATE:", dateFmt.format(quotation.dateMillis)))
            .add(metaLine(fontBold, fontRegular, "QUOTE NO.:", quotation.quoteNo))
            .add(metaLine(fontBold, fontRegular, "VALID UNTIL:", dateFmt.format(quotation.validUntilMillis)))
        headerTable.addCell(metaCell)

        doc.add(headerTable)
        doc.add(Paragraph(" ").setFontSize(4f))

        // ---- Customer block: larger heading; line spacing matches the owner details ----
        doc.add(
            compactLine("CUSTOMER", fontBold, 14f, NAVY).setMarginBottom(2f)
        )
        if (quotation.customerCompanySnapshot.isNotBlank()) {
            doc.add(compactLine(quotation.customerCompanySnapshot, fontBold, 11f))
        }
        if (quotation.customerNameSnapshot.isNotBlank()) {
            doc.add(compactLine(quotation.customerNameSnapshot, fontRegular, 10f))
        }
        if (quotation.customerAddressSnapshot.isNotBlank()) {
            doc.add(compactLine(quotation.customerAddressSnapshot, fontRegular, 10f))
        }
        if (quotation.customerGstinSnapshot.isNotBlank()) {
            doc.add(compactLine("GSTIN: ${quotation.customerGstinSnapshot}", fontRegular, 10f))
        }
        doc.add(
            Paragraph("Subject: QUOTATION FOR SPL GAUGE${if (quotation.subject.isNotBlank()) " - ${quotation.subject}" else ""}")
                .setFont(fontBold).setFontSize(11f).setMarginTop(6f).setMarginBottom(8f)
        )

        // ---- Items table ----
        val table = Table(UnitValue.createPercentArray(floatArrayOf(8f, 42f, 18f, 14f, 18f))).useAllAvailableWidth()
        table.setBorder(SolidBorder(BLACK, 0.75f))

        val headers = listOf("SR.NO", "DESCRIPTION", "ITEM CODE", "QUANTITY", "AMOUNT")
        for (h in headers) {
            table.addHeaderCell(
                Cell().add(Paragraph(h).setFont(fontBold).setFontSize(10.5f).setFontColor(WHITE))
                    .setBackgroundColor(NAVY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(5f)
                    .setBorder(SolidBorder(BLACK, 0.5f))
            )
        }

        items.forEachIndexed { index, item ->
            table.addCell(bodyCell((index + 1).toString(), fontRegular, TextAlignment.CENTER))
            table.addCell(bodyCell(item.description, fontRegular, TextAlignment.LEFT))
            table.addCell(bodyCell(item.itemCode, fontRegular, TextAlignment.CENTER))
            table.addCell(bodyCell(item.quantityText, fontRegular, TextAlignment.CENTER))
            table.addCell(bodyCell(Rupee.format(item.priceRupees), fontRegular, TextAlignment.RIGHT))
        }

        doc.add(table)

        // ---- Terms & Conditions ----
        doc.add(Paragraph("TERMS AND CONDITIONS").setFont(fontBold).setFontSize(11f).setFontColor(NAVY).setMarginTop(14f).setMarginBottom(3f))
        doc.add(Paragraph("1. Delivery: Within 30 days from the date of receipt of the confirmed purchase order.").setFont(fontRegular).setFontSize(10f).setMarginBottom(1f))
        doc.add(Paragraph("2. Taxes: Applicable taxes shall be charged as per the prevailing rates at the time of dispatch.").setFont(fontRegular).setFontSize(10f).setMarginBottom(1f))
        doc.add(Paragraph("3. Payment Terms: 50% advance payment along with the order confirmation and the remaining 50% upon delivery.").setFont(fontRegular).setFontSize(10f))

        // ---- Closing section ----
        // Keep the authorisation label, signature image, company name and footer together.
        // If they do not fit at the bottom of a page, iText moves the whole block to the next page.
        val closingBlock = Div().setKeepTogether(true).setMarginTop(14f)
        val sigTable = Table(UnitValue.createPercentArray(floatArrayOf(55f, 45f)))
            .useAllAvailableWidth()
            .setBorder(Border.NO_BORDER)
            .setKeepTogether(true)
        sigTable.addCell(Cell().setBorder(Border.NO_BORDER))

        val sigCell = Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.LEFT)
            .setKeepTogether(true)
        sigCell.add(compactLine("Authorised Signatory For", fontRegular, 10f).setMarginBottom(2f))
        try {
            context.assets.open("signature.png").use { sigStream ->
                val bytes = sigStream.readBytes()
                val img = Image(com.itextpdf.io.image.ImageDataFactory.create(bytes)).setWidth(110f)
                sigCell.add(img)
            }
        } catch (e: Exception) {
            sigCell.add(Paragraph(" ").setFontSize(26f))
        }
        sigCell.add(compactLine("Sidh Baba Enterprises", fontRegular, 10f))
        sigTable.addCell(sigCell)
        closingBlock.add(sigTable)

        closingBlock.add(
            Paragraph("Please get in touch with us if you have any questions regarding this price quote.")
                .setFont(fontRegular).setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(18f)
                .setBorderTop(SolidBorder(BLACK, 1f)).setPaddingTop(8f)
        )
        closingBlock.add(
            Paragraph("We Look Forward To working With You")
                .setFont(fontBold).setFontSize(11f).setFontColor(NAVY).setMarginTop(2f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        doc.add(closingBlock)

        doc.close()
        return outFile
    }

    private fun createPdfFont(systemFontPath: String): com.itextpdf.kernel.font.PdfFont {
        return try {
            PdfFontFactory.createFont(systemFontPath, PdfEncodings.IDENTITY_H)
        } catch (_: Exception) {
            // Fallback if a device uses a different system font path. Roboto is present on
            // standard Android builds; if not, continue creating the PDF instead of crashing.
            PdfFontFactory.createFont()
        }
    }

    private fun compactLine(
        text: String,
        font: com.itextpdf.kernel.font.PdfFont,
        size: Float,
        color: com.itextpdf.kernel.colors.Color? = null
    ): Paragraph {
        val p = Paragraph(text).setFont(font).setFontSize(size)
            .setMarginTop(0f).setMarginBottom(0f)
            .setFixedLeading(size * 1.25f)
        if (color != null) p.setFontColor(color)
        return p
    }

    private fun metaLine(bold: com.itextpdf.kernel.font.PdfFont, regular: com.itextpdf.kernel.font.PdfFont, label: String, value: String): Paragraph {
        val p = Paragraph().setMarginBottom(2f)
        p.add(Text("$label ").setFont(bold).setFontSize(10f))
        p.add(Text(value).setFont(regular).setFontSize(10f))
        return p
    }

    private fun bodyCell(text: String, font: com.itextpdf.kernel.font.PdfFont, align: TextAlignment): Cell {
        return Cell().add(Paragraph(text).setFont(font).setFontSize(10f))
            .setTextAlignment(align)
            .setPadding(5f)
            .setBorder(SolidBorder(BLACK, 0.5f))
    }
}
