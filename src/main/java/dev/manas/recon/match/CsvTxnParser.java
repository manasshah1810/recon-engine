package dev.manas.recon.match;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Parses CSV with header: reference,amount,currency,date (ISO yyyy-MM-dd). Quoted fields are supported. */
public final class CsvTxnParser {

    private CsvTxnParser() {}

    public static List<Txn> parse(InputStream in) throws IOException {
        List<Txn> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String header = r.readLine();
            if (header == null) throw new CsvFormatException("file is empty");
            List<String> cols = split(header.replace("﻿", ""), 1);
            if (!lower(cols).equals(List.of("reference", "amount", "currency", "date")))
                throw new CsvFormatException("header must be: reference,amount,currency,date");
            String line;
            int n = 1;
            while ((line = r.readLine()) != null) {
                n++;
                if (line.isBlank()) continue;
                List<String> f = split(line, n);
                if (f.size() != 4) throw new CsvFormatException("line " + n + ": expected 4 columns, got " + f.size());
                try {
                    out.add(new Txn(f.get(0), new BigDecimal(f.get(1).trim()), f.get(2), LocalDate.parse(f.get(3).trim())));
                } catch (DateTimeParseException | IllegalArgumentException e) {
                    throw new CsvFormatException("line " + n + ": " + e.getMessage());
                }
            }
        }
        return out;
    }

    private static List<String> lower(List<String> l) {
        return l.stream().map(s -> s.trim().toLowerCase()).toList();
    }

    static List<String> split(String line, int lineNo) {
        List<String> res = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                res.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (quoted) throw new CsvFormatException("line " + lineNo + ": unterminated quote");
        res.add(cur.toString());
        return res;
    }

    public static class CsvFormatException extends RuntimeException {
        public CsvFormatException(String m) {
            super(m);
        }
    }
}
