package az.yusif.fx.web;

import az.yusif.fx.domain.ApiClient;
import az.yusif.fx.domain.SyncLog;
import az.yusif.fx.dto.BackfillResult;
import az.yusif.fx.dto.CreateClientRequest;
import az.yusif.fx.dto.CreatedClientResponse;
import az.yusif.fx.dto.SyncResult;
import az.yusif.fx.service.ApiClientService;
import az.yusif.fx.service.RateSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin")
public class AdminController {

    private final RateSyncService syncService;
    private final ApiClientService apiClientService;

    public AdminController(RateSyncService syncService, ApiClientService apiClientService) {
        this.syncService = syncService;
        this.apiClientService = apiClientService;
    }

    @PostMapping("/sync")
    @Operation(summary = "Pull rates from CBAR for a single date (today by default)")
    public SyncResult sync(@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        return syncService.sync(date);
    }

    @PostMapping("/sync/backfill")
    @Operation(summary = "Pull rates for every day in a range")
    public BackfillResult backfill(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
                                   @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return syncService.backfill(from, to);
    }

    @GetMapping("/sync/logs")
    @Operation(summary = "Recent synchronization attempts")
    public List<SyncLog> syncLogs(@RequestParam(defaultValue = "20") @Min(1) @Max(200) int limit) {
        return syncService.recentLogs(limit);
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register an API client", description = "The API key is returned only once")
    public CreatedClientResponse createClient(@Valid @RequestBody CreateClientRequest request) {
        return apiClientService.create(request);
    }

    @GetMapping("/clients")
    public List<ApiClient> clients() {
        return apiClientService.findAll();
    }

    @DeleteMapping("/clients/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke an API client")
    public void deactivateClient(@PathVariable long id) {
        apiClientService.deactivate(id);
    }
}
