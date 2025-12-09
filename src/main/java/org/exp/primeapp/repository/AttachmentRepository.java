package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    Optional<Attachment> findByIdAndActiveTrue(Long id);

    List<Attachment> findAllByActiveTrue();

    List<Attachment> findAllByActiveFalse();

    List<Attachment> findByProductIsNull();

    List<Attachment> findByProductIsNullAndActiveTrue();

    List<Attachment> findByProductIsNullAndActiveFalse();

    List<Attachment> findByProductIsNotNull();

    List<Attachment> findByProductIsNotNullAndActiveTrue();

    List<Attachment> findByProductIsNotNullAndActiveFalse();

    List<Attachment> findByProductId(Long productId);

    List<Attachment> findByProductIdAndActiveTrue(Long productId);

    List<Attachment> findByProductIdAndActiveFalse(Long productId);

    @Query("SELECT COUNT(a) FROM Attachment a")
    int countAll();

    @Query("SELECT COUNT(a) FROM Attachment a WHERE a.active = true")
    int countAllByActiveTrue();

    @Query("SELECT COUNT(a) FROM Attachment a WHERE a.active = false")
    int countAllByActiveFalse();

    Attachment findByUrl(String url);

    Set<Attachment> findAllByUrlIn(Collection<String> urls);
}