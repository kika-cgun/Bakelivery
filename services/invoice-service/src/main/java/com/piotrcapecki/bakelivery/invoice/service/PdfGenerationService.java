package com.piotrcapecki.bakelivery.invoice.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.piotrcapecki.bakelivery.invoice.dto.OrderItemResponse;
import com.piotrcapecki.bakelivery.invoice.dto.OrderPlacedEvent;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfGenerationService {

    public byte[] generate(OrderPlacedEvent event) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);
        document.open();
        try {
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("FAKTURA", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Font normalFont = new Font(Font.HELVETICA, 10);
            document.add(new Paragraph("Numer zamówienia: " + event.orderId(), normalFont));
            document.add(new Paragraph("Data: " + event.placedAt(), normalFont));
            document.add(new Paragraph("Adres dostawy: " + event.deliveryAddress(), normalFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 1f, 2f, 2f});

            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            table.addCell(headerCell("Produkt", headerFont));
            table.addCell(headerCell("Ilość", headerFont));
            table.addCell(headerCell("Cena jedn.", headerFont));
            table.addCell(headerCell("Razem", headerFont));

            for (OrderItemResponse item : event.items()) {
                String productLabel = item.productName()
                        + (item.variantName() != null ? " (" + item.variantName() + ")" : "");
                table.addCell(new PdfPCell(new Phrase(productLabel, normalFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(item.quantity()), normalFont)));
                table.addCell(new PdfPCell(new Phrase(item.unitPrice().toPlainString() + " PLN", normalFont)));
                table.addCell(new PdfPCell(new Phrase(item.lineTotal().toPlainString() + " PLN", normalFont)));
            }

            PdfPCell totalLabelCell = new PdfPCell(new Phrase("RAZEM:", headerFont));
            totalLabelCell.setColspan(3);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalLabelCell);
            table.addCell(new PdfPCell(new Phrase(event.totalAmount().toPlainString() + " PLN", headerFont)));

            document.add(table);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new java.awt.Color(220, 220, 220));
        return cell;
    }
}
