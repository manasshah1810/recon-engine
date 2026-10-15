package dev.manas.recon.web;

import dev.manas.recon.domain.ReconBreak;
import dev.manas.recon.domain.ReconRun;
import dev.manas.recon.match.CsvTxnParser;
import dev.manas.recon.match.MatchConfig;
import dev.manas.recon.service.ReconService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ReconController {
    private final ReconService service;

    public ReconController(ReconService service) {
        this.service = service;
    }

    public record UpdateBreakRequest(@NotNull ReconBreak.Status status, @Size(max = 500) String note) {}

    /** Upload two CSVs (reference,amount,currency,date) and reconcile them. */
    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public ReconRun create(@RequestParam("fileA") MultipartFile fileA,
                           @RequestParam("fileB") MultipartFile fileB,
                           @RequestParam(defaultValue = "0") BigDecimal amountTolerance,
                           @RequestParam(defaultValue = "0") int dateToleranceDays) throws IOException {
        MatchConfig cfg = new MatchConfig(amountTolerance, dateToleranceDays);
        var a = CsvTxnParser.parse(fileA.getInputStream());
        var b = CsvTxnParser.parse(fileB.getInputStream());
        return service.reconcile(a, b, cfg);
    }

    @GetMapping("/runs/{id}")
    public ReconRun get(@PathVariable Long id) {
        return service.getRun(id);
    }

    @GetMapping("/runs/{id}/breaks")
    public List<ReconBreak> breaks(@PathVariable Long id,
                                   @RequestParam(required = false) ReconBreak.Status status,
                                   @RequestParam(required = false) ReconBreak.Type type) {
        return service.listBreaks(id, status, type);
    }

    @PatchMapping("/breaks/{id}")
    public ReconBreak update(@PathVariable Long id, @Valid @RequestBody UpdateBreakRequest req) {
        return service.updateBreak(id, req.status(), req.note());
    }
}
