package com.neonshift.app;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Small, offline PDF writer. Standard Helvetica fonts and Invoice Deck geometry. */
public final class InvoicePdf {
    private static final float MM = 72f / 25.4f, M = 18 * MM;
    private final float W,H;private final boolean timesheet;private boolean summaryInvoice;
    private static final Charset CP1252 = Charset.forName("windows-1252");
    private final List<StringBuilder> pages = new ArrayList<>();
    private StringBuilder page;
    private final String number;
    private final Map<String, String> profile;
    private float y;

    private InvoicePdf(int number, Map<String, String> profile) {
        this(number,profile,false);
    }
    private InvoicePdf(int number,Map<String,String> profile,boolean timesheet){
        this.timesheet=timesheet;W=(timesheet?297:210)*MM;H=(timesheet?210:297)*MM;
        this.number = InvoiceEngine.number(number);
        this.profile = profile;
    }
    private static String f(float n) { return String.format(Locale.ROOT, "%.2f", n); }
    private static String encoded(String s) {
        s = s.replace('\u2013', '-').replace('\u2014', '-');
        if (!CP1252.newEncoder().canEncode(s)) throw new IllegalArgumentException("O PDF aceita caracteres latinos. Remova emojis ou símbolos do texto.");
        StringBuilder out = new StringBuilder();
        for (byte value : s.getBytes(CP1252)) {
            int c = value & 255;
            if (c == '(' || c == ')' || c == '\\') out.append('\\').append((char)c);
            else if (c < 32 || c > 126) out.append(String.format(Locale.ROOT, "\\%03o", c));
            else out.append((char)c);
        }
        return out.toString();
    }
    private static float width(String s, float size, boolean bold) {
        int[] metrics = bold ? InvoiceFont.BOLD : InvoiceFont.REGULAR;
        float units = 0;
        for (byte b : s.getBytes(CP1252)) units += metrics[b & 255];
        return units * size / 1000f;
    }
    private static List<String> wrap(String text, float maxWidth, float size, boolean bold) {
        List<String> result = new ArrayList<>();
        for (String para : text.replace("\r", "").split("\n", -1)) {
            if (para.isEmpty()) { result.add(""); continue; }
            String line = "";
            for (String word : para.trim().split("\\s+")) {
                if (!line.isEmpty() && width(line + " " + word, size, bold) <= maxWidth) { line += " " + word; continue; }
                if (!line.isEmpty()) { result.add(line); line = ""; }
                while (width(word, size, bold) > maxWidth) {
                    int cut = 1;
                    while (cut < word.length() && width(word.substring(0, cut + 1), size, bold) <= maxWidth) cut++;
                    result.add(word.substring(0, cut)); word = word.substring(cut);
                }
                line = word;
            }
            if (!line.isEmpty()) result.add(line);
        }
        return result;
    }
    private void text(String text, float x, float baseline, float size, boolean bold, String color) {
        page.append(color).append(" rg BT /").append(bold ? "F2" : "F1").append(' ').append(f(size))
            .append(" Tf 1 0 0 1 ").append(f(x)).append(' ').append(f(H - baseline)).append(" Tm (")
            .append(encoded(text)).append(") Tj ET\n");
    }
    private void right(String value, float x, float baseline, float size, boolean bold, String color) {
        text(value, x - width(value, size, bold), baseline, size, bold, color);
    }
    private void rect(float x, float top, float w, float h, String color) {
        page.append(color).append(" rg ").append(f(x)).append(' ').append(f(H - top - h)).append(' ')
            .append(f(w)).append(' ').append(f(h)).append(" re f\n");
    }
    private void line(float x1, float x2, float top) {
        page.append("0.84 0.87 0.89 RG 0.4 w ").append(f(x1)).append(' ').append(f(H - top)).append(" m ")
            .append(f(x2)).append(' ').append(f(H - top)).append(" l S\n");
    }
    private void newPage() {
        page = new StringBuilder(); pages.add(page); y = M;
    }
    private float paragraph(String value, float x, float top, float maxWidth) {
        List<String> lines = wrap(value, maxWidth, 9, false);
        for (int i = 0; i < lines.size(); i++) text(lines.get(i), x, top + 9 + i * 12, 9, false, "0.20 0.25 0.33");
        return lines.size() * 12;
    }
    private String get(String key) { return profile.getOrDefault(key, ""); }
    private void header(LocalDate issued) {
        String title = "INVOICE FOR SERVICES";
        text(title, (W - width(title, 18, true)) / 2, y + 18, 18, true, "0 0 0"); y += 50;
        text("INVOICE #" + number, M + 6, y, 14, true, "0 0 0");
        right("Issue date: " + issued.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)), W - M - 6, y - 8, 9, false, "0.20 0.25 0.33");
        right("Currency: AUD", W - M - 6, y + 4, 9, false, "0.20 0.25 0.33"); y += 32;
        text("FROM", M, y, 11, true, "0 0 0"); text("BILL TO", W / 2, y, 11, true, "0 0 0"); y += 13;
        float a = paragraph(get("name") + "\n" + get("address") + "\nABN: " + get("abn") + "\n" + get("email"), M, y, (W - 2 * M) / 2 - 12);
        float b = paragraph(get("client") + "\n" + get("client_address") + "\nABN: " + get("client_abn") + "\n" + get("client_email"), W / 2, y, (W - 2 * M) / 2 - 12);
        float headerBottom = y + Math.max(a, b) + 45 + 13 + wrap(get("description"), W - 2 * M - 12, 9, false).size() * 12 + 16 + 24;
        if (headerBottom > H - M - 10) throw new IllegalArgumentException("Reduza os textos do prestador, cliente ou descrição para caber no cabeçalho do PDF.");
        y += Math.max(a, b) + 45;
        text("SHIFT DETAILS", M + 6, y, 14, true, "0 0 0"); y += 13;
        y += paragraph(get("description"), M + 6, y, W - 2 * M - 12) + 16;
    }
    private void tableHead() {
        rect(M, y, W - 2 * M, 24, "0.91 0.94 0.94");
        text(summaryInvoice?"Service period":"Date / day", M + 6, y + 15, 9, false, "0.20 0.25 0.33");
        text(summaryInvoice?"Description":"Work location", M + 32 * MM + 6, y + 15, 9, false, "0.20 0.25 0.33");
        right("Amount AUD", W - M - 6, y + 15, 9, false, "0.20 0.25 0.33");
        y += 24; line(M, W - M, y);
    }
    private void row(InvoiceEngine.Item i, int index) {
        List<String> address = wrap(i.dayOff ? "Day off" : i.address, 110 * MM - 12, 9, false);
        float height = Math.max(2, address.size()) * 12 + 12;
        if (y + height > H - M - 10) { newPage(); tableHead(); }
        if ((index & 1) == 1) rect(M, y, W - 2 * M, height, "0.973 0.980 0.984");
        text(i.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), M + 6, y + 15, 9, false, "0.20 0.25 0.33");
        text(i.date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH), M + 6, y + 27, 9, false, "0.20 0.25 0.33");
        for (int n = 0; n < address.size(); n++) text(address.get(n), M + 32 * MM + 6, y + 15 + 12 * n, 9, false, "0.20 0.25 0.33");
        right(String.format(Locale.US, "%,.2f", i.amount), W - M - 6, y + 15, 9, false, "0.20 0.25 0.33");
        y += height; line(M, W - M, y);
    }
    private void payment(List<InvoiceEngine.Item> items) {
        String bank = "PAYMENT DETAILS\n" + get("bank") + "\nAccount name: " + get("account_name") +
            "\nBSB: " + get("bsb") + "   Account: " + get("account") + "\nReference: Invoice " + number;
        int bankLines = wrap(bank, W - 2 * M - 12, 9, false).size();
        float need = 54 + bankLines * 12;
        if (y + need > H - M) newPage();
        y += 29;
        text("TOTAL AUD  $" + String.format(Locale.US, "%,.2f", InvoiceEngine.total(items)), M + 94 * MM + 6, y, 14, true, "0 0 0");
        y += 27; paragraph(bank, M + 6, y, W - 2 * M - 12);
    }
    private byte[] finish() throws Exception {
        for (int n = 0; n < pages.size(); n++) {
            page = pages.get(n);
            String footer = (timesheet?"Timesheet for invoice ":"Invoice ") + number + " | " + get("name");
            if (width(footer, 8, false) > W - 2 * M - 65) {
                while (width(footer + "...", 8, false) > W - 2 * M - 65) footer = footer.substring(0, footer.length() - 1);
                footer += "...";
            }
            text(footer, M, H - 11 * MM, 8, false, "0.39 0.45 0.55");
            right("Page " + (n + 1), W - M, H - 11 * MM, 8, false, "0.39 0.45 0.55");
        }
        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int n = 0; n < pages.size(); n++) kids.append(6 + n * 2).append(" 0 R ");
        objects.add("<< /Type /Pages /Count " + pages.size() + " /Kids [" + kids + "] >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        for (int n = 0; n < pages.size(); n++) {
            String content = pages.get(n).toString();
            objects.add("<< /Length " + content.getBytes(StandardCharsets.ISO_8859_1).length + " >>\nstream\n" + content + "endstream");
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + f(W) + " " + f(H) + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + (5 + n * 2) + " 0 R >>");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".getBytes(StandardCharsets.ISO_8859_1));
        List<Integer> offsets = new ArrayList<>(); offsets.add(0);
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            out.write(((i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
        }
        int xref = out.size();
        out.write(("xref\n0 " + offsets.size() + "\n0000000000 65535 f \n").getBytes(StandardCharsets.US_ASCII));
        for (int i = 1; i < offsets.size(); i++) out.write(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i)).getBytes(StandardCharsets.US_ASCII));
        out.write(("trailer\n<< /Size " + offsets.size() + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII));
        return out.toByteArray();
    }
    private void summaryRow(List<InvoiceEngine.Item> items){
        LocalDate from=items.get(0).date,to=from;for(InvoiceEngine.Item item:items){if(item.date.isBefore(from))from=item.date;if(item.date.isAfter(to))to=item.date;}
        List<String> lines=wrap(MarkenBilling.SERVICE+" - "+get("client"),110*MM-12,9,false);float height=Math.max(3,lines.size())*12+12;
        if(y+height>H-M-10){newPage();tableHead();}text(from.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),M+6,y+15,9,false,"0.20 0.25 0.33");if(!from.equals(to))text("to "+to.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),M+6,y+27,8,false,"0.20 0.25 0.33");
        for(int n=0;n<lines.size();n++)text(lines.get(n),M+32*MM+6,y+15+12*n,9,false,"0.20 0.25 0.33");right(String.format(Locale.US,"%,.2f",InvoiceEngine.total(items)),W-M-6,y+15,9,false,"0.20 0.25 0.33");y+=height;line(M,W-M,y);
    }
    private void sheetHead(){
        text("TIMESHEET",M,y,20,true,"0.08 0.20 0.30");right("Invoice #"+number,W-M,y,11,true,"0.08 0.20 0.30");y+=22;
    }
    private void sheetTableHead(){String[] names={"Date","Start","Finish","Unpaid break","Ordinary","OT1.5","OT2","Base AUD/h"};float[] x={0,92,170,248,340,440,540,638};rect(M,y,W-2*M,24,"0.10 0.26 0.38");for(int n=0;n<names.length;n++)text(names[n],M+x[n]+5,y+15,9,true,"1 1 1");y+=24;}
    public static byte[] generateTimesheet(int number,LocalDate submitted,Map<String,String> profile,List<MarkenBilling.Entry> entries)throws Exception{
        if(entries.isEmpty())throw new IllegalArgumentException("Vincule jornadas Marken para gerar o timesheet.");InvoicePdf p=new InvoicePdf(number,profile,true);p.newPage();p.sheetHead();
        String position=MarkenBilling.POSITION;String company=profile.getOrDefault("timesheet_company","");if(company.isEmpty())company=entries.get(0).company;String site=profile.getOrDefault("timesheet_location","");if(site.isEmpty())site=entries.get(0).site;
        p.y+=p.paragraph("Worker: "+profile.getOrDefault("name","")+" | Position: "+position+"\nCompany: "+company+" | Location: "+site+"\nDate range: "+MarkenBilling.period(entries)+" | Submitted: "+submitted,M,p.y,Wsheet(p)-2*M)+12;p.sheetTableHead();
        List<MarkenBilling.Entry> ordered=new ArrayList<>(entries);ordered.sort(Comparator.comparing((MarkenBilling.Entry e)->e.date).thenComparing(e->e.start));long[] totals=new long[4];float[] x={0,92,170,248,340,440,540,638};
        for(MarkenBilling.Entry e:ordered){List<String> details=wrap("Site: "+e.site+" | Position: "+MarkenBilling.POSITION,p.W-2*M-10,8,false);float height=27+details.size()*11;if(p.y+height>p.H-M-12){p.newPage();p.sheetHead();p.sheetTableHead();}
            String[] values={e.date,e.start,e.end,MarkenBilling.duration(e.breakMs),MarkenBilling.duration(e.regularMs),MarkenBilling.duration(e.ot15Ms),MarkenBilling.duration(e.ot2Ms),e.rate.setScale(2,java.math.RoundingMode.HALF_UP).toPlainString()};for(int n=0;n<values.length;n++)p.text(values[n],M+x[n]+5,p.y+14,8,false,"0.12 0.18 0.24");for(int n=0;n<details.size();n++)p.text(details.get(n),M+5,p.y+27+n*11,8,false,"0.35 0.40 0.45");p.y+=height;p.line(M,p.W-M,p.y);totals[0]+=e.breakMs;totals[1]+=e.regularMs;totals[2]+=e.ot15Ms;totals[3]+=e.ot2Ms;
        }
        if(p.y+150>p.H-M){p.newPage();p.sheetHead();}p.y+=17;p.text("TOTAL",M+5,p.y,10,true,"0.10 0.26 0.38");for(int n=0;n<4;n++)p.text(MarkenBilling.duration(totals[n]),M+x[n+3]+5,p.y,9,true,"0.10 0.26 0.38");p.y+=20;
        for(MarkenBilling.Group g:MarkenBilling.groups(entries)){String rates="Rates AUD: ordinary "+g.rate.setScale(2,java.math.RoundingMode.HALF_UP)+" | OT1.5 "+g.rate.multiply(new java.math.BigDecimal("1.5")).setScale(2,java.math.RoundingMode.HALF_UP)+" | OT2 "+g.rate.multiply(new java.math.BigDecimal("2")).setScale(2,java.math.RoundingMode.HALF_UP);if(p.y+85>p.H-M){p.newPage();p.sheetHead();}p.y+=p.paragraph(rates+"\nAmounts AUD: ordinary "+g.amount(0)+" | OT1.5 "+g.amount(1)+" | OT2 "+g.amount(2),M+5,p.y,p.W-2*M-10)+12;}
        if(p.y+83>p.H-M){p.newPage();p.sheetHead();}p.text("TOTAL INVOICE AUD $"+MarkenBilling.total(entries),M+5,p.y,14,true,"0.05 0.30 0.18");p.y+=24;p.text("Site manager name: ____________________  Signature: ____________________  Date: __________",M+5,p.y,9,false,"0.12 0.18 0.24");p.y+=17;p.text("Keep a signed copy. Hours and payment remain subject to site manager approval.",M+5,p.y,8,false,"0.35 0.40 0.45");return p.finish();
    }
    private static float Wsheet(InvoicePdf p){return p.W;}
    public static byte[] generate(int number, LocalDate issued, Map<String, String> profile, List<InvoiceEngine.Item> items) throws Exception {
        return generate(number,issued,profile,items,false);
    }
    public static byte[] generate(int number,LocalDate issued,Map<String,String> profile,List<InvoiceEngine.Item> items,boolean summary)throws Exception{
        InvoiceEngine.total(items);
        if(summary){profile=new HashMap<>(profile);profile.put("description",MarkenBilling.SERVICE);}
        InvoicePdf pdf = new InvoicePdf(number, profile);
        pdf.summaryInvoice=summary;pdf.newPage(); pdf.header(issued); pdf.tableHead();
        if(summary)pdf.summaryRow(items);else for (int n = 0; n < items.size(); n++) pdf.row(items.get(n), n);
        pdf.payment(items);
        return pdf.finish();
    }
}
