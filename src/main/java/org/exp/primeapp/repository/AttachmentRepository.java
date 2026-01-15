package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, String> {

    List<Attachment> findByProductIsNull();

    List<Attachment> findByProductIsNotNull();

    List<Attachment> findByProductId(Long productId);

    @Query("SELECT COUNT(a) FROM Attachment a")
    int countAll();

    /**
     * Fetch only base64 data for fallback when disk file is missing.
     * Uses native query for performance - only retrieves the base64 column.
     */
    @Query(value = "SELECT file_data_base64 FROM attachments WHERE uuid = :uuid", nativeQuery = true)
    String findBase64DataByUuid(@Param("uuid") String uuid);

    /**
     * Fetch only base64 data by filename for fallback when disk file is missing.
     * Uses native query for performance - only retrieves the base64 column.
     * Uses indexed filename column for fast lookup.
     */
    @Query(value = "SELECT file_data_base64 FROM attachments WHERE filename = :filename", nativeQuery = true)
    String findBase64DataByFilename(@Param("filename") String filename);

    /**
     * Find attachment by filename
     */
    java.util.Optional<Attachment> findByFilename(String filename);
}