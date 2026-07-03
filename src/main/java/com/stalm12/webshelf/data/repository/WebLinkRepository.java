package com.stalm12.webshelf.data.repository;

import com.stalm12.webshelf.data.entity.AppUser;
import com.stalm12.webshelf.data.entity.WebLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebLinkRepository extends JpaRepository<WebLink, Long> {
    List<WebLink> findByUserOrderByCreatedAtDesc(AppUser user);
}
