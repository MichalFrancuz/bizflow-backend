
package com.crudapp.fullstackbackend.dto.employee;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmployeeResponse(

        Long id,
        String name,
        String familyname,
        String email,
        String phonenumber,
        BigDecimal salary,
        String salaryCurrency,
        boolean active,
        LocalDateTime createdAt

) {
}
