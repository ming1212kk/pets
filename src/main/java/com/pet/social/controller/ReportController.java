package com.pet.social.controller;

import com.pet.social.config.AuthInterceptor;
import com.pet.social.domain.ReportEntry;
import com.pet.social.domain.UserAccount;
import com.pet.social.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ReportResponse createReport(@Valid @RequestBody CreateReportRequest request, HttpServletRequest httpRequest) {
        UserAccount actor = (UserAccount) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER);
        ReportEntry report = reportService.createReport(actor, request.targetType(), request.targetId(), request.reason());
        return new ReportResponse(report.getId(), report.getTargetType(), report.getTargetId(), report.getStatus(),
            report.getCreatedAt().toString());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(SecurityException.class)
    public ErrorResponse handleSecurity(SecurityException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    public record CreateReportRequest(@NotBlank String targetType, @NotNull Long targetId, @NotBlank String reason) {
    }

    public record ReportResponse(long reportId, String targetType, long targetId, String status, String createdAt) {
    }

    public record ErrorResponse(String message) {
    }
}
