package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import java.util.Set;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByProductIsNull();

    List<Attachment> findByProductIsNotNull();

    List<Attachment> findByProductId(Long productId);

    @Query("SELECT COUNT(a) FROM Attachment a")
    int countAll();

    Attachment findByUrl(String url);

    Set<Attachment> findAllByUrlIn(Collection<String> urls);
}