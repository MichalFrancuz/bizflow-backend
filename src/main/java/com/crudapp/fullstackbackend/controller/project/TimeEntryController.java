
package com.crudapp.fullstackbackend.controller.project;

import com.crudapp.fullstackbackend.dto.project.CreateTimeEntryRequest;
import com.crudapp.fullstackbackend.dto.project.TimeEntryResponse;
import com.crudapp.fullstackbackend.mapper.TimeEntryMapper;
import com.crudapp.fullstackbackend.model.TimeEntry;
import com.crudapp.fullstackbackend.service.project.TimeEntryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/organizations/{organizationId}")
public class TimeEntryController {

    private final TimeEntryService timeEntryService;
    private final TimeEntryMapper timeEntryMapper;

    public TimeEntryController(
            TimeEntryService timeEntryService,
            TimeEntryMapper timeEntryMapper
    ) {
        this.timeEntryService = timeEntryService;
        this.timeEntryMapper = timeEntryMapper;
    }

    @GetMapping("/projects/{projectId}/time-entries")
    public List<TimeEntryResponse> getProjectTimeEntries(
            @PathVariable Long organizationId,
            @PathVariable Long projectId
    ) {
        return timeEntryService.getTimeEntries(organizationId, projectId)
                .stream()
                .map(timeEntryMapper::toResponse)
                .toList();
    }

    @GetMapping("/projects/{projectId}/time-entries/date-range")
    public List<TimeEntryResponse> getTimeEntriesByDateRange(
            @PathVariable Long organizationId,
            @PathVariable Long projectId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return timeEntryService.getTimeEntriesByDateRange(
                        organizationId,
                        projectId,
                        startDate,
                        endDate
                )
                .stream()
                .map(timeEntryMapper::toResponse)
                .toList();
    }

    @GetMapping("/employees/{employeeId}/time-entries")
    public List<TimeEntryResponse> getEmployeeTimeEntries(
            @PathVariable Long organizationId,
            @PathVariable Long employeeId
    ) {
        return timeEntryService.getEmployeeTimeEntries(organizationId, employeeId)
                .stream()
                .map(timeEntryMapper::toResponse)
                .toList();
    }

    @PostMapping("/projects/{projectId}/time-entries")
    @ResponseStatus(HttpStatus.CREATED)
    public TimeEntryResponse createTimeEntry(
            @PathVariable Long organizationId,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTimeEntryRequest request
    ) {
        TimeEntry entry = new TimeEntry();
        entry.setDescription(request.description());
        entry.setWorkDate(request.workDate());
        entry.setHours(request.hours());
        entry.setHourlyRate(request.hourlyRate());
        entry.setCurrency(request.currency());

        TimeEntry created = timeEntryService.createTimeEntry(
                organizationId,
                projectId,
                request.employeeId(),
                entry
        );

        return timeEntryMapper.toResponse(created);
    }

    @PutMapping("/projects/{projectId}/time-entries/{timeEntryId}")
    public TimeEntryResponse updateTimeEntry(
            @PathVariable Long organizationId,
            @PathVariable Long projectId,
            @PathVariable Long timeEntryId,
            @Valid @RequestBody CreateTimeEntryRequest request
    ) {
        TimeEntry entry = new TimeEntry();
        entry.setDescription(request.description());
        entry.setWorkDate(request.workDate());
        entry.setHours(request.hours());
        entry.setHourlyRate(request.hourlyRate());
        entry.setCurrency(request.currency());

        TimeEntry updated = timeEntryService.updateTimeEntry(
                organizationId,
                projectId,
                timeEntryId,
                request.employeeId(),
                entry
        );

        return timeEntryMapper.toResponse(updated);
    }

    @DeleteMapping("/projects/{projectId}/time-entries/{timeEntryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTimeEntry(
            @PathVariable Long organizationId,
            @PathVariable Long projectId,
            @PathVariable Long timeEntryId
    ) {
        timeEntryService.deleteTimeEntry(
                organizationId,
                projectId,
                timeEntryId
        );
    }
}
