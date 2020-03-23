package io.gr1d.billing.service;

import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.Cell;
import be.quodlibet.boxable.HorizontalAlignment;
import be.quodlibet.boxable.Row;
import be.quodlibet.boxable.line.LineStyle;
import io.gr1d.billing.api.subscriptions.Tenant;
import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.DocumentType;
import io.gr1d.billing.model.invoice.Invoice;
import io.gr1d.billing.model.invoice.InvoiceItem;
import io.gr1d.billing.response.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

@Slf4j
public class InvoicePDFTemplate {

    private final static float WIDTH = 595.27563f;
    private final static float HEIGHT = 841.8898f;
    private static PDType0Font font_regular = null;
    private static PDType0Font font_medium = null;
    private static PDType0Font font_light = null;

    public static byte[] createPdf(final Invoice invoice, final Card card, final UserResponse user, final Tenant tenant) {
        final byte[] pdf;
        final PDPage page = new PDPage(PDRectangle.A4);
        final ClassLoader classLoader = InvoicePDFTemplate.class.getClassLoader();

        try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
             final PDDocument document = new PDDocument()) {

            final PDPageContentStream contentStream = new PDPageContentStream(document, page);
            final AccessPermission accessPermission = new AccessPermission();
            font_regular = PDType0Font.load(document, classLoader.getResourceAsStream("fonts/Roboto/Roboto-Regular.ttf"));
            font_medium = PDType0Font.load(document, classLoader.getResourceAsStream("fonts/Roboto/Roboto-Medium.ttf"));
            font_light = PDType0Font.load(document, classLoader.getResourceAsStream("fonts/Roboto/Roboto-Light.ttf"));

            document.addPage(page);
            accessPermission.setCanPrint(true);
            accessPermission.setCanModify(false);

            final StandardProtectionPolicy standardProtectionPolicy = new StandardProtectionPolicy(UUID.randomUUID().toString(),
                    StringUtils.isEmpty(user.getDocument()) ? card.getDocument() : user.getDocument().replaceAll("\\D", ""), accessPermission);
            document.protect(standardProtectionPolicy);

            createHeaderTitle(contentStream, tenant, document);
            createClientTable(invoice, card, user, document, page);
            createPeriodTable(invoice, document, page);
            createCardInfoTable(invoice, card, document, page);
            createInvoiceTable(invoice, document, page);

            contentStream.close();
            document.save(out);
            pdf = out.toByteArray();
        } catch (IOException e) {
            log.error("Error while creating PDF file", e);
            return null;
        }

