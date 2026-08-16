package com.tiny.url.repository;

import com.tiny.url.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlRepository extends JpaRepository<Url, String> {

    Url findByTinyUrl(String tinyUrl);

    Url findByOriginalUrl(String originalUrl);
}
