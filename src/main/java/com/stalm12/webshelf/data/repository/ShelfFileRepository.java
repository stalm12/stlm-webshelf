package com.stalm12.webshelf.data.repository;

import com.stalm12.webshelf.data.entity.AppUser;
import com.stalm12.webshelf.data.entity.ShelfFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShelfFileRepository extends JpaRepository<ShelfFile, Long> {
    List<ShelfFile> findByUserOrderByCreatedAtDesc(AppUser user);
    Optional<ShelfFile> findByPublicIdAndUserUsername(String publicId, String username);
}
