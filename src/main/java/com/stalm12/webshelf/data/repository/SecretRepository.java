package com.stalm12.webshelf.data.repository;

import com.stalm12.webshelf.data.entity.AppUser;
import com.stalm12.webshelf.data.entity.Secret;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecretRepository extends JpaRepository<Secret, Long> {
    List<Secret> findByUserOrderByCreatedAtDesc(AppUser user);
}
