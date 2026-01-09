package org.exp.primeapp.service.face.global.attachment;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.exp.primeapp.models.dto.responce.global.AttachmentRes;
import org.exp.primeapp.models.entities.Attachment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public interface AttachmentService {

    void get(String attachmentUrl, HttpServletRequest request, HttpServletResponse response) throws IOException;

    Attachment getAttachment(Long attachmentId);

    Attachment getAttachmentWithUrl(String attachmentUrl);

    void validateFile(MultipartFile file);

    void validateAttachmentId(Long attachmentId);

    List<AttachmentRes> convertToAttachmentResList(List<Attachment> attachments);

    AttachmentRes convertToAttachmentRes(Attachment attachment);

    List<String> convertToAttachmentUrls(List<Attachment> attachments);
}
