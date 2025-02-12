package com.tiny.url.repository;

import com.tiny.url.models.Url;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UrlRepository extends JpaRepository<Url, String> {

    List<Url> findAllByTinyUrl(String tinyUrl);

    Url findByTinyUrl(String tinyUrl);

    Url findByOriginalUrl(String originalUrl);
}
