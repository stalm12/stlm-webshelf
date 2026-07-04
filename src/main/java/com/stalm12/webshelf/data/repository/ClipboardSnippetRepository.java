package com.stalm12.webshelf.data.repository;

import com.stalm12.webshelf.data.entity.AppUser;
import com.stalm12.webshelf.data.entity.ClipboardSnippet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClipboardSnippetRepository extends JpaRepository<ClipboardSnippet, Long> {
    List<ClipboardSnippet> findByUserOrderByCreatedAtDesc(AppUser user);
}
