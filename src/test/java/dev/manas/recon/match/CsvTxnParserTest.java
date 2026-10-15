package dev.manas.recon.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.manas.recon.match.CsvTxnParser.CsvFormatException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvTxnParserTest {
    private static List<Txn> parse(String s) throws IOException {
        return CsvTxnParser.parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parsesValidFileAndSkipsBlankLines() throws IOException {
        var rows = parse("reference,amount,currency,date\nT1,10.50,usd,2026-01-01\n\nT2,-3,EUR,2026-01-02\n");
        assertEquals(2, rows.size());
        assertEquals("USD", rows.get(0).currency());
        assertEquals("-3", rows.get(1).amount().toPlainString());
    }

    @Test
    void handlesQuotedFieldsWithCommasAndBom() throws IOException {
        var rows = parse("﻿Reference,Amount,Currency,Date\n\"T,1\",1,USD,2026-01-01\n");
        assertEquals("T,1", rows.get(0).reference());
    }

    @Test
    void headerOnlyGivesEmptyList() throws IOException {
        assertTrue(parse("reference,amount,currency,date\n").isEmpty());
    }

    @Test
    void rejectsBadHeader() {
        var e = assertThrows(CsvFormatException.class, () -> parse("ref,amt\n"));
        assertTrue(e.getMessage().contains("header"));
    }

    @Test
    void rejectsEmptyFile() {
        assertThrows(CsvFormatException.class, () -> parse(""));
    }

    @Test
    void reportsLineNumberOnBadAmountDateAndColumns() {
        var h = "reference,amount,currency,date\n";
        assertTrue(assertThrows(CsvFormatException.class, () -> parse(h + "T1,abc,USD,2026-01-01\n")).getMessage().startsWith("line 2"));
        assertTrue(assertThrows(CsvFormatException.class, () -> parse(h + "T1,1,USD,01/01/2026\n")).getMessage().startsWith("line 2"));
        assertTrue(assertThrows(CsvFormatException.class, () -> parse(h + "T1,1,USD\n")).getMessage().contains("4 columns"));
        assertTrue(assertThrows(CsvFormatException.class, () -> parse(h + "T1,1,US,2026-01-01\n")).getMessage().contains("currency"));
    }

    @Test
    void rejectsUnterminatedQuote() {
        assertThrows(CsvFormatException.class, () -> parse("reference,amount,currency,date\n\"T1,1,USD,2026-01-01\n"));
    }
}