        return pdf;
    }

    private static void createHeaderTitle(final PDPageContentStream contentStream, final Tenant tenant, final PDDocument document) throws IOException {

        contentStream.setFont(font_light, 9);
        contentStream.beginText();
        contentStream.newLineAtOffset(WIDTH/3, HEIGHT - 26);
        contentStream.showText(i18n("io.gr1d.billing.invoice.pdf.title.info"));
        contentStream.endText();

        contentStream.setFont(font_medium, 18);
        contentStream.beginText();
        contentStream.newLineAtOffset(40, HEIGHT - 77);
        contentStream.showText(i18n("io.gr1d.billing.invoice.pdf.title"));
        contentStream.endText();

        final String logo = Optional.ofNullable(tenant)
                .map(Tenant::getLogo)
                .filter(StringUtils::isNotEmpty)
                .orElse(null);

        if (logo != null) {
            final URL url = new URL(logo);
            final BufferedImage bim = ImageIO.read(url);
            if (bim != null) {
                final PDImageXObject pdImage = LosslessFactory.createFromImage(document, bim);
                contentStream.drawImage(pdImage, WIDTH - 100, HEIGHT - 91.8f, 66, 37);
            }
        }
    }

    private static void createInvoiceTable(final Invoice invoice, final PDDocument document, final PDPage page) throws IOException {
        final NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
        format.setMaximumFractionDigits(6);

        final float margin = 40;
        // starting y position is whole page height subtracted by top and bottom margin
        final float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
        // we want table across whole page width (subtracted by left and right margin ofcourse)
        final float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
        final Color background = new Color(250, 250, 250);
        final BaseTable table = new BaseTable(HEIGHT - 290, yStartNewPage, 20, tableWidth, margin, document, page, true, true);
        final Row<PDPage> descriptionRow = table.createRow(15f);

        Cell<PDPage> cell = descriptionRow.createCell(100, i18n("io.gr1d.billing.invoice.pdf.invoiceDescription"));
        cell.setFontSize(12);
        cell.setBottomPadding(25);
        cell.setLeftPadding(0);
        cell.setFont(font_medium);
        cell.setBorderStyle(new LineStyle(Color.WHITE, 0));

        Row<PDPage> headerRow = table.createRow(15f);
        cell = headerRow.createCell(50, i18n("io.gr1d.billing.invoice.pdf.description"));
        cell.setFontSize(10);
        cell.setFont(font_regular);
        cell.setBottomPadding(10);
        cell.setLeftPadding(0);
        cell.setBottomBorderStyle(new LineStyle(Color.gray, 0.5f));
        cell.setLeftBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setRightBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setTopBorderStyle(new LineStyle(Color.WHITE, 0));

        cell = headerRow.createCell(16f, i18n("io.gr1d.billing.invoice.pdf.qtd"));
        cell.setFontSize(10);
        cell.setFont(font_regular);
        cell.setBottomPadding(10);
        cell.setBottomBorderStyle(new LineStyle(Color.gray, 0.5f));
        cell.setLeftBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setRightBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setTopBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setAlign(HorizontalAlignment.CENTER);

        cell = headerRow.createCell(18, i18n("io.gr1d.billing.invoice.pdf.unitValue"));
        cell.setFontSize(10);
        cell.setFont(font_regular);
        cell.setBottomPadding(10);
        cell.setLeftPadding(20);
        cell.setBottomBorderStyle(new LineStyle(Color.gray, 0.5f));
        cell.setLeftBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setRightBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setTopBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setAlign(HorizontalAlignment.LEFT);

        cell = headerRow.createCell(16f, i18n("io.gr1d.billing.invoice.pdf.price"));
        cell.setFontSize(10);
        cell.setFont(font_regular);
        cell.setBottomPadding(10);
        cell.setLeftPadding(20);
        cell.setBottomBorderStyle(new LineStyle(Color.gray, 0.5f));
        cell.setLeftBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setRightBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setTopBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setAlign(HorizontalAlignment.LEFT);

        int i = 1;

        for (final InvoiceItem item : invoice.getItems()) {

            final Row<PDPage> itemRow = table.createRow(12);
            cell = itemRow.createCell(50, item.getDescription());
            cell.setFontSize(10);
            cell.setTextColor(new Color(0, 0, 0, 0.6f));
            cell.setFont(font_regular);
            cell.setTopPadding(10);
            configureCell(background, cell, i);

            cell = itemRow.createCell(16, "" + NumberFormat.getNumberInstance(Locale.getDefault()).format(item.getQuantity()));
            cell.setFontSize(10);
            cell.setTextColor(new Color(0, 0, 0, 0.6f));
            cell.setFont(font_regular);
            cell.setTopPadding(10);
            cell.setAlign(HorizontalAlignment.CENTER);
            configureCell(background, cell, i);

            cell = itemRow.createCell(18, format.format(item.getUnitValue()));
            cell.setFontSize(10);
            cell.setTextColor(new Color(0, 0, 0, 0.6f));
            cell.setFont(font_regular);
            cell.setTopPadding(10);
            cell.setLeftPadding(20);
            cell.setAlign(HorizontalAlignment.LEFT);
            configureCell(background, cell, i);

            cell = itemRow.createCell(16, format.format(item.getValue()));
            cell.setFontSize(10);
            cell.setTextColor(new Color(0, 0, 0, 0.6f));
            cell.setFont(font_regular);
            cell.setTopPadding(10);
            cell.setLeftPadding(20);
            cell.setAlign(HorizontalAlignment.LEFT);
            configureCell(background, cell, i);

            i++;
        }

        final Row<PDPage> border = table.createRow(12);
        cell = border.createCell(100, "");
        cell.setTopBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setLeftBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setRightBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setBottomBorderStyle(new LineStyle(Color.gray, 0.5f));

        final Row<PDPage> totalRow = table.createRow(12);
        cell = totalRow.createCell(77, i18n("io.gr1d.billing.invoice.pdf.total"));
        cell.setFontSize(14);
        cell.setTopPadding(15);
        cell.setFont(font_medium);
        cell.setAlign(HorizontalAlignment.RIGHT);
        cell.setLeftBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setRightBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setBottomBorderStyle(new LineStyle(Color.WHITE, 0));

        cell = totalRow.createCell(22, NumberFormat.getCurrencyInstance(Locale.getDefault()).format(invoice.getValue()));
        cell.setFontSize(14);
        cell.setTopPadding(15);
        cell.setFont(font_medium);
        cell.setAlign(HorizontalAlignment.RIGHT);
        cell.setLeftBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setRightBorderStyle(new LineStyle(Color.WHITE, 0));
        cell.setBottomBorderStyle(new LineStyle(Color.WHITE, 0));

        table.draw();
    }

    private static void configureCell(final Color background, final Cell<PDPage> cell, final int rowIndex) {
        if (rowIndex % 2 == 0) {
            cell.setBorderStyle(new LineStyle(background, 0));
            cell.setFillColor(background);
        } else {
            cell.setLeftBorderStyle(new LineStyle(Color.WHITE, 0));
            cell.setRightBorderStyle(new LineStyle(Color.WHITE, 0));
            cell.setBottomBorderStyle(new LineStyle(Color.WHITE, 0));
        }
    }

    private static void createClientTable(final Invoice invoice, final Card card, final UserResponse user, final PDDocument document, final PDPage page) throws IOException {
        final BaseTable clientTable = new BaseTable(HEIGHT - 90, 100, 200, 265, 40, document, page, true, true);
        final Row<PDPage> headerRowClientTable = clientTable.createRow(15);
        final Color background = new Color(250, 250, 250);

        Cell<PDPage> cellClientTable = headerRowClientTable.createCell(100, i18n("io.gr1d.billing.invoice.pdf.clientData"));
        cellClientTable.setFontSize(12);
        cellClientTable.setFont(font_medium);
        cellClientTable.setBottomPadding(11);
        cellClientTable.setLeftPadding(0);
        cellClientTable.setBorderStyle(new LineStyle(background, 0));

        final Row<PDPage> nameRowClientTable = clientTable.createRow(15);
        cellClientTable = nameRowClientTable.createCell(100, StringUtils.capitalize(user.getFirstName()) + " " + StringUtils.capitalize(user.getLastName()));
        cellClientTable.setFontSize(10);
        cellClientTable.setFont(font_medium);
        cellClientTable.setBottomPadding(0);
        cellClientTable.setLeftPadding(11);
        cellClientTable.setTopPadding(11);
        cellClientTable.setFillColor(background);
        cellClientTable.setBorderStyle(new LineStyle(background, 0));

        final Row<PDPage> cpfCnpjRowClientTable = clientTable.createRow(15);
        cellClientTable = cpfCnpjRowClientTable.createCell(25.2f, i18n("io.gr1d.billing.invoice.pdf.document"));
        cellClientTable.setFont(font_medium);
        cellClientTable.setFontSize(10);
        cellClientTable.setBottomPadding(0);
        cellClientTable.setLeftPadding(11);
        cellClientTable.setFillColor(background);
        cellClientTable.setBorderStyle(new LineStyle(background, 0));

        String cpfCnpj = StringUtils.isEmpty(user.getDocument()) ? card.getDocument() : user.getDocument().replaceAll("\\D", "");
        String documentType = StringUtils.isEmpty(user.getDocument()) ? card.getDocumentType().toString() : user.getDocumentType();

        try {
            if (documentType.equals(DocumentType.CPF.toString())) {
                final MaskFormatter cpfFormatter = new MaskFormatter("###.###.###-##");
                cpfFormatter.setValueContainsLiteralCharacters(false);
                cpfCnpj = cpfFormatter.valueToString(cpfCnpj);
                new DefaultFormatterFactory(cpfFormatter);
            } else if (card.getDocumentType() == DocumentType.CNPJ) {
                final MaskFormatter cnpjFormatter = new MaskFormatter("##.###.###/####-##");
                cnpjFormatter.setValueContainsLiteralCharacters(false);
                cpfCnpj = cnpjFormatter.valueToString(cpfCnpj);
            }

        } catch (Exception e) {
            log.error("Error while trying to serialize document on PDF", e);
            //do nothing
        }


        cellClientTable = cpfCnpjRowClientTable.createCell(55, cpfCnpj);
        cellClientTable.setAlign(HorizontalAlignment.LEFT);
        cellClientTable.setFontSize(10);
        cellClientTable.setFont(font_regular);
        cellClientTable.setBottomPadding(0);
        cellClientTable.setTopPadding(5);
        cellClientTable.setLeftPadding(0);
        cellClientTable.setFillColor(background);
        cellClientTable.setBorderStyle(new LineStyle(background, 0));

        final Row<PDPage> invoiceIdRowClientTable = clientTable.createRow(92);
        cellClientTable = invoiceIdRowClientTable.createCell(38.2f, i18n("io.gr1d.billing.invoice.pdf.invoiceNumber"));
        cellClientTable.setFontSize(10);
        cellClientTable.setFont(font_medium);
        cellClientTable.setLeftPadding(11);
        cellClientTable.setFillColor(background);
        cellClientTable.setBorderStyle(new LineStyle(background, 0));

        cellClientTable = invoiceIdRowClientTable.createCell(52, invoice.getNumber());
        cellClientTable.setFontSize(10);
        cellClientTable.setFont(font_regular);
        cellClientTable.setAlign(HorizontalAlignment.LEFT);
        cellClientTable.setTopPadding(5);
        cellClientTable.setLeftPadding(0);
        cellClientTable.setFillColor(background);
        cellClientTable.setBorderStyle(new LineStyle(background, 0));

        clientTable.draw();
    }

    private static void createPeriodTable(final Invoice invoice, final PDDocument document, final PDPage page) throws IOException {
        final BaseTable periodTable = new BaseTable(HEIGHT - 120, 100, 200, 249, 312, document, page, true, true);
        final Color background = new Color(250, 250, 250);
        final Row<PDPage> headerRowClientTable = periodTable.createRow(15);

        Cell<PDPage> cellPeriodTable = headerRowClientTable.createCell(33, i18n("io.gr1d.billing.invoice.pdf.expiryDate"));
        cellPeriodTable.setFontSize(10);
        cellPeriodTable.setTopPadding(10);
        cellPeriodTable.setLeftPadding(11);
        cellPeriodTable.setFont(font_regular);
        cellPeriodTable.setAlign(HorizontalAlignment.LEFT);
        cellPeriodTable.setBottomPadding(4);
        cellPeriodTable.setFillColor(background);
        cellPeriodTable.setBorderStyle(new LineStyle(background, 0));

        cellPeriodTable = headerRowClientTable.createCell(33, i18n("io.gr1d.billing.invoice.pdf.period"));
        cellPeriodTable.setFontSize(10);
        cellPeriodTable.setTopPadding(10);
        cellPeriodTable.setLeftPadding(15);
        cellPeriodTable.setFont(font_regular);
        cellPeriodTable.setAlign(HorizontalAlignment.LEFT);
        cellPeriodTable.setBottomPadding(4);
        cellPeriodTable.setFillColor(background);
        cellPeriodTable.setBorderStyle(new LineStyle(background, 0));

        cellPeriodTable = headerRowClientTable.createCell(33, i18n("io.gr1d.billing.invoice.pdf.total") + ":");
        cellPeriodTable.setFontSize(10);
        cellPeriodTable.setTopPadding(10);
        cellPeriodTable.setLeftPadding(11);
        cellPeriodTable.setFont(font_regular);
        cellPeriodTable.setAlign(HorizontalAlignment.LEFT);
        cellPeriodTable.setBottomPadding(4);
        cellPeriodTable.setFillColor(background);
        cellPeriodTable.setBorderStyle(new LineStyle(background, 0));

        final Row<PDPage> rowPeriodTable = periodTable.createRow(15);

        cellPeriodTable = rowPeriodTable.createCell(33, invoice.getExpirationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        cellPeriodTable.setFontSize(12);
        cellPeriodTable.setFont(font_medium);
        cellPeriodTable.setBottomPadding(10);
        cellPeriodTable.setLeftPadding(11);
        cellPeriodTable.setAlign(HorizontalAlignment.LEFT);
        cellPeriodTable.setFillColor(background);
        cellPeriodTable.setBorderStyle(new LineStyle(background, 0));

        cellPeriodTable = rowPeriodTable.createCell(33, invoice.getPeriodStart().format(DateTimeFormatter.ofPattern("MMM/yyyy")).toUpperCase());
        cellPeriodTable.setFontSize(12);
        cellPeriodTable.setFont(font_medium);
        cellPeriodTable.setBottomPadding(10);
        cellPeriodTable.setLeftPadding(15);
        cellPeriodTable.setAlign(HorizontalAlignment.LEFT);
        cellPeriodTable.setFillColor(background);
        cellPeriodTable.setBorderStyle(new LineStyle(background, 0));

        final NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
        format.setMaximumFractionDigits(2);

        cellPeriodTable = rowPeriodTable.createCell(33, format.format(invoice.getValue()));
        cellPeriodTable.setFontSize(12);
        cellPeriodTable.setFont(font_medium);
        cellPeriodTable.setBottomPadding(10);
        cellPeriodTable.setLeftPadding(11);
        cellPeriodTable.setAlign(HorizontalAlignment.LEFT);
        cellPeriodTable.setFillColor(background);
        cellPeriodTable.setBorderStyle(new LineStyle(background, 0));

        periodTable.draw();
    }

    private static void createCardInfoTable(final Invoice invoice, final Card card, final PDDocument document, final PDPage page) throws IOException {
        final BaseTable periodTable = new BaseTable(HEIGHT - 182, 100, 200, 249, 312, document, page, true, true);
        final Color background = new Color(250, 250, 250);
        final Row<PDPage> headerRowCardInfoTable = periodTable.createRow(15);

        Cell<PDPage> cellPeriodTable = headerRowCardInfoTable.createCell(72.8f, i18n("io.gr1d.billing.invoice.pdf.cardInfo.title"));
        cellPeriodTable.setFontSize(9);
        cellPeriodTable.setFont(font_regular);
        cellPeriodTable.setFillColor(background);
        cellPeriodTable.setBorderStyle(new LineStyle(background, 0));
        cellPeriodTable.setTopPadding(10);
        cellPeriodTable.setLeftPadding(11);

        cellPeriodTable = headerRowCardInfoTable.createCell(28, invoice.getExpirationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        cellPeriodTable.setFontSize(9);
        cellPeriodTable.setFont(font_regular);
        cellPeriodTable.setFillColor(background);
        cellPeriodTable.setBorderStyle(new LineStyle(background, 0));
        cellPeriodTable.setTopPadding(10);

        final Row<PDPage> rowNameCardInfoTable = periodTable.createRow(15);
        cellPeriodTable = rowNameCardInfoTable.createCell(100, card.getCardHolderName());
        cellPeriodTable.setFontSize(12);
        cellPeriodTable.setFont(font_medium);
        cellPeriodTable.setFillColor(background);
        cellPeriodTable.setBorderStyle(new LineStyle(background, 0));
        cellPeriodTable.setTopPadding(2);
        cellPeriodTable.setLeftPadding(11);

        final Row<PDPage> rowNumberCardInfoTable = periodTable.createRow(15);
        cellPeriodTable = rowNumberCardInfoTable.createCell(100, StringUtils.capitalize(card.getBrand().toString()) + " - " + card.getLastDigits());
        cellPeriodTable.setFontSize(10);
        cellPeriodTable.setFont(font_regular);
        cellPeriodTable.setFillColor(background);
        cellPeriodTable.setBorderStyle(new LineStyle(background, 0));
        cellPeriodTable.setBottomPadding(10);
        cellPeriodTable.setTopPadding(0);
        cellPeriodTable.setLeftPadding(11);

        periodTable.draw();
    }

    private static String i18n(final String key) {
        try {
            final ResourceBundle messages = ResourceBundle.getBundle("messages", Locale.getDefault());
            return messages.getString(key);
        } catch (final Exception e) {
            return key;
        }
    }
}
