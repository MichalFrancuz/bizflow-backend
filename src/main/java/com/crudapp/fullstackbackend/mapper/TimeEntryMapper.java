
package com.crudapp.fullstackbackend.mapper;

import com.crudapp.fullstackbackend.dto.project.TimeEntryResponse;
import com.crudapp.fullstackbackend.model.TimeEntry;
import org.springframework.stereotype.Component;

@Component
public class TimeEntryMapper {

    public TimeEntryResponse toResponse(TimeEntry timeEntry) {
        return new TimeEntryResponse(
                timeEntry.getId(),
                timeEntry.getProject().getId(),
                timeEntry.getEmployee().getId(),
                timeEntry.getEmployee().getName(),
                timeEntry.getEmployee().getFamilyname(),
                timeEntry.getDescription(),
                timeEntry.getWorkDate(),
                timeEntry.getHours(),
                timeEntry.getHourlyRate(),
                timeEntry.getCurrency(),
                timeEntry.getTotalCost(),
                timeEntry.getCreatedAt()
        );
    }
}
