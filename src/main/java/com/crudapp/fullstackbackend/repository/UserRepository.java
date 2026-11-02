package com.crudapp.fullstackbackend.repository;

import com.crudapp.fullstackbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {
}


